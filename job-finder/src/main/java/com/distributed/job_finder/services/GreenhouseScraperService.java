package com.distributed.job_finder.services;

import com.distributed.job_finder.config.GreenhouseConfig;
import com.distributed.job_finder.dtos.JobDto;
import com.distributed.job_finder.dtos.greenhouse.GreenhouseJobResponse;
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
public class GreenhouseScraperService {

    private final WebClient webClient;
    private final ReactiveRedisTemplate<String, JobDto> reactiveRedisTemplate;
    private final GreenhouseConfig config;
    private final CompanyRepo companyRepo;

    private static final String JOB_INGESTION_STREAM = "job:ingestion:stream";

    // 1. Notice WebClient or WebClient.Builder is REMOVED from the parameters
    @Autowired
    public GreenhouseScraperService(ReactiveRedisTemplate<String, JobDto> reactiveRedisTemplate, 
                                    GreenhouseConfig config,
                                    CompanyRepo companyRepo) {
        
        // 2. Increase buffer size to 16MB to handle massive job descriptions
        org.springframework.web.reactive.function.client.ExchangeStrategies strategies = 
            org.springframework.web.reactive.function.client.ExchangeStrategies.builder()
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(16 * 1024 * 1024)) 
                .build();

        // 3. We call WebClient.builder() directly instead of relying on Spring injection
        this.webClient = org.springframework.web.reactive.function.client.WebClient.builder()
                .exchangeStrategies(strategies)
                .build();
        
        this.reactiveRedisTemplate = reactiveRedisTemplate;
        this.config = config;
        this.companyRepo = companyRepo;
    }

    public Mono<Void> scrapeAllConfiguredBoards() {
        List<String> targetBoards = config.getTargetBoards();

        log.info("Starting scrape for {} configured Greenhouse boards...", config.getTargetBoards().size());

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
        String greenhouseApiUrl = String.format("%s/%s/jobs?content=true", config.getBaseUrl(), boardToken);
        log.info("Fetching jobs from Greenhouse for board: {}", boardToken);

        return webClient.get()
                .uri(greenhouseApiUrl)
                .retrieve()
                .bodyToMono(GreenhouseJobResponse.class)
                .flatMapMany(response -> {
                    if (response != null && response.jobs() != null) {
                        return Flux.fromIterable(response.jobs());
                    }
                    return Flux.empty();
                })
                .map(ghJob -> {
                    String jobTitle = ghJob.title();
                    String jobDescription = ghJob.content() != null ? ghJob.content() : "";

                    return new JobDto(
                            String.valueOf(ghJob.id()),
                            companyId,
                            companyName,
                            jobTitle,
                            ghJob.location() != null ? ghJob.location().name() : "Remote / Unspecified",
                            JobDataParser.extractDepartment(jobTitle),
                            ghJob.absoluteUrl(),
                            jobDescription,
                            JobDataParser.extractExperienceLevel(jobTitle),
                            JobDataParser.extractEmploymentType(jobTitle, jobDescription),
                            "USD" 
                    );
                })
                // CRITICAL: Throttle the ingestion so we don't crash the Redis socket
                .delayElements(java.time.Duration.ofMillis(5)) 
                .flatMap(jobDto -> pushToRedisStream(jobDto), 16) // Max 16 concurrent Redis pushes per board
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