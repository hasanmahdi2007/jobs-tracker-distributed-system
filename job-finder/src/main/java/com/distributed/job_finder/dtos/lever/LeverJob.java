package com.distributed.job_finder.dtos.lever;

import com.fasterxml.jackson.annotation.JsonProperty;

public record LeverJob(
    String id,
    String text, 
    @JsonProperty("hostedUrl") String hostedUrl,
    @JsonProperty("descriptionPlain") String descriptionPlain,
    LeverCategories categories
) {}