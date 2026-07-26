package com.distributed.job_finder.services;

import com.distributed.job_finder.dtos.JobDto;
import com.distributed.job_finder.entities.Job;
import com.distributed.job_finder.repos.JobRepo;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Range;
import org.springframework.data.redis.connection.stream.*;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;

@Slf4j
@Service
public class JobStreamConsumer {

    private final JobRepo jobRepository;
    private final ReactiveRedisTemplate<String, JobDto> redisTemplate;

    private static final String STREAM_KEY = "job:ingestion:stream";
    private static final String CONSUMER_GROUP = "job-workers-group";

    @Autowired
    public JobStreamConsumer(JobRepo jobRepository, ReactiveRedisTemplate<String, JobDto> redisTemplate) {
        this.jobRepository = jobRepository;
        this.redisTemplate = redisTemplate;
    }

    @PostConstruct
    public void startConsuming() {
        log.info("🚀 Hardcore polling worker initialized. Attacking Redis stream...");

        // Ensure consumer group exists
        redisTemplate.opsForStream().createGroup(STREAM_KEY, ReadOffset.from("0"), CONSUMER_GROUP)
            .onErrorResume(e -> Mono.empty())
            .thenMany(Flux.interval(Duration.ofMillis(500))) // Poll every 500ms
            .publishOn(Schedulers.boundedElastic())
            .flatMap(tick -> pollAndProcessBatch())
            .subscribe(
                count -> {
                    if (count > 0) log.info("Successfully flushed {} jobs into PostgreSQL.", count);
                },
                err -> log.error("Error in fallback consumer loop", err)
            );
    }

    private Mono<Long> pollAndProcessBatch() {
        // Read pending/new messages using XREADGROUP command directly
        return redisTemplate.opsForStream()
            .read(JobDto.class,
                Consumer.from(CONSUMER_GROUP, "worker-1"),
                StreamOffset.create(STREAM_KEY, ReadOffset.from(">"))
            )
            .collectList()
            .flatMap(records -> {
                if (records.isEmpty()) {
                    return Mono.just(0L);
                }

                long processed = 0;
                for (ObjectRecord<String, JobDto> record : records) {
                    try {
                        JobDto incomingJob = record.getValue();
                        if (incomingJob != null) {
                            saveOrUpdateJob(incomingJob);
                        }
                        
                        // Acknowledge and delete immediately
                        redisTemplate.opsForStream().acknowledge(STREAM_KEY, CONSUMER_GROUP, record.getId()).block();
                        redisTemplate.opsForStream().delete(STREAM_KEY, record.getId()).block();
                        processed++;
                    } catch (Exception e) {
                        log.error("Failed to process job record ID: {}", record.getId(), e);
                    }
                }
                return Mono.just(processed);
            })
            .onErrorResume(e -> {
                log.error("Polling error: {}", e.getMessage());
                return Mono.just(0L);
            });
    }

    private void saveOrUpdateJob(JobDto incomingJob) {
        jobRepository.findByAtsJobIdAndCompanyId(incomingJob.atsJobId(), incomingJob.companyId())
            .ifPresentOrElse(
                existingJob -> {
                    existingJob.setTitle(incomingJob.title());
                    existingJob.setLocation(incomingJob.location());
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
                    newJob.setLocation(incomingJob.location());
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