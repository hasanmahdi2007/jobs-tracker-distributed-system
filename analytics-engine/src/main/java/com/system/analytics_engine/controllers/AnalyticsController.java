package com.system.analytics_engine.controllers;

import com.system.analytics_engine.dtos.SystemHealthStats;
import com.system.analytics_engine.services.AnalyticsQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsQueryService analyticsQueryService;

    @GetMapping("/health")
    public SystemHealthStats getHealthStats() {
        return analyticsQueryService.getSystemHealth();
    }
}