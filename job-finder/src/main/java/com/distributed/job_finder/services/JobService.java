package com.distributed.job_finder.services;

import com.distributed.job_finder.dtos.JobDto;
import com.distributed.job_finder.entities.Job;
import com.distributed.job_finder.enums.JobSort; // <-- Import the new Enum
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

    // Notice we changed 'String sort' to 'JobSort sort' here!
    public Page<JobDto> getJobs(String search, String location, String type, String company, String category, JobSort sort, int page, int size) {
        
        Page<Job> jobPage;
        Pageable pageable;

        // Clean enterprise routing using the Enum
        switch (sort) {
            case RECENT:
                pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
                jobPage = jobRepo.searchJobs(search, location, type, company, category, pageable);
                break;
            case RELEVANT:
                pageable = PageRequest.of(page, size, Sort.by("title").ascending());
                jobPage = jobRepo.searchJobs(search, location, type, company, category, pageable);
                break;
            case SALARY_DESC:
                pageable = PageRequest.of(page, size, Sort.by("salaryMax").descending());
                jobPage = jobRepo.searchJobs(search, location, type, company, category, pageable);
                break;
            case SALARY_ASC:
                pageable = PageRequest.of(page, size, Sort.by("salaryMin").ascending());
                jobPage = jobRepo.searchJobs(search, location, type, company, category, pageable);
                break;
            case DIVERSE:
            default:
                // Pass an unsorted pageable to the Native Query so Hibernate doesn't interfere
                pageable = PageRequest.of(page, size); 
                jobPage = jobRepo.findDiversifiedFeed(search, location, type, company, category, pageable);
                break;
        }

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