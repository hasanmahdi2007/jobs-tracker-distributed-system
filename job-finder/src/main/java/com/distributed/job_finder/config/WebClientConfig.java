package com.distributed.job_finder.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;

@Configuration
public class WebClientConfig {

    // I added a ":5000" fallback just in case it's missing from application.yml
    @Value("${app.scraper.webclient.timeout-ms:5000}")
    private int timeoutMs;

    @Bean
    public WebClient webClient() { // <-- Removed the parameter here!
        
        HttpClient httpClient = HttpClient.create()
                .responseTimeout(Duration.ofMillis(timeoutMs));

        // Create the builder manually right here
        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }
}