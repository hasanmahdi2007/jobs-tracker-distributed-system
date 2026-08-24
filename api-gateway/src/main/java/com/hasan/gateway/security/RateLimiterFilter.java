package com.hasan.gateway.security;

import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import org.springframework.core.Ordered;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.List;

@Component
public class RateLimiterFilter implements WebFilter, Ordered {

    private final ReactiveStringRedisTemplate redisTemplate;
    private final DefaultRedisScript<Long> script;

    public RateLimiterFilter(ReactiveStringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.script = new DefaultRedisScript<>();
        this.script.setLocation(new ClassPathResource("scripts/token_bucket.lua"));
        this.script.setResultType(Long.class);
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {

        String rawApiKey = exchange.getRequest().getHeaders().getFirst("X-API-KEY");
        String trackingId;
        
        // 1. DETERMINE WHO WE ARE TRACKING
        if (rawApiKey == null || rawApiKey.isEmpty()) {
            // No API key? Fall back to tracking by IP address (Free User / UI Visitor)
            String ipAddress = exchange.getRequest().getHeaders().getFirst("X-Forwarded-For");
            if (ipAddress != null && !ipAddress.isEmpty()) {
                ipAddress = ipAddress.split(",")[0].trim();
            } else {
                ipAddress = exchange.getRequest().getRemoteAddress() != null ? 
                        exchange.getRequest().getRemoteAddress().getAddress().getHostAddress() : "unknown-ip";
            }
            trackingId = "anon_ip:" + ipAddress; // Unique prefix for anonymous users
        } else {
            // Has an API key? Track them by their API key (B2B Client)
            trackingId = rawApiKey;
        }

        // 2. READ THE LIMITS (Assigned by the Bouncer upstream)
        // If they are anonymous, the Bouncer gave them "15" capacity. If PRO, "3000".
        String capacity = exchange.getAttributeOrDefault("user_capacity", "15");
        String rate = exchange.getAttributeOrDefault("user_rate", "2");
        
        String now = String.valueOf(Instant.now().getEpochSecond());
        String requested = "1";

        // 3. CREATE REDIS KEYS BASED ON WHO THEY ARE
        List<String> keys = List.of("tokens:" + trackingId, "timestamp:" + trackingId);
        List<String> args = List.of(rate, capacity, now, requested);

        // 4. EXECUTE THE TOKEN BUCKET SCRIPT
        return redisTemplate.execute(script, keys, args)
                .next()
                .flatMap(result -> {
                    if (result == 1L) {
                        return chain.filter(exchange); 
                    } else {
                        exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
                        return exchange.getResponse().setComplete(); 
                    }
                });
    }

    @Override
    public int getOrder() {
        return -1; 
    }
}