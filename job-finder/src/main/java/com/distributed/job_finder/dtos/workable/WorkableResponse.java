package com.distributed.job_finder.dtos.workable;

import java.util.List;

public record WorkableResponse(
    String name,
    String description,
    List<WorkableJob> jobs // The API wraps the actual job list in this array
) {}