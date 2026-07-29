package com.jobfinder.validator.workable;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

@Service
public class WorkableValidatorService {

    private final WebClient webClient;

    @Autowired
    public WorkableValidatorService(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder
                .baseUrl("https://apply.workable.com/api/v1/widget/accounts/")
                .defaultHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                .defaultHeader("Accept", "application/json")
                .build();
    }

    /**
     * Non-blocking check for Workable slug existence.
     * @param slug Candidate company token from Redis
     * @return Mono<Boolean> true if HTTP 200, false if HTTP 404/Error
     */
    public Mono<Boolean> validateSlug(String slug) {
        return webClient.get()
                .uri(slug)
                .retrieve()
                .toBodilessEntity()
                .map(response -> response.getStatusCode().is2xxSuccessful())
                .onErrorResume(WebClientResponseException.NotFound.class, e -> Mono.just(false))
                .onErrorResume(WebClientResponseException.TooManyRequests.class, e -> {
                    // Quick safety net if Workable rate limits
                    System.err.println("⚠️ Rate limited (429) on slug: " + slug);
                    return Mono.just(false);
                })
                .onErrorResume(e -> Mono.just(false));
    }
}