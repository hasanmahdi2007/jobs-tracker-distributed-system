package com.distributed.job_finder.config;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "app.scrapers.bamboohr")
public class BambooHRConfig {

    private List<String> targetBoards;

    public List<String> getTargetBoards() {
        return this.targetBoards;
    }

    public void setTargetBoards(List<String> targetBoards) {
        this.targetBoards = targetBoards;
    }
}