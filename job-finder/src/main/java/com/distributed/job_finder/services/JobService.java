package com.distributed.job_finder.services;

import com.distributed.job_finder.dtos.JobDto;
import com.distributed.job_finder.enums.JobSort;
import com.distributed.job_finder.repos.JobRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Service
public class JobService {

    private final JobRepo jobRepo;

    @Autowired
    public JobService(JobRepo jobRepo) {
        this.jobRepo = jobRepo;
    }

    // Now returns a raw Flux stream directly from the database!
    public Flux<JobDto> getJobs(String search, String location, String type, String company, 
                                String category, JobSort sort, int page, int size) {
        
        return jobRepo.findJobsDynamically(search, location, type, company, category, sort, page, size);
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