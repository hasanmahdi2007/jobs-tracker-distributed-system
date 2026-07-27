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

    public Page<JobDto> getJobs(String search, String location, String type, String company, String category, String sort, int page, int size) {
        
        // Dynamically build the sort order based on frontend request
        Sort sortOrder;
        if ("relevant".equalsIgnoreCase(sort)) {
            sortOrder = Sort.by("title").ascending();
        } else if ("salary_desc".equalsIgnoreCase(sort)) {
            sortOrder = Sort.by("salaryMax").descending(); // Highest Salary
        } else if ("salary_asc".equalsIgnoreCase(sort)) {
            sortOrder = Sort.by("salaryMin").ascending(); // Lowest Salary
        } else {
            // Default to 'recent' (Newest first)
            sortOrder = Sort.by("createdAt").descending();
        }

        Pageable pageable = PageRequest.of(page, size, sortOrder);
        
        // Pass all parameters to the repository search query
        Page<Job> jobPage = jobRepo.searchJobs(search, location, type, company, category, pageable);

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