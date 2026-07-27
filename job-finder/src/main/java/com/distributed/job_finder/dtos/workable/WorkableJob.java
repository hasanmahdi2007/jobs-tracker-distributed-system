package com.distributed.job_finder.dtos.workable;

import com.fasterxml.jackson.annotation.JsonProperty;

public record WorkableJob(
    String id, // Workable uses alphanumeric shortcodes (e.g. "AE123456")
    String title,
    String department,
    String url,
    String description,
    @JsonProperty("employment_type") String employmentType,
    @JsonProperty("created_at") String createdAt,
    WorkableLocation location
) {}