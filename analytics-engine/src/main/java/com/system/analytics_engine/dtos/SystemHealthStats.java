package com.system.analytics_engine.dtos;

public class SystemHealthStats {

    private final long totalRequests;
    private final double averageLatencyMs;
    private final long totalErrors; // 400s and 500s
    private final long activeUsers; // Count of unique IPs

    public SystemHealthStats(long totalRequests, double averageLatencyMs, long totalErrors, long activeUsers) {
        this.totalRequests = totalRequests;
        this.averageLatencyMs = averageLatencyMs;
        this.totalErrors = totalErrors;
        this.activeUsers = activeUsers;
    }

    public long getTotalRequests() {
        return totalRequests;
    }

    public double getAverageLatencyMs() {
        return averageLatencyMs;
    }

    public long getTotalErrors() {
        return totalErrors;
    }

    public long getActiveUsers() {
        return activeUsers;
    }
}