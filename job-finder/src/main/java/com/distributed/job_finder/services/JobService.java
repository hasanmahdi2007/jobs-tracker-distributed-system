package com.distributed.job_finder.services;

import com.distributed.job_finder.dtos.JobDto;
import com.distributed.job_finder.entities.Job;
import com.distributed.job_finder.enums.JobSort;
import com.distributed.job_finder.repos.JobRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class JobService {

    private final JobRepo jobRepo;

    @Autowired
    public JobService(JobRepo jobRepo) {
        this.jobRepo = jobRepo;
    }

    public List<JobDto> getJobs(String search, String location, String type, String company, 
                                String category, JobSort sort, String lastTitle, 
                                LocalDateTime lastCreatedAt, UUID lastId, int size) {
        
        List<Job> jobs;

        // hack to make spring data apply the SQL LIMIT clause. page is always 0 since we use cursors to jump around.
        Pageable limit = PageRequest.of(0, size);

        // routing to the specific repo query based on sort. 
        // passing the cursors down so the db avoids offset scanning.
        switch (sort) {
            case RECENT:
                jobs = jobRepo.searchJobsRecent(search, location, type, company, category, lastCreatedAt, lastId, limit);
                break;
            case RELEVANT:
                jobs = jobRepo.searchJobsRelevant(search, location, type, company, category, lastTitle, lastId, limit);
                break;
            case DIVERSE:
            default:
                jobs = jobRepo.findDiversifiedFeed(search, location, type, company, category, lastCreatedAt, lastId, limit);
                break;
        }

        // map raw entities to dtos so we don't leak db structure to the api response
        return jobs.stream().map(job -> new JobDto(
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
        )).collect(Collectors.toList());
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