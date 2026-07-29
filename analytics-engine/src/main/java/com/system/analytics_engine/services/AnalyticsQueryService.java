package com.system.analytics_engine.services;

import com.system.analytics_engine.dtos.SystemHealthStats;
import com.system.analytics_engine.repos.ApiRequestLogRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AnalyticsQueryService {

    private final ApiRequestLogRepo repository;

    public SystemHealthStats getSystemHealth() {
        return new SystemHealthStats(
                repository.countTotalRequests(),
                repository.getAverageLatency(),
                repository.countTotalErrors(),
                repository.countUniqueIps()
        );
    }
}