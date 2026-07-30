package com.distributed.job_validator.greenhouse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

@Service
public class GreenhouseValidatorService {

    private final WebClient webClient;

    @Autowired
    public GreenhouseValidatorService(WebClient.Builder webClientBuilder) {
        this.webClient = WebClient.builder()
                .baseUrl("https://boards-api.greenhouse.io/v1/boards/")
                .defaultHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                .defaultHeader("Accept", "application/json")
                .build();
    }

    /**
     * Non-blocking check for Greenhouse slug existence.
     * @param slug Candidate company token from Redis
     * @return Mono<Boolean> true if HTTP 200, false if HTTP 404/Error
     */
    public Mono<Boolean> validateSlug(String slug) {
        return webClient.get()
                .uri(slug) // Validates the board metadata exists
                .retrieve()
                .toBodilessEntity()
                .map(response -> response.getStatusCode().is2xxSuccessful())
                .onErrorResume(WebClientResponseException.NotFound.class, e -> Mono.just(false))
                .onErrorResume(WebClientResponseException.TooManyRequests.class, e -> {
                    System.err.println("⚠️ Rate limited (429) on slug: " + slug + " - backing off...");
                    return Mono.just(false);
                })
                .onErrorResume(e -> Mono.just(false));
    }
}