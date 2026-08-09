package com.system.analytics_engine.services;

import com.system.analytics_engine.dtos.SystemHealthStats;
import com.system.analytics_engine.repos.ApiRequestLogRepo;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class AnalyticsQueryService {

    private final ApiRequestLogRepo repository;

    public AnalyticsQueryService(ApiRequestLogRepo repository) {
        this.repository = repository;
    }

    public Mono<SystemHealthStats> getSystemHealth() {
        return Mono.zip(
                repository.countTotalRequests(),
                repository.getAverageLatency(),
                repository.countTotalErrors(),
                repository.countUniqueIps()
        ).map(tuple -> new SystemHealthStats(
                tuple.getT1(),
                tuple.getT2(),
                tuple.getT3(),
                tuple.getT4()
        ));
    }
}