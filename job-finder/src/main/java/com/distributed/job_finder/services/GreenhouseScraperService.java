package com.distributed.job_finder.services;

import com.distributed.job_finder.config.GreenhouseConfig;
import com.distributed.job_finder.dtos.JobDto;
import com.distributed.job_finder.dtos.greenhouse.GreenhouseJobResponse;
import com.distributed.job_finder.repos.CompanyRepo; // <-- Add this import
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

import java.util.UUID;

@Slf4j
@Service
public class GreenhouseScraperService {

    private final WebClient webClient;
    private final ReactiveRedisTemplate<String, JobDto> reactiveRedisTemplate;
    private final GreenhouseConfig config;
    private final CompanyRepo companyRepo; // <-- 1. Declare repository

    private static final String JOB_INGESTION_STREAM = "job:ingestion:stream";

    @Autowired
    public GreenhouseScraperService(WebClient webClient, 
                                    ReactiveRedisTemplate<String, JobDto> reactiveRedisTemplate, 
                                    GreenhouseConfig config,
                                    CompanyRepo companyRepo) { // <-- 2. Inject it here
        this.webClient = webClient;
        this.reactiveRedisTemplate = reactiveRedisTemplate;
        this.config = config;
        this.companyRepo = companyRepo;
    }

    public Mono<Void> scrapeAllConfiguredBoards() {
        log.info("Starting scrape for {} configured Greenhouse boards...", config.getTargetBoards().size());

        return Flux.fromIterable(config.getTargetBoards())
                // Limit to 3 concurrent HTTP requests so we don't get IP banned
                .flatMap(boardToken -> 
                    // Wrap the blocking DB call in a Mono and run it on a blocking thread pool
                    Mono.fromCallable(() -> companyRepo.findByBoardTokenIgnoreCase(boardToken)
                            .orElseThrow(() -> new RuntimeException("Company not found in DB for board token: " + boardToken)))
                            .subscribeOn(reactor.core.scheduler.Schedulers.boundedElastic())
                            .flatMap(company -> fetchAndPushJobs(company.getId(), boardToken))
                , 3)
                .then();
    }

    private Mono<Void> fetchAndPushJobs(UUID companyId, String boardToken) {
        String greenhouseApiUrl = String.format("%s/%s/jobs", config.getBaseUrl(), boardToken);
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
                .map(ghJob -> new JobDto(
                        String.valueOf(ghJob.id()),
                        companyId,
                        ghJob.title(),
                        ghJob.location() != null ? ghJob.location().name() : "Remote / Unspecified",
                        "General",
                        ghJob.absoluteUrl(),
                        "",     // description
                        null,   // experienceLevel
                        null,   // employmentType
                        null,   // salaryMin
                        null,   // salaryMax
                        "USD"   // salaryCurrency
                ))
                .flatMap(this::pushToRedisStream)
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