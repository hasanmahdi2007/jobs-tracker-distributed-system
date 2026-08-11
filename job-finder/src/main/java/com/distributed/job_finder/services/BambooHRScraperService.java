package com.distributed.job_finder.services;

import com.distributed.job_finder.config.BambooHRConfig;
import com.distributed.job_finder.dtos.JobDto;
import com.distributed.job_finder.dtos.bamboohr.BambooHRJobDetail;
import com.distributed.job_finder.dtos.bamboohr.BambooHRJobResponse;
import com.distributed.job_finder.dtos.bamboohr.BambooHRJob;
import com.distributed.job_finder.repos.CompanyRepo;
import com.distributed.job_finder.utils.JobDataParser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.stream.ObjectRecord;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.connection.stream.StreamRecords;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class BambooHRScraperService {

    private final WebClient webClient;
    private final ReactiveRedisTemplate<String, JobDto> reactiveRedisTemplate;
    private final BambooHRConfig config;
    private final CompanyRepo companyRepo;

    private static final String JOB_INGESTION_STREAM = "job:ingestion:stream";

    @Autowired
    public BambooHRScraperService(ReactiveRedisTemplate<String, JobDto> reactiveRedisTemplate,
                                  BambooHRConfig config,
                                  CompanyRepo companyRepo) {

        org.springframework.web.reactive.function.client.ExchangeStrategies strategies =
                org.springframework.web.reactive.function.client.ExchangeStrategies.builder()
                        .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(16 * 1024 * 1024))
                        .build();

        this.webClient = WebClient.builder()
                .exchangeStrategies(strategies)
                .build();

        this.reactiveRedisTemplate = reactiveRedisTemplate;
        this.config = config;
        this.companyRepo = companyRepo;
    }

    public Mono<Void> scrapeAllConfiguredBoards() {
        List<String> targetBoards = config.getTargetBoards();
        log.info("Starting deep scrape for {} configured BambooHR boards...", targetBoards.size());

        return Flux.fromIterable(targetBoards)
                .flatMap(boardToken -> companyRepo.findByBoardTokenIgnoreCase(boardToken)
                        .switchIfEmpty(Mono.error(new RuntimeException("Company not found in DB for board token: " + boardToken)))
                        .flatMap(company -> fetchAndPushJobs(company.getId(), company.getName(), boardToken))
                , 3) // Max 3 companies concurrently
                .then();
    }

    private Mono<Void> fetchAndPushJobs(UUID companyId, String companyName, String boardToken) {
        String bambooApiUrl = String.format("https://%s.bamboohr.com/careers/list", boardToken);
        log.info("Fetching job list from BambooHR for board: {}", boardToken);

        return webClient.get()
                .uri(bambooApiUrl)
                .retrieve()
                .bodyToMono(BambooHRJobResponse.class)
                .flatMapMany(response -> {
                    if (response != null && response.result() != null) {
                        return Flux.fromIterable(response.result());
                    }
                    return Flux.empty();
                })
                // CRITICAL FIX: Chain the secondary detail request to get the description.
                // We use a concurrency limit of 5 to avoid triggering IP rate limits on the detail endpoint.
                .flatMap(bbJob -> fetchJobDetails(companyId, companyName, boardToken, bbJob), 5)
                .delayElements(Duration.ofMillis(5))
                .flatMap(jobDto -> pushToRedisStream(jobDto), 16)
                .doOnComplete(() -> log.info("Finished deep fetching jobs for {}", boardToken))
                .onErrorResume(error -> {
                    log.warn("Skipping BambooHR board '{}' due to error: {}", boardToken, error.getMessage());
                    return Flux.empty();
                })
                .then();
    }

    private Mono<JobDto> fetchJobDetails(UUID companyId, String companyName, String boardToken, BambooHRJob bbJob) {
        String detailUrl = String.format("https://%s.bamboohr.com/careers/%s/detail", boardToken, bbJob.id());
        String applyUrl = String.format("https://%s.bamboohr.com/careers/%s", boardToken, bbJob.id());

        return webClient.get()
                .uri(detailUrl)
                .retrieve()
                .bodyToMono(BambooHRJobDetail.class)
                .map(detail -> {
                    String jobTitle = bbJob.jobOpeningName();
                    String jobDescription = detail.description() != null ? detail.description() : "";

                    return new JobDto(
                            bbJob.id(),
                            companyId,
                            companyName,
                            jobTitle,
                            bbJob.location() != null ? bbJob.location().getFullLocation() : "Remote / Unspecified",
                            bbJob.departmentLabel(),
                            applyUrl,
                            jobDescription,
                            JobDataParser.extractExperienceLevel(jobTitle),
                            JobDataParser.extractEmploymentType(jobTitle, jobDescription),
                            "USD"
                    );
                })
                // FAULT TOLERANCE: If the detail endpoint fails (e.g. timeout), still push the job but with an empty description
                .onErrorResume(e -> {
                    log.warn("Failed to fetch details for job {} at {}: {}", bbJob.id(), boardToken, e.getMessage());
                    return Mono.just(new JobDto(
                            bbJob.id(), companyId, companyName, bbJob.jobOpeningName(),
                            bbJob.location() != null ? bbJob.location().getFullLocation() : "Remote / Unspecified",
                            bbJob.departmentLabel(), applyUrl, "",
                            JobDataParser.extractExperienceLevel(bbJob.jobOpeningName()),
                            JobDataParser.extractEmploymentType(bbJob.jobOpeningName(), ""),
                            "USD"
                    ));
                });
    }

    private Mono<RecordId> pushToRedisStream(JobDto jobDto) {
        ObjectRecord<String, JobDto> record = StreamRecords.newRecord()
                .ofObject(jobDto)
                .withStreamKey(JOB_INGESTION_STREAM);

        return reactiveRedisTemplate.opsForStream().add(record)
                .doOnSuccess(recordId -> log.debug("Pushed job ticket to Redis Stream: {}", jobDto.atsJobId()));
    }
}