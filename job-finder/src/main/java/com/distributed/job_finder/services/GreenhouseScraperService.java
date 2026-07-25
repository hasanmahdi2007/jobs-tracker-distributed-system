package com.distributed.job_finder.services;

import com.distributed.job_finder.config.GreenhouseConfig;
import com.distributed.job_finder.dtos.JobDto;
import com.distributed.job_finder.dtos.greenhouse.GreenhouseJobResponse;
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

    private static final String JOB_INGESTION_STREAM = "job:ingestion:stream";

    @Autowired
    public GreenhouseScraperService(WebClient webClient, ReactiveRedisTemplate<String, JobDto> reactiveRedisTemplate, GreenhouseConfig config) {
        this.webClient = webClient;
        this.reactiveRedisTemplate = reactiveRedisTemplate;
        this.config = config;
    }

    public Mono<Void> scrapeAllConfiguredBoards() {
        log.info("Starting scrape for {} configured Greenhouse boards...", config.getTargetBoards().size());

        return Flux.fromIterable(config.getTargetBoards())
                // Limit to 3 concurrent HTTP requests so we don't get IP banned
                .flatMap(boardToken -> {
                    UUID dummyCompanyId = UUID.randomUUID();
                    return fetchAndPushJobs(dummyCompanyId, boardToken);
                }, 3)
                .then();
    }

    private Mono<Void> fetchAndPushJobs(UUID companyId, String boardToken) {
        String greenhouseApiUrl = String.format("%s/%s/jobs", config.getBaseUrl(), boardToken);
        log.info("Fetching jobs from Greenhouse for board: {}", boardToken);

        return webClient.get()
                .uri(greenhouseApiUrl)
                .retrieve()
                .bodyToMono(GreenhouseJobResponse.class)
                // CLEANER FIX: Instead of mapping to a List, we safely turn it directly into a Flux stream
                .flatMapMany(response -> {
                    if (response != null && response.jobs() != null) {
                        return Flux.fromIterable(response.jobs());
                    }
                    return Flux.empty(); // Safely skips if the API returned no jobs
                })
                .map(ghJob -> new JobDto(
                        String.valueOf(ghJob.id()),
                        companyId,
                        ghJob.title(),
                        ghJob.location() != null ? ghJob.location().name() : "Remote / Unspecified",
                        "General",
                        ghJob.absoluteUrl(),
                        ""
                ))
                .flatMap(this::pushToRedisStream)
                .doOnComplete(() -> log.info("Finished fetching jobs for {}", boardToken))
                .doOnError(error -> log.error("Failed to fetch jobs for board {}: {}", boardToken, error.getMessage()))
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