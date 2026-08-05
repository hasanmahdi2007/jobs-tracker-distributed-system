package com.distributed.job_finder.services;

import com.distributed.job_finder.dtos.JobDto;
import com.distributed.job_finder.entities.Job;
import com.distributed.job_finder.repos.JobRepo;
import com.distributed.job_finder.utils.LocationNormalizer;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.stream.*;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

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

                List<RecordId> recordIds = new ArrayList<>();
                long processed = 0;

                for (ObjectRecord<String, JobDto> record : records) {
                    try {
                        JobDto incomingJob = record.getValue();
                        if (incomingJob != null) {
                            saveOrUpdateJob(incomingJob); 
                        }
                        recordIds.add(record.getId());
                        processed++;
                    } catch (Exception e) {
                        log.error("Failed to process job record ID: {}", record.getId(), e);
                        recordIds.add(record.getId()); 
                    }
                }

                if (recordIds.isEmpty()) {
                    return Mono.just(0L);
                }

                RecordId[] idArray = recordIds.toArray(new RecordId[0]);

                return redisTemplate.opsForStream().acknowledge(STREAM_KEY, CONSUMER_GROUP, idArray)
                    .flatMap(acked -> redisTemplate.opsForStream().delete(STREAM_KEY, idArray))
                    .thenReturn(processed);
            })
            .onErrorResume(e -> {
                log.error("Polling error: {}", e.getMessage());
                return Mono.just(0L);
            });
    }

    private void saveOrUpdateJob(JobDto incomingJob) {
        // Preserves city name and appends country ("Paris, France") before DB insert
        String cleanLocation = locationNormalizer.normalizeLocationForDatabase(incomingJob.location());

        jobRepository.findByAtsJobIdAndCompanyId(incomingJob.atsJobId(), incomingJob.companyId())
            .ifPresentOrElse(
                existingJob -> {
                    existingJob.setTitle(incomingJob.title());
                    existingJob.setLocation(cleanLocation);
                    existingJob.setDepartment(incomingJob.department());
                    existingJob.setDescriptionText(incomingJob.description());
                    existingJob.setExperienceLevel(incomingJob.experienceLevel());
                    existingJob.setEmploymentType(incomingJob.employmentType());
                    existingJob.setSalaryMin(incomingJob.salaryMin());
                    existingJob.setSalaryMax(incomingJob.salaryMax());
                    existingJob.setSalaryCurrency(incomingJob.salaryCurrency());
                    jobRepository.save(existingJob);
                },
                () -> {
                    Job newJob = new Job();
                    newJob.setAtsJobId(incomingJob.atsJobId());
                    newJob.setCompanyId(incomingJob.companyId());
                    newJob.setCompanyName(incomingJob.companyName());
                    newJob.setTitle(incomingJob.title());
                    newJob.setLocation(cleanLocation);
                    newJob.setDepartment(incomingJob.department());
                    newJob.setApplyUrl(incomingJob.url());
                    newJob.setDescriptionText(incomingJob.description());
                    newJob.setExperienceLevel(incomingJob.experienceLevel());
                    newJob.setEmploymentType(incomingJob.employmentType());
                    newJob.setSalaryMin(incomingJob.salaryMin());
                    newJob.setSalaryMax(incomingJob.salaryMax());
                    newJob.setSalaryCurrency(incomingJob.salaryCurrency());
                    jobRepository.save(newJob);
                }
            );
    }
}