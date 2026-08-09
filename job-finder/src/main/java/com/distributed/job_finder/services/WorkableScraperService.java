package com.distributed.job_finder.services;

import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.stream.ObjectRecord;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.connection.stream.StreamRecords;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import com.distributed.job_finder.config.WorkableConfig;
import com.distributed.job_finder.dtos.JobDto;
import com.distributed.job_finder.dtos.workable.WorkableResponse;
import com.distributed.job_finder.repos.CompanyRepo;
import com.distributed.job_finder.utils.JobDataParser;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@Service
public class WorkableScraperService {

    private final WebClient webClient;
    private final ReactiveRedisTemplate<String, JobDto> reactiveRedisTemplate;
    private final CompanyRepo companyRepo;
    private final WorkableConfig workableConfig; // Added config dependency

    private static final String JOB_INGESTION_STREAM = "job:ingestion:stream";

    @Autowired
    public WorkableScraperService(ReactiveRedisTemplate<String, JobDto> reactiveRedisTemplate, 
                                  CompanyRepo companyRepo,
                                  WorkableConfig workableConfig) { 
        
        org.springframework.web.reactive.function.client.ExchangeStrategies strategies = 
            org.springframework.web.reactive.function.client.ExchangeStrategies.builder()
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(16 * 1024 * 1024)) 
                .build();

        this.webClient = org.springframework.web.reactive.function.client.WebClient.builder()
                .exchangeStrategies(strategies)
                .build();
        
        this.reactiveRedisTemplate = reactiveRedisTemplate;
        this.companyRepo = companyRepo;
        this.workableConfig = workableConfig; // Assign config
    }

    public Mono<Void> scrapeAllConfiguredBoards() {
        List<String> targetBoards = workableConfig.getTargetBoards();
        
        if (targetBoards == null || targetBoards.isEmpty()) {
            log.warn("No Workable target boards configured. Skipping scrape.");
            return Mono.empty();
        }

        log.info("Starting scrape for {} configured Workable boards...", targetBoards.size());

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
        // Use the baseUrl from the config dynamically
        String workableApiUrl = String.format("%s/%s?details=true", workableConfig.getBaseUrl(), boardToken);
        log.info("Fetching jobs from Workable for board: {}", boardToken);

        return webClient.get()
                .uri(workableApiUrl)
                .retrieve()
                .bodyToMono(WorkableResponse.class)
                .flatMapMany(response -> {
                    if (response != null && response.jobs() != null) {
                        return Flux.fromIterable(response.jobs());
                    }
                    return Flux.empty();
                })
                .map(wJob -> {
                    String jobTitle = wJob.title();
                    String jobDescription = wJob.description() != null ? wJob.description() : "";

                    String locationStr = "Unspecified";
                    if (wJob.location() != null) {
                        if (wJob.location().telecommuting()) {
                            locationStr = "Remote";
                        } else {
                            String city = wJob.location().city() != null ? wJob.location().city() : "";
                            String country = wJob.location().country() != null ? wJob.location().country() : "";
                            locationStr = (city + ", " + country).replaceAll("^, |, $", "");
                        }
                    }

                    return new JobDto(
                            wJob.id(),
                            companyId,
                            companyName,
                            jobTitle,
                            locationStr,
                            wJob.department() != null ? wJob.department() : JobDataParser.extractDepartment(jobTitle),
                            wJob.url(),
                            jobDescription,
                            JobDataParser.extractExperienceLevel(jobTitle),
                            wJob.employmentType() != null ? wJob.employmentType() : JobDataParser.extractEmploymentType(jobTitle, jobDescription),
                            "USD"
                    );
                })
                .delayElements(java.time.Duration.ofMillis(5)) 
                .flatMap(this::pushToRedisStream, 16) 
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
                .doOnSuccess(recordId -> log.debug("Pushed Workable job ticket to Redis Stream: {}", jobDto.atsJobId()));
    }
}