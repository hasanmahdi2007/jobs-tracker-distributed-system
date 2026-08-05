package com.distributed.job_validator.smartrecruiters;

import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

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
                .bodyToMono(String.class)
                .map(body -> body != null && body.contains("\"content\""))
                .onErrorResume(WebClientResponseException.class, e -> {
                    if (e.getStatusCode().is4xxClientError()) {
                        return Mono.just(false); 
                    }
                    return Mono.<Boolean>error(e); 
                })
                .onErrorReturn(false);
    }
}