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
                // CRITICAL: Moves the slow database inserts to a separate thread pool so Redis isn't blocked!
                .publishOn(Schedulers.boundedElastic()) 
                .subscribe(this::processMessage);
    }

    @Transactional
    public void processMessage(ObjectRecord<String, JobDto> message) {
        JobDto incomingJob = message.getValue();
        log.info("Worker picked up ticket for ATS Job ID: {}", incomingJob.atsJobId());

        try {
            // 1. Check if we already have this job in the database
            jobRepository.findByAtsJobIdAndCompanyId(incomingJob.atsJobId(), incomingJob.companyId())
                    .ifPresentOrElse(
                            existingJob -> {
                                existingJob.setTitle(incomingJob.title());
                                existingJob.setLocation(incomingJob.location());
                                jobRepository.save(existingJob);
                                log.debug("Updated existing job: {}", existingJob.getId());
                            },
                            () -> {
                                Job newJob = new Job();
                                newJob.setAtsJobId(incomingJob.atsJobId());
                                newJob.setCompanyId(incomingJob.companyId());
                                newJob.setTitle(incomingJob.title());
                                newJob.setLocation(incomingJob.location());
                                newJob.setDepartment(incomingJob.department());
                                newJob.setApplyUrl(incomingJob.url());
                                newJob.setDescriptionText(incomingJob.description());

                                jobRepository.save(newJob);
                                log.info("Saved BRAND NEW job to PostgreSQL: {}", incomingJob.title());
                            }
                    );

            // 2. Acknowledge the message so Redis knows we finished saving it
                factory.getReactiveConnection().streamCommands()
        .xAck(ByteBuffer.wrap(STREAM_KEY.getBytes()), CONSUMER_GROUP, message.getId().getValue())
        .subscribe();

        } catch (Exception e) {
            log.error("Failed to process job ticket: {}", incomingJob.atsJobId(), e);
            // We intentionally do NOT acknowledge the message here. 
            // It stays in Redis so another worker can try again later!
        }
    }
}