package com.distributed.job_validator.lever;

import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
public class LeverValidatorService {

    private final WebClient webClient;
    private static final String LEVER_API_BASE = "https://api.lever.co/v0/postings";

    public LeverValidatorService(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder
                .defaultHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .build();
    }

    /**
     * Checks if a company slug exists on Lever by querying its JSON endpoint.
     * Returns true if the endpoint returns HTTP 200 OK.
     */
    public Mono<Boolean> validateSlug(String slug) {
        String targetUrl = String.format("%s/%s?mode=json", LEVER_API_BASE, slug.trim());

        return webClient.get()
                .uri(targetUrl)
                .exchangeToMono(response -> {
                    if (response.statusCode().is2xxSuccessful()) {
                        return Mono.just(true);
                    }
                    return Mono.just(false);
                })
                .onErrorResume(e -> Mono.just(false));
    }
}