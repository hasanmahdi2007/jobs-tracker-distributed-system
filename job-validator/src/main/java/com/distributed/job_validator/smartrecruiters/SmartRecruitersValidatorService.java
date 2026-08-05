package com.distributed.job_validator.smartrecruiters;

import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;

@Service
public class SmartRecruitersValidatorService {

    private final WebClient webClient;

    public SmartRecruitersValidatorService(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder
                .baseUrl("https://api.smartrecruiters.com/v1/companies")
                .defaultHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .build();
    }

    public Mono<Boolean> validateSlug(String slug) {
        return webClient.get()
                .uri("/{slug}/postings?limit=1", slug.trim())
                .retrieve()
                // Use a standard Java Map instead of JsonNode - no special imports needed!
                .bodyToMono(Map.class)
                .map(json -> {
                    // SR returns 200 OK for FAKE companies! 
                    // Verify the response has the totalFound key and the count is > 0
                    if (json != null && json.containsKey("totalFound")) {
                        Object total = json.get("totalFound");
                        if (total instanceof Number) {
                            return ((Number) total).intValue() > 0;
                        }
                    }
                    return false;
                })
                .onErrorResume(Exception.class, e -> {
                    // Catches 403/404s AND Cloudflare HTML captchas (which fail to parse as a Map)
                    return Mono.just(false); 
                });
    }
}