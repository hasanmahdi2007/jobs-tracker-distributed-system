package com.distributed.job_validator.talentera;

import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Service
public class TalenteraHunter {

    private final WebClient webClient;

    public TalenteraHunter(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder
                .defaultHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                .build();
    }

    public Mono<Boolean> isTalentera(String slug) {
        if (slug == null || slug.isBlank()) {
            return Mono.just(false);
        }

        String cleanSlug = slug.toLowerCase().replaceAll("[^a-z0-9-]", "");

        // 1. Direct Talentera Subdomain Check
        return checkUrl("https://" + cleanSlug + ".talentera.com")
                // 2. Careers Custom Domain Probe
                .flatMap(found -> found ? Mono.just(true) : checkUrlAndFootprint("https://careers." + cleanSlug + ".com"))
                // 3. Jobs Custom Domain Probe
                .flatMap(found -> found ? Mono.just(true) : checkUrlAndFootprint("https://jobs." + cleanSlug + ".com"));
    }

    private Mono<Boolean> checkUrl(String url) {
        return webClient.get()
                .uri(url)
                .retrieve()
                .toBodilessEntity()
                .map(response -> response.getStatusCode().is2xxSuccessful() || response.getStatusCode().is3xxRedirection())
                .timeout(Duration.ofSeconds(3))
                .onErrorResume(e -> Mono.just(false));
    }

    private Mono<Boolean> checkUrlAndFootprint(String url) {
        return webClient.get()
                .uri(url)
                .retrieve()
                .bodyToMono(String.class)
                .map(String::toLowerCase)
                .map(body -> body.contains("powered by talentera")
                        || body.contains("bayt.com")
                        || body.contains("talentera"))
                .timeout(Duration.ofSeconds(4))
                .onErrorResume(e -> Mono.just(false));
    }
}