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

    // ClientRepo is completely gone!
    public ApiAuthenticationFilter(ApiKeyRepo apiKeyRepo, ReactiveStringRedisTemplate redisTemplate) {
        this.apiKeyRepo = apiKeyRepo;
        this.redisTemplate = redisTemplate;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {

        if (exchange.getRequest().getMethod() == HttpMethod.OPTIONS) {
            return chain.filter(exchange);
        }

        String path = exchange.getRequest().getURI().getPath();
        HttpMethod method = exchange.getRequest().getMethod();

        // ALLOW ROUTE: Client Registration AND Public Job Viewing
        if (path.startsWith("/api/v1/clients/register") || 
           (path.startsWith("/api/v1/jobs") && method == HttpMethod.GET)) {
            return chain.filter(exchange);
        }

        String rawApiKey = exchange.getRequest().getHeaders().getFirst("X-API-KEY");
        if (rawApiKey == null || rawApiKey.isEmpty()) {
            return rejectRequest(exchange, "Missing X-API-KEY header");
        }

        String hashedIncomingKey = SecurityUtil.hashKey(rawApiKey);
        String cacheKey = "auth:" + hashedIncomingKey;

        return redisTemplate.opsForValue().get(cacheKey)
                .switchIfEmpty(Mono.defer(() -> 
                    // Single repository lookup! The tier is right on the ApiKey.
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