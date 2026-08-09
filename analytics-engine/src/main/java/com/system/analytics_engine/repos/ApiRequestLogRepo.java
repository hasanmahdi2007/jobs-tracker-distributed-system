package com.system.analytics_engine.repos;

import com.system.analytics_engine.entities.ApiRequestLog;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Repository
public interface ApiRequestLogRepo extends ReactiveCrudRepository<ApiRequestLog, UUID> {
    
    @Query("SELECT COUNT(*) FROM api_request_logs")
    Mono<Long> countTotalRequests();

    @Query("SELECT COALESCE(AVG(latency_ms), 0) FROM api_request_logs")
    Mono<Double> getAverageLatency();

    @Query("SELECT COUNT(*) FROM api_request_logs WHERE status >= 400")
    Mono<Long> countTotalErrors();

    @Query("SELECT COUNT(DISTINCT ip) FROM api_request_logs")
    Mono<Long> countUniqueIps();
}