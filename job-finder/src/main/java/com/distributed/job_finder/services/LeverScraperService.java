package com.distributed.job_finder.services;

import com.distributed.job_finder.config.LeverConfig;
import com.distributed.job_finder.dtos.JobDto;
import com.distributed.job_finder.dtos.lever.LeverJob;
import com.distributed.job_finder.repos.CompanyRepo;
import com.distributed.job_finder.utils.JobDataParser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.stream.ObjectRecord;
import org.springframework.data.redis.connection.stream.StreamRecords;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import org.springframework.data.redis.connection.stream.RecordId;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class LeverScraperService {

    private final WebClient webClient;
    private final ReactiveRedisTemplate<String, JobDto> reactiveRedisTemplate;
    private final LeverConfig config;
    private final CompanyRepo companyRepo;

    private static final String JOB_INGESTION_STREAM = "job:ingestion:stream";

    @Autowired
    public LeverScraperService(ReactiveRedisTemplate<String, JobDto> reactiveRedisTemplate, 
                               LeverConfig config,
                               CompanyRepo companyRepo) {
        
        // Increase buffer size to handle massive job descriptions
        org.springframework.web.reactive.function.client.ExchangeStrategies strategies = 
            org.springframework.web.reactive.function.client.ExchangeStrategies.builder()
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(16 * 1024 * 1024)) 
                .build();

        this.webClient = org.springframework.web.reactive.function.client.WebClient.builder()
                .exchangeStrategies(strategies)
                .build();
        
        this.reactiveRedisTemplate = reactiveRedisTemplate;
        this.config = config;
        this.companyRepo = companyRepo;
    }

    public Mono<Void> scrapeAllConfiguredBoards() {
        List<String> targetBoards = config.getTargetBoards();

        log.info("Starting scrape for {} configured Lever boards...", config.getTargetBoards().size());

        return Flux.fromIterable(targetBoards)
                .flatMap(boardToken -> 
                    // 1. Call the repo directly (it returns Mono<Company>)
                    companyRepo.findByBoardTokenIgnoreCase(boardToken)
                        // 2. Handle the "Not Found" case reactively
                        .switchIfEmpty(Mono.error(new RuntimeException("Company not found in DB for board token: " + boardToken)))
                        // 3. Chain to the fetch method
                        .flatMap(company -> fetchAndPushJobs(company.getId(), company.getName(), boardToken))
                , 3)
                .then();
    }

    private Mono<Void> fetchAndPushJobs(UUID companyId, String companyName, String boardToken) {
        // Lever's API requires ?mode=json
        String leverApiUrl = String.format("%s/%s?mode=json", config.getBaseUrl(), boardToken);
        log.info("Fetching jobs from Lever for board: {}", boardToken);

        return webClient.get()
                .uri(leverApiUrl)
                .retrieve()
                .bodyToFlux(LeverJob.class) // Lever returns a raw JSON array, so we use bodyToFlux directly
                .map(leverJob -> {
                    String jobTitle = leverJob.text() != null ? leverJob.text() : "Unknown Title";
                    String jobDescription = leverJob.descriptionPlain() != null ? leverJob.descriptionPlain() : "";
                    String location = (leverJob.categories() != null && leverJob.categories().location() != null) 
                            ? leverJob.categories().location() : "Remote / Unspecified";

                    return new JobDto(
                            leverJob.id(), // Lever IDs are already Strings
                            companyId,
                            companyName,
                            jobTitle,
                            location,
                            JobDataParser.extractDepartment(jobTitle),
                            leverJob.hostedUrl(),
                            jobDescription,
                            JobDataParser.extractExperienceLevel(jobTitle),
                            JobDataParser.extractEmploymentType(jobTitle, jobDescription),
                            "USD" 
                    );
                })
                .delayElements(java.time.Duration.ofMillis(5)) 
                .flatMap(jobDto -> pushToRedisStream(jobDto), 16) 
                .doOnComplete(() -> log.info("Finished fetching jobs for {}", boardToken))
                .onErrorResume(error -> {
                    log.warn("Skipping board '{}' due to error: {}", boardToken, error.getMessage());
                    return Flux.empty();
                })
                .then(); 
    }

    private Mono<RecordId> pushToRedisStream(JobDto jobDto) {
        ObjectRecord<String, JobDto> record = StreamRecords.newRecord()
                .ofObject(jobDto)
                .withStreamKey(JOB_INGESTION_STREAM);

        return reactiveRedisTemplate.opsForStream().add(record)
                .doOnSuccess(recordId -> log.debug("Pushed job ticket to Redis Stream: {}", jobDto.atsJobId()));
    }
}