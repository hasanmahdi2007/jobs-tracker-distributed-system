package com.distributed.job_validator;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;

import java.time.Duration;

@Configuration
public class WebClientConfig {

    @Bean
    public WebClient.Builder webClientBuilder() {
        
        // 1. Create a Connection Pool to stop your router's NAT table from filling up
        ConnectionProvider provider = ConnectionProvider.builder("global-connection-pool")
                .maxConnections(300) // Higher than your 250 concurrency limit
                .maxIdleTime(Duration.ofSeconds(15)) // Closes idle connections quickly to free router memory
                .maxLifeTime(Duration.ofSeconds(60)) 
                .pendingAcquireTimeout(Duration.ofSeconds(45))
                .build();

        // 2. Attach the pool to an HttpClient and force TCP Keep-Alive
        HttpClient httpClient = HttpClient.create(provider)
                .keepAlive(true);

        // 3. Return your original Builder, but with the new connection pool attached
        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient));
    }
}