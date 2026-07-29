package com.distributed.job_finder.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Data
@Configuration
@ConfigurationProperties(prefix = "scrapers.talentera")
public class TalenteraConfig {
    private List<String> targetBoards;
}