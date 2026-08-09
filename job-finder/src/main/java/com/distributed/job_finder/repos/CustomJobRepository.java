package com.distributed.job_finder.repos;

import com.distributed.job_finder.dtos.JobDto;
import com.distributed.job_finder.enums.JobSort;

import reactor.core.publisher.Flux;

public interface CustomJobRepository {
    Flux<JobDto> findJobsDynamically(
            String search, String location, String type, String company, 
            String category, JobSort sort, 
            int page, int limit
    );
}