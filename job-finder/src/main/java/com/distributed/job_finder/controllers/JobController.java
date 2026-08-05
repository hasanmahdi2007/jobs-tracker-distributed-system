package com.distributed.job_finder.controllers;

import com.distributed.job_finder.dtos.JobDto;
import com.distributed.job_finder.enums.JobSort; // <-- Make sure this matches where you saved your Enum!
import com.distributed.job_finder.services.JobService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
    public ResponseEntity<Page<JobDto>> getAllJobs(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String company,
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "DIVERSE") String sort, 
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        // Prevent database null crashes by defaulting unselected filters to empty strings
        String safeSearch = (search == null || search.trim().isEmpty()) ? "" : search.trim();
        String safeLocation = (location == null || location.trim().isEmpty()) ? "" : location.trim();
        String safeType = (type == null || type.trim().isEmpty()) ? "" : type.trim();
        String safeCompany = (company == null || company.trim().isEmpty()) ? "" : company.trim();
        String safeCategory = (category == null || category.trim().isEmpty()) ? "" : category.trim();

        // ENTERPRISE FIX: Safely parse the frontend string into your Enum
        JobSort safeSortEnum;
        try {
            safeSortEnum = JobSort.valueOf(sort.toUpperCase());
        } catch (IllegalArgumentException | NullPointerException e) {
            // If the frontend sends ?sort=banana or lowercases it, fallback to DIVERSE instead of crashing
            safeSortEnum = JobSort.DIVERSE;
        }

        // Pass all safe variables (including the clean Enum) down to the service layer
        Page<JobDto> jobs = jobService.getJobs(safeSearch, safeLocation, safeType, safeCompany, safeCategory, safeSortEnum, page, size);
        return ResponseEntity.ok(jobs);
    }

    @GetMapping("/{id}")
    public ResponseEntity<JobDto> getJobById(@PathVariable UUID id) {
        JobDto job = jobService.getJobById(id);
        return ResponseEntity.ok(job);
    }
}