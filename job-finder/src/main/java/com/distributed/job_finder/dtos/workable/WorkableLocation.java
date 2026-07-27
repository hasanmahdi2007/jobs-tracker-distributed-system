package com.distributed.job_finder.dtos.workable;

import com.fasterxml.jackson.annotation.JsonProperty;

public record WorkableLocation(
    String country,
    @JsonProperty("country_code") String countryCode,
    String region,
    String city,
    boolean telecommuting // Workable's flag for remote roles
) {}