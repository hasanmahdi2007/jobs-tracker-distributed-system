package com.distributed.job_finder.dtos.bamboohr;

public record BambooHRJob(
    String id,
    String jobOpeningName,
    BambooHRLocation location,
    String departmentLabel,
    String description 
) {}