package com.distributed.job_finder.services;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;

import com.distributed.job_finder.dtos.JobDto;
import com.distributed.job_finder.entities.Job;
import com.distributed.job_finder.repos.JobRepo;
import com.distributed.job_finder.utils.LocationNormalizer;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Slf4j
@Service
public class JobStreamConsumer {

    private final JobRepo jobRepository;
    private final ReactiveRedisTemplate<String, JobDto> redisTemplate;
    private final LocationNormalizer locationNormalizer;

    private static final String STREAM_KEY = "job:ingestion:stream";
    private static final String CONSUMER_GROUP = "job-workers-group";

    @Autowired
    public JobStreamConsumer(JobRepo jobRepository, 
                              ReactiveRedisTemplate<String, JobDto> redisTemplate, 
                              LocationNormalizer locationNormalizer) {
        this.jobRepository = jobRepository;
        this.redisTemplate = redisTemplate;
        this.locationNormalizer = locationNormalizer;
    }

    @PostConstruct
    public void startConsuming() {
        log.info("🚀 Pro Worker initialized. Prioritizing live jobs, cleaning pending during idle time...");

        redisTemplate.opsForStream().createGroup(STREAM_KEY, ReadOffset.from("0"), CONSUMER_GROUP)
            .onErrorResume(e -> Mono.empty())
            .thenMany(Flux.interval(Duration.ofMillis(500)))
            .onBackpressureDrop()
            .concatMap(tick -> pollAndProcessBatch()) 
            .subscribe(
                count -> {
                    if (count > 0) log.info("Successfully flushed {} jobs into PostgreSQL.", count);
                },
                err -> log.error("Error in consumer loop", err)
            );
    }

    private Mono<Long> pollAndProcessBatch() {
        // Step 1: Prioritize reading new incoming jobs (">") first
        return readBatchFromOffset(ReadOffset.from(">"))
            .flatMap(processedNew -> {
                if (processedNew > 0) {
                    return Mono.just(processedNew);
                }
                // Step 2: If no new jobs exist (stream is idle), sweep for stuck/pending jobs ("0")
                return readBatchFromOffset(ReadOffset.from("0"))
                    .doOnNext(processedPending -> {
                        if (processedPending > 0) {
                            log.info("Idle time sweep: Drained {} stuck/pending jobs from Redis stream.", processedPending);
                        }
                    });
            });
    }

    private Mono<Long> readBatchFromOffset(ReadOffset readOffset) {
        return redisTemplate.opsForStream()
            .read(JobDto.class,
                Consumer.from(CONSUMER_GROUP, "worker-1"),
                StreamOffset.create(STREAM_KEY, readOffset)
            )
            .collectList()
            .publishOn(Schedulers.boundedElastic()) 
            .flatMap(records -> {
                if (records.isEmpty()) {
                    return Mono.just(0L);
                }

                // Create a reactive pipeline to process all records
                return Flux.fromIterable(records)
                    .flatMap(record -> {
                        JobDto incomingJob = record.getValue();
                        if (incomingJob != null) {
                            return saveOrUpdateJob(incomingJob)
                                .thenReturn(record.getId())
                                .onErrorResume(e -> {
                                    log.error("Failed to process job record ID: {}", record.getId(), e);
                                    return Mono.just(record.getId()); // Still return ID to ack/delete it
                                });
                        }
                        return Mono.just(record.getId());
                    })
                    .collectList()
                    .flatMap(recordIds -> {
                        if (recordIds.isEmpty()) return Mono.just(0L);

                        RecordId[] idArray = recordIds.toArray(new RecordId[0]);

                        // Acknowledge and delete from Redis
                        return redisTemplate.opsForStream().acknowledge(STREAM_KEY, CONSUMER_GROUP, idArray)
                            .flatMap(acked -> redisTemplate.opsForStream().delete(STREAM_KEY, idArray))
                            .thenReturn((long) recordIds.size());
                    });
            })
            .onErrorResume(e -> {
                log.error("Polling error: {}", e.getMessage());
                return Mono.just(0L);
            });
    }

    private Mono<Void> saveOrUpdateJob(JobDto incomingJob) {
        String cleanLocation = locationNormalizer.normalizeLocationForDatabase(incomingJob.location());

        // We return the Mono chain so the caller can subscribe to it
        // UPDATED: Now looks up by Company, Title, and Location to support Cross-ATS deduplication
        return jobRepository.findByCompanyIdAndTitleAndLocation(incomingJob.companyId(), incomingJob.title(), cleanLocation)
            .flatMap(existingJob -> {

                // 1. Check if the core data actually changed
                boolean dataChanged = (existingJob.getTitle() != null && !existingJob.getTitle().equals(incomingJob.title())) ||
                                      (existingJob.getLocation() != null && !existingJob.getLocation().equals(cleanLocation)) ||
                                      (existingJob.getDescriptionText() != null && !existingJob.getDescriptionText().equals(incomingJob.description()));

                if (dataChanged) {
                    // It's a real update! Modify the data and bump the 'updatedAt' timestamp
                    existingJob.setTitle(incomingJob.title());
                    existingJob.setLocation(cleanLocation);
                    existingJob.setDepartment(incomingJob.department());
                    existingJob.setDescriptionText(incomingJob.description());
                    existingJob.setExperienceLevel(incomingJob.experienceLevel());
                    existingJob.setEmploymentType(incomingJob.employmentType());
                    existingJob.setSalaryCurrency(incomingJob.salaryCurrency());
                    
                    existingJob.setUpdatedAt(java.time.LocalDateTime.now());
                    log.debug("Job {} changed, updating data and timestamp.", existingJob.getId());
                } else {
                    // Nothing changed! Just bump 'lastSeenAt' to keep it alive from the sweeper
                    log.debug("Job {} unchanged, only bumping last_seen_at.", existingJob.getId());
                }

                // Always bump lastSeenAt and ensure it is ACTIVE
                existingJob.setLastSeenAt(java.time.LocalDateTime.now());
                existingJob.setStatus("ACTIVE"); 

                return jobRepository.save(existingJob);
            })
            .switchIfEmpty(Mono.defer(() -> {
                // INSERT SCENARIO
                Job newJob = Job.builder()
                        .atsJobId(incomingJob.atsJobId()) // Still saving this for debugging!
                        .companyId(incomingJob.companyId())
                        .companyName(incomingJob.companyName())
                        .title(incomingJob.title())
                        .location(cleanLocation)
                        .department(incomingJob.department())
                        .applyUrl(incomingJob.url())
                        .descriptionText(incomingJob.description())
                        .experienceLevel(incomingJob.experienceLevel())
                        .employmentType(incomingJob.employmentType())
                        .salaryCurrency(incomingJob.salaryCurrency())
                        .status("ACTIVE") // Ensure new jobs are ACTIVE
                        // NOTE: We don't set updatedAt or lastSeenAt here, Postgres handles the DEFAULT
                        .build();

                return jobRepository.save(newJob);
            }))
            .then(); 
    }
}