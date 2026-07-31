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

    public Mono<Boolean> validateSlug(String slug) {
        String targetUrl = String.format("%s/%s?mode=json", LEVER_API_BASE, slug.trim());

        return webClient.get()
                .uri(targetUrl)
                .exchangeToMono(response -> 
                    response.releaseBody()
                            .thenReturn(response.statusCode().is2xxSuccessful())
                )
                .onErrorReturn(false);
    }
}