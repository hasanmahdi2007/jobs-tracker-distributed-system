package com.distributed.job_finder.services;

import com.distributed.job_finder.dtos.JobDto;
import com.distributed.job_finder.enums.JobSort;
import com.distributed.job_finder.repos.JobRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.UUID;

@Service
public class JobService {

    private final JobRepo jobRepo;
    private final ReactiveRedisTemplate<String, JobDto> redisTemplate;

    private static final int MAX_CACHE_SIZE = 250;

    @Autowired
    public JobService(JobRepo jobRepo, ReactiveRedisTemplate<String, JobDto> redisTemplate) {
        this.jobRepo = jobRepo;
        this.redisTemplate = redisTemplate;
    }

    // Now returns a raw Flux stream directly from the database or Redis cache!
    public Flux<JobDto> getJobs(String search, String location, String type, String company, 
                                String category, JobSort sort, int page, int size) {
        
        JobSort safeSort = (sort == null) ? JobSort.DIVERSE : sort;

        // Check if this is an unfiltered request
        boolean isDefaultFeed = (search == null && location == null && type == null && 
                                 company == null && category == null && 
                                 (safeSort == JobSort.RECENT || safeSort == JobSort.DIVERSE));

        long startIndex = (long) page * size;
        long endIndex = startIndex + size - 1;

        // If requesting within the first 250 cached items, try Redis first
        if (isDefaultFeed && startIndex < MAX_CACHE_SIZE) {
            String cacheKey = "hot:feed:sort:" + safeSort.name();

            return redisTemplate.opsForList().range(cacheKey, startIndex, endIndex)
                .switchIfEmpty(
                    // Cache Miss: Fetch the top 250 jobs from PostgreSQL
                    jobRepo.findJobsDynamically(search, location, type, company, category, safeSort, 0, MAX_CACHE_SIZE)
                        .collectList()
                        .flatMapMany(jobList -> {
                            if (jobList.isEmpty()) {
                                return Flux.empty();
                            }
                            // Clear old cache, push fresh top 250, set 60s TTL
                            return redisTemplate.delete(cacheKey)
                                .then(redisTemplate.opsForList().rightPushAll(cacheKey, jobList))
                                .flatMap(added -> redisTemplate.expire(cacheKey, Duration.ofSeconds(60)))
                                .thenMany(Flux.fromIterable(jobList))
                                .skip(startIndex) // Slice exactly what the controller requested
                                .take(size);
                        })
                );
        }

        // Filtered queries or deep pagination (>250 results) bypass Redis and hit Postgres directly
        return jobRepo.findJobsDynamically(search, location, type, company, category, safeSort, page, size);
    }

    public Mono<JobDto> getJobById(UUID id) {
        return jobRepo.findById(id)
                .switchIfEmpty(Mono.error(new RuntimeException("Job not found with id: " + id)))
                .map(job -> new JobDto(
                        job.getAtsJobId(),
                        job.getCompanyId(),
                        job.getCompanyName(),
                        job.getTitle(),
                        job.getLocation(),
                        job.getDepartment(),
                        job.getApplyUrl(),
                        job.getDescriptionText(), 
                        job.getExperienceLevel(),
                        job.getEmploymentType(),
                        job.getSalaryCurrency()
                ));
    }
}