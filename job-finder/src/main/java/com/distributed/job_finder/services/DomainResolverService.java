package com.distributed.job_finder.services;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
public class DomainResolverService {

    private final WebClient webClient;

    public DomainResolverService(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder
                .baseUrl("https://api.clearout.io/public/companies")
                .build();
    }

    public Mono<String> getDomainForCompany(String companyName) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/autocomplete")
                        .queryParam("query", companyName)
                        .build())
                .retrieve()
                .bodyToMono(JsonNode.class)
                .map(response -> {
                    JsonNode dataArray = response.path("data");
                    if (dataArray.isArray() && !dataArray.isEmpty()) {
                        JsonNode bestMatch = dataArray.get(0);
                        int confidence = bestMatch.path("confidence_score").asInt(0);
                        String domain = bestMatch.path("domain").asText("");

                        if (confidence >= 80 && !domain.isEmpty()) {
                            return "https://" + domain;
                        }
                    }
                    return "";
                })
                .onErrorResume(e -> {
                    System.err.println("❌ API Error for " + companyName + ": " + e.getMessage());
                    return Mono.just("");
                });
    }
}