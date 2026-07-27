package com.distributed.job_finder.services;

import com.distributed.job_finder.dtos.JobDto;
import com.distributed.job_finder.entities.Job;
import com.distributed.job_finder.repos.JobRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class JobService {

    private final JobRepo jobRepo;

    @Autowired
    public JobService(JobRepo jobRepo) {
        this.jobRepo = jobRepo;
    }

    public Page<JobDto> getJobs(String search, String location, String type, String sort, int page, int size) {
        
        // Dynamically build the sort order based on frontend request
        Sort sortOrder;
        if ("relevant".equalsIgnoreCase(sort)) {
            // If they want relevant, we sort alphabetically by title (or you can adjust later)
            sortOrder = Sort.by("title").ascending();
        } else {
            // Default to 'recent' (Newest first)
            sortOrder = Sort.by("createdAt").descending();
        }

        Pageable pageable = PageRequest.of(page, size, sortOrder);
        
        // Pass the new type parameter to the repo
        Page<Job> jobPage = jobRepo.searchJobs(search, location, type, pageable);

        return jobPage.map(job -> new JobDto(
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
                job.getSalaryMin(),
                job.getSalaryMax(),
                job.getSalaryCurrency()
        ));
    }

    public JobDto getJobById(UUID id) {
        Job job = jobRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Job not found with id: " + id));

        return new JobDto(
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
                job.getSalaryMin(),
                job.getSalaryMax(),
                job.getSalaryCurrency()
        );
    }
}