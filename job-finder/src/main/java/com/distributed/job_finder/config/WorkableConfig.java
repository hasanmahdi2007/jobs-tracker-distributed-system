package com.distributed.job_finder.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import java.util.List;

@Configuration
@ConfigurationProperties(prefix = "app.scraper.workable")
public class WorkableConfig {

    // Default URL, can be overridden in properties
    private String baseUrl = "https://www.workable.com/api/accounts";
    private List<String> targetBoards;

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public List<String> getTargetBoards() {
        return targetBoards;
    }

    public void setTargetBoards(List<String> targetBoards) {
        this.targetBoards = targetBoards;
    }
}