package com.system.analytics_engine.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SystemHealthStats {
    private long totalRequests;
    private double averageLatencyMs;
    private long totalErrors; // 400s and 500s
    private long activeUsers; // Count of unique IPs
}