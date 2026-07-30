package com.system.analytics_engine.repos;

import com.system.analytics_engine.entities.ApiRequestLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface ApiRequestLogRepo extends JpaRepository<ApiRequestLog, Long> {
    
    @Query("SELECT COUNT(l) FROM ApiRequestLog l")
    long countTotalRequests();

    @Query("SELECT COALESCE(AVG(l.latencyMs), 0) FROM ApiRequestLog l")
    double getAverageLatency();

    @Query("SELECT COUNT(l) FROM ApiRequestLog l WHERE l.status >= 400")
    long countTotalErrors();

    @Query("SELECT COUNT(DISTINCT l.ip) FROM ApiRequestLog l")
    long countUniqueIps();
}