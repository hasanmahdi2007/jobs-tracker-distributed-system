package com.distributed.job_finder.dtos.smartrecruiters;

import java.util.List;

public record SmartRecruitersPageResponse(
    int limit,
    int offset,
    int totalFound,
    List<SmartRecruitersPosting> content
) {}