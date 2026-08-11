package com.distributed.job_finder.dtos.bamboohr;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record BambooHRJobResponse(
    List<BambooHRJob> result
) {}