package com.distributed.job_finder.controllers;

import com.distributed.job_finder.dtos.JobDto;
import com.distributed.job_finder.enums.JobSort;
import com.distributed.job_finder.services.JobService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/jobs")
public class JobController {

    private final JobService jobService;

    @Autowired
    public JobController(JobService jobService) {
        this.jobService = jobService;
    }

    @GetMapping
    public ResponseEntity<List<JobDto>> getAllJobs(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String company,
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "DIVERSE") String sort, 
            
            // KEYSET PAGINATION CURSORS
            @RequestParam(required = false) String lastTitle,
            @RequestParam(required = false) LocalDateTime lastCreatedAt,
            @RequestParam(required = false) UUID lastId,
            
            @RequestParam(defaultValue = "10") int size
    ) {
        // default to empty string so the db query doesn't crash on nulls
        String safeSearch = (search == null || search.trim().isEmpty()) ? "" : search.trim();
        String safeLocation = (location == null || location.trim().isEmpty()) ? "" : location.trim();
        String safeType = (type == null || type.trim().isEmpty()) ? "" : type.trim();
        String safeCompany = (company == null || company.trim().isEmpty()) ? "" : company.trim();
        String safeCategory = (category == null || category.trim().isEmpty()) ? "" : category.trim();

        JobSort safeSortEnum;
        try {
            safeSortEnum = JobSort.valueOf(sort.toUpperCase());
        } catch (IllegalArgumentException | NullPointerException e) {
            // fallback if frontend sends a weird string like ?sort=banana
            safeSortEnum = JobSort.DIVERSE;
        }

        // pass the cursors to the service so the db can jump straight to the right index
        List<JobDto> jobs = jobService.getJobs(
                safeSearch, safeLocation, safeType, safeCompany, 
                safeCategory, safeSortEnum, 
                lastTitle, lastCreatedAt, lastId, size
        );
        
        return ResponseEntity.ok(jobs);
    }

    @GetMapping("/{id}")
    public ResponseEntity<JobDto> getJobById(@PathVariable UUID id) {
        JobDto job = jobService.getJobById(id);
        return ResponseEntity.ok(job);
    }
}