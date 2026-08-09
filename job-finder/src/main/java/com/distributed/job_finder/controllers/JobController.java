package com.distributed.job_finder.controllers;

import com.distributed.job_finder.dtos.JobDto;
import com.distributed.job_finder.enums.JobSort;
import com.distributed.job_finder.services.JobService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/jobs")
public class JobController {

    private final JobService jobService;

    @Autowired
    public JobController(JobService jobService) {
        this.jobService = jobService;
    }

    // Now returns a raw Flux stream of jobs instead of a Page wrapper!
    @GetMapping
    public Flux<JobDto> getAllJobs(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String company,
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "DIVERSE") String sort, 
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        String safeSearch = (search == null || search.trim().isEmpty()) ? null : search.trim();
        String safeLocation = (location == null || location.trim().isEmpty()) ? null : location.trim();
        String safeType = (type == null || type.trim().isEmpty()) ? null : type.trim();
        String safeCompany = (company == null || company.trim().isEmpty()) ? null : company.trim();
        String safeCategory = (category == null || category.trim().isEmpty()) ? null : category.trim();

        JobSort safeSortEnum;
        try {
            safeSortEnum = JobSort.valueOf(sort.toUpperCase());
        } catch (IllegalArgumentException | NullPointerException e) {
            safeSortEnum = JobSort.DIVERSE;
        }

        return jobService.getJobs(
                safeSearch, safeLocation, safeType, safeCompany, 
                safeCategory, safeSortEnum, page, size
        );
    }

    @GetMapping("/{id}")
    public Mono<JobDto> getJobById(@PathVariable UUID id) {
        return jobService.getJobById(id);
    }
}