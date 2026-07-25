package com.distributed.job_finder.services;

import com.distributed.job_finder.dtos.JobDto;
import com.distributed.job_finder.entities.Job;
import com.distributed.job_finder.repos.JobRepo;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;

import java.nio.ByteBuffer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.ObjectRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.stream.StreamReceiver;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.scheduler.Schedulers;

@Slf4j
@Service
public class JobStreamConsumer {

    private final JobRepo jobRepository;
    private final StreamReceiver<String, ObjectRecord<String, JobDto>> streamReceiver;
    private final ReactiveRedisConnectionFactory factory;

    private static final String STREAM_KEY = "job:ingestion:stream";
    private static final String CONSUMER_GROUP = "job-workers-group";

    @Autowired
    public JobStreamConsumer(JobRepo jobRepository, StreamReceiver<String, ObjectRecord<String, JobDto>> streamReceiver, ReactiveRedisConnectionFactory factory) {
        this.jobRepository = jobRepository;
        this.streamReceiver = streamReceiver;
        this.factory = factory;
    }

    @PostConstruct
    public void startConsuming() {
        streamReceiver.receive(Consumer.from(CONSUMER_GROUP, "worker-1"),
                        StreamOffset.create(STREAM_KEY, ReadOffset.lastConsumed()))
                .publishOn(Schedulers.boundedElastic()) 
                .subscribe(this::processMessage);
    }

    @Transactional
    public void processMessage(ObjectRecord<String, JobDto> message) {
        JobDto incomingJob = message.getValue();
        log.info("Worker picked up ticket for ATS Job ID: {}", incomingJob.atsJobId());

        try {
            jobRepository.findByAtsJobIdAndCompanyId(incomingJob.atsJobId(), incomingJob.companyId())
                    .ifPresentOrElse(
                            existingJob -> {
                                existingJob.setTitle(incomingJob.title());
                                existingJob.setLocation(incomingJob.location());
                                existingJob.setDepartment(incomingJob.department());
                                existingJob.setDescriptionText(incomingJob.description());
                                existingJob.setExperienceLevel(incomingJob.experienceLevel());
                                existingJob.setEmploymentType(incomingJob.employmentType());
                                // Update salary fields if they were extracted
                                existingJob.setSalaryMin(incomingJob.salaryMin());
                                existingJob.setSalaryMax(incomingJob.salaryMax());
                                existingJob.setSalaryCurrency(incomingJob.salaryCurrency());

                                jobRepository.save(existingJob);
                                log.debug("Updated existing job: {}", existingJob.getId());
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
                                log.info("Saved BRAND NEW job to PostgreSQL: {}", incomingJob.title());
                            }
                    );

            // 2. Acknowledge AND Delete the message to free up Redis memory
            factory.getReactiveConnection().streamCommands()
                    .xAck(ByteBuffer.wrap(STREAM_KEY.getBytes()), CONSUMER_GROUP, message.getId().getValue())
                    .then(factory.getReactiveConnection().streamCommands()
                            .xDel(ByteBuffer.wrap(STREAM_KEY.getBytes()), message.getId().getValue()))
                    .subscribe(
                            result -> log.debug("Successfully acked and deleted message from stream"),
                            err -> log.error("Failed to delete message from stream", err)
                    );

        } catch (Exception e) {
            log.error("Failed to process job ticket: {}", incomingJob.atsJobId(), e);
        }
    }
}