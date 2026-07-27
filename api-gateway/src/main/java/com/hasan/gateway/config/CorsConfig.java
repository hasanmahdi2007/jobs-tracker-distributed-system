package com.hasan.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
public class CorsConfig {

    @Bean
    public CorsWebFilter corsWebFilter() {
        CorsConfiguration corsConfig = new CorsConfiguration();
        
        // 1. Allow your exact React frontend URL
        corsConfig.setAllowedOrigins(List.of("http://localhost:5173", "http://localhost:3000"));
        
        // 2. Allow all headers (including your custom X-API-KEY)
        corsConfig.setAllowedHeaders(List.of("*"));
        
        // 3. Allow all standard HTTP methods, especially OPTIONS (Preflight)
        corsConfig.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        
        // 4. Cache this rule for 1 hour so Chrome doesn't spam OPTIONS requests
        corsConfig.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        // Apply this rule to every single route in the Gateway
        source.registerCorsConfiguration("/**", corsConfig);

        return new CorsWebFilter(source);
    }
}