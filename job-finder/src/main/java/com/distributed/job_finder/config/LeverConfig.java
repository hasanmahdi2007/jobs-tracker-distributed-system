package com.distributed.job_finder.config;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "app.scrapers.lever")
public class LeverConfig {

    private String baseUrl;
    private List<String> targetBoards;

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public List<String> getTargetBoards() {
        return this.targetBoards;
    }

    public void setTargetBoards(List<String> targetBoards) {
        this.targetBoards = targetBoards;
    }
}