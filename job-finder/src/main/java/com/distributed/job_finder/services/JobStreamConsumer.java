package com.distributed.job_finder.services;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.ObjectRecord;
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
                            // 💥 FIX: Chain the Mono so it actually executes!
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
        return jobRepository.findByAtsJobIdAndCompanyId(incomingJob.atsJobId(), incomingJob.companyId())
            .flatMap(existingJob -> {
                // UPDATE SCENARIO
                existingJob.setTitle(incomingJob.title());
                existingJob.setLocation(cleanLocation);
                existingJob.setDepartment(incomingJob.department());
                existingJob.setDescriptionText(incomingJob.description());
                existingJob.setExperienceLevel(incomingJob.experienceLevel());
                existingJob.setEmploymentType(incomingJob.employmentType());
                existingJob.setSalaryCurrency(incomingJob.salaryCurrency());
                // In R2DBC, you must manually update the timestamp
                existingJob.setUpdatedAt(java.time.LocalDateTime.now());
                
                return jobRepository.save(existingJob);
            })
            .switchIfEmpty(Mono.defer(() -> {
                // INSERT SCENARIO
                Job newJob = Job.builder()
                        .atsJobId(incomingJob.atsJobId())
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
                        .build();

                return jobRepository.save(newJob);
            }))
            .then(); // Convert Mono<Job> to Mono<Void> since we just want to know it finished
    }
}