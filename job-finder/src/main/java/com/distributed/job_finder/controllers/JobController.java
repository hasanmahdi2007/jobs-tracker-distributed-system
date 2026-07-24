package com.distributed.job_finder.controllers;

import com.distributed.job_finder.dtos.JobDto;
import com.distributed.job_finder.services.JobService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/jobs")
@CrossOrigin(origins = "*") 
public class JobController {

    private final JobService jobService;

    @Autowired
    public JobController(JobService jobService) {
        this.jobService = jobService;
    }

    /**
     * Endpoint 1: Search and Pagination
     */
    @GetMapping
    public ResponseEntity<Page<JobDto>> getAllJobs(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String location,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        // Prevent the PostgreSQL 'bytea' null crash by defaulting to empty strings
        String safeSearch = (search == null) ? "" : search;
        String safeLocation = (location == null) ? "" : location;

        // Pass the safe strings to the service
        Page<JobDto> jobs = jobService.getJobs(safeSearch, safeLocation, page, size);
        return ResponseEntity.ok(jobs);
    }

    /**
     * Endpoint 2: Get a single job by its ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<JobDto> getJobById(@PathVariable UUID id) {
        JobDto job = jobService.getJobById(id);
        return ResponseEntity.ok(job);
    }
}