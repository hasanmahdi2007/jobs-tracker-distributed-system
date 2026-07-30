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

    @Value("${app.scraper.webclient.timeout-ms:5000}")
    private int timeoutMs;

    // 1. Give Spring the Builder that your DomainResolverService is asking for
    @Bean
    public WebClient.Builder webClientBuilder() {
        return WebClient.builder();
    }

    // 2. Use that Builder to create your custom timed-out WebClient
    @Bean
    public WebClient webClient(WebClient.Builder builder) {
        HttpClient httpClient = HttpClient.create()
                .responseTimeout(Duration.ofMillis(timeoutMs));

        return builder
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }
}