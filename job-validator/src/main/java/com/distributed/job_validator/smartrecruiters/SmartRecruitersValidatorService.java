package com.distributed.job_validator.smartrecruiters;

import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

@Service
public class SmartRecruitersValidatorService {

    private final WebClient webClient;

    public SmartRecruitersValidatorService(WebClient.Builder webClientBuilder) {
        // SmartRecruiters API endpoint for checking company postings
        this.webClient = webClientBuilder.baseUrl("https://api.smartrecruiters.com/v1/companies").build();
    }

    public Mono<Boolean> validateSlug(String slug) {
        return webClient.get()
                .uri("/{slug}/postings?limit=1", slug)
                .retrieve()
                .toBodilessEntity() // We only care about the 200 HTTP status code, not the JSON body
                .map(response -> response.getStatusCode().is2xxSuccessful())
                .onErrorResume(WebClientResponseException.class, e -> {
                    if (e.getStatusCode().is4xxClientError()) {
                        return Mono.just(false); // 404 Not Found -> Invalid company slug
                    }
                    return Mono.error(e); // Let timeouts propagate so the Runner doesn't mark it as failed
                })
                .onErrorReturn(false);
    }
}