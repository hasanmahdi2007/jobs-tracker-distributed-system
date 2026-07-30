package com.distributed.job_finder.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;

@Service
public class UniversalJobScraperService {

    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    
    // Most common path variations for career portals
    private static final String[] CAREER_PATHS = {
        "/careers", "/jobs", "/work-with-us", "/join-us", "/about/careers"
    };

    public UniversalJobScraperService(WebClient.Builder webClientBuilder, ObjectMapper objectMapper) {
        this.webClient = webClientBuilder
                .defaultHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                .build();
        this.objectMapper = objectMapper;
    }

    /**
     * Entry point: Takes a domain (e.g. "https://careem.com") and returns a Flux of JSON-LD job objects found.
     */
    public Flux<JsonNode> scrapeJobsFromDomain(String baseDomain) {
        return findActiveCareerUrl(baseDomain)
                .flatMapMany(this::downloadAndExtractJsonLd);
    }

    /**
     * Tries career paths sequentially until one returns HTTP 200 OK
     */
    private Mono<String> findActiveCareerUrl(String baseDomain) {
        String cleanDomain = baseDomain.replaceAll("/+$", ""); // strip trailing slash
        
        return Flux.fromArray(CAREER_PATHS)
                .map(path -> cleanDomain + path)
                .concatMap(url -> webClient.get()
                        .uri(url)
                        .exchangeToMono(response -> {
                            if (response.statusCode().is2xxSuccessful()) {
                                System.out.println("🎯 Found active career page: " + url);
                                return Mono.just(url);
                            }
                            return Mono.empty();
                        })
                        .onErrorResume(e -> Mono.empty())
                )
                .next(); // Take only the first path that responds 200 OK
    }

    /**
     * Fetches the page HTML and extracts schema.org/JobPosting JSON-LD blocks
     */
    private Flux<JsonNode> downloadAndExtractJsonLd(String url) {
        return webClient.get()
                .uri(url)
                .retrieve()
                .bodyToMono(String.class)
                .flatMapMany(html -> {
                    List<JsonNode> jobNodes = extractJobPostingsFromHtml(html);
                    return Flux.fromIterable(jobNodes);
                })
                .onErrorResume(e -> {
                    System.err.println("❌ Failed to parse HTML for " + url + ": " + e.getMessage());
                    return Flux.empty();
                });
    }

    private List<JsonNode> extractJobPostingsFromHtml(String html) {
        List<JsonNode> jobPostings = new ArrayList<>();
        
        try {
            Document doc = Jsoup.parse(html);
            Elements jsonLdScripts = doc.select("script[type=application/ld+json]");

            for (Element script : jsonLdScripts) {
                String rawJson = script.html();
                if (rawJson.trim().isEmpty()) continue;

                JsonNode root = objectMapper.readTree(rawJson);

                // JSON-LD can be a single object or an array of objects
                if (root.isArray()) {
                    for (JsonNode node : root) {
                        if (isJobPosting(node)) jobPostings.add(node);
                    }
                } else if (root.isObject()) {
                    // Sometimes jobs are embedded inside a "@graph" wrapper
                    if (root.has("@graph") && root.get("@graph").isArray()) {
                        for (JsonNode node : root.get("@graph")) {
                            if (isJobPosting(node)) jobPostings.add(node);
                        }
                    } else if (isJobPosting(root)) {
                        jobPostings.add(root);
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("JSON-LD parse error: " + e.getMessage());
        }

        return jobPostings;
    }

    private boolean isJobPosting(JsonNode node) {
        if (!node.has("@type")) return false;
        
        JsonNode typeNode = node.get("@type");
        if (typeNode.isArray()) {
            for (JsonNode t : typeNode) {
                if ("JobPosting".equalsIgnoreCase(t.asText())) return true;
            }
        } else {
            return "JobPosting".equalsIgnoreCase(typeNode.asText());
        }
        return false;
    }
}