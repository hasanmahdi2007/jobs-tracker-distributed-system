package com.system.analytics_engine.controllers;

import com.system.analytics_engine.dtos.SystemHealthStats;
import com.system.analytics_engine.services.AnalyticsQueryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/analytics")
public class AnalyticsController {

    private final AnalyticsQueryService analyticsQueryService;

    public AnalyticsController(AnalyticsQueryService analyticsQueryService) {
        this.analyticsQueryService = analyticsQueryService;
    }

    @GetMapping("/health")
    public Mono<SystemHealthStats> getHealthStats() {
        return analyticsQueryService.getSystemHealth();
    }
}