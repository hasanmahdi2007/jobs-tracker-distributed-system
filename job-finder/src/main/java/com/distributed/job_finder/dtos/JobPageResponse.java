package com.distributed.job_finder.dtos;

import java.util.List;

public record JobPageResponse(
    List<JobDto> content,
    long totalElements,
    int totalPages,
    int currentPage,
    int pageSize
) {}