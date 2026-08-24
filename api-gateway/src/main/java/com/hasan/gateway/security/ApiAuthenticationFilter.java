package com.hasan.gateway.security;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

import org.springframework.core.Ordered;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;

import com.hasan.gateway.repos.ApiKeyRepo;

import reactor.core.publisher.Mono;

@Component
public class ApiAuthenticationFilter implements WebFilter, Ordered {

    private final ApiKeyRepo apiKeyRepo;
    private final ReactiveStringRedisTemplate redisTemplate;

    public ApiAuthenticationFilter(ApiKeyRepo apiKeyRepo, ReactiveStringRedisTemplate redisTemplate) {
        this.apiKeyRepo = apiKeyRepo;
        this.redisTemplate = redisTemplate;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {

        // 1. Let CORS preflight requests through cleanly
        if (exchange.getRequest().getMethod().name().equalsIgnoreCase("OPTIONS")) {
            return chain.filter(exchange);
        }

        String path = exchange.getRequest().getURI().getPath();
        String rawApiKey = exchange.getRequest().getHeaders().getFirst("X-API-KEY");

        if (path.startsWith("/api/v1/clients") || path.startsWith("/actuator")) {
            return chain.filter(exchange);
        }

        // 2. PUBLIC SLOW LANE: No API Key provided
        if (rawApiKey == null || rawApiKey.trim().isEmpty()) {
            
            // Match any job retrieval endpoint robustly
            if (path.contains("/jobs") && exchange.getRequest().getMethod().name().equalsIgnoreCase("GET")) {
                exchange.getAttributes().put("user_capacity", "15");
                exchange.getAttributes().put("user_rate", "2");
                return chain.filter(exchange);
            }
            // other than GET /jobs
            return rejectRequest(exchange, "Missing X-API-KEY header");
        }

        // 3. API Key Path (B2B Clients)
        String hashedIncomingKey = SecurityUtil.hashKey(rawApiKey.trim());
        String cacheKey = "auth:" + hashedIncomingKey;

        return redisTemplate.opsForValue().get(cacheKey)
                .switchIfEmpty(Mono.defer(() -> 
                    apiKeyRepo.findByKeyHash(hashedIncomingKey)
                        .flatMap(apiKey -> {
                            String tier = apiKey.getTier(); 
                            String limits = "PRO".equalsIgnoreCase(tier) ? "3000:200" : "20:5";

                            return redisTemplate.opsForValue()
                                    .set(cacheKey, limits, Duration.ofHours(24))
                                    .thenReturn(limits);
                        })
                        .switchIfEmpty(
                            redisTemplate.opsForValue()
                                    .set(cacheKey, "invalid", Duration.ofMinutes(5))
                                    .thenReturn("invalid")
                        )
                ))
                .flatMap(cachedValue -> {
                    if ("invalid".equals(cachedValue)) {
                        return rejectRequest(exchange, "Invalid API Key"); 
                    } else {
                        String[] limitParts = cachedValue.split(":");
                        String capacity = limitParts[0];
                        String rate = limitParts[1];

                        exchange.getAttributes().put("user_capacity", capacity);
                        exchange.getAttributes().put("user_rate", rate);

                        return chain.filter(exchange); 
                    }
                });
    }

    private Mono<Void> rejectRequest(ServerWebExchange exchange, String message) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        
        // This is the magic fix! Attach explicit CORS header on errors so browser exposes actual status message
        String origin = exchange.getRequest().getHeaders().getOrigin();
        if (origin != null) {
            exchange.getResponse().getHeaders().set("Access-Control-Allow-Origin", origin);
            exchange.getResponse().getHeaders().set("Access-Control-Allow-Credentials", "true");
        }
        
        String body = "{\"error\": \"" + message + "\"}";
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        
        return exchange.getResponse().writeWith(
            Mono.just(exchange.getResponse().bufferFactory().wrap(bytes))
        );
    }

    @Override
    public int getOrder() {
        return -2; 
    }
}