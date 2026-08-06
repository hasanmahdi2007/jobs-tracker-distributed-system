package com.distributed.job_finder.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

@Configuration
@ConfigurationProperties(prefix = "app.scrapers.smartrecruiters")
public class TalenteraConfig {

    private String baseUrl;
    private List<String> targetBoards = new ArrayList<>();

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    // BULLETPROOF FIX: Prevents NullPointerException if YAML is empty
    public List<String> getTargetBoards() {
        return this.targetBoards;
    }

    public void setTargetBoards(List<String> targetBoards) {
        this.targetBoards = targetBoards;
    }
}