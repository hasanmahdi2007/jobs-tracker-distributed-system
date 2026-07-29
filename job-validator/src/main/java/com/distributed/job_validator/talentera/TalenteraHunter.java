package com.distributed.job_validator.talentera;

import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Service
public class TalenteraHunter {

    private final HttpClient httpClient;

    public TalenteraHunter() {
        this.httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .connectTimeout(Duration.ofSeconds(3))
                .build();
    }

    public boolean isTalentera(String slug) {
        if (slug == null || slug.isBlank()) {
            return false;
        }

        String cleanSlug = slug.toLowerCase().replaceAll("[^a-z0-9-]", "");

        // 1. Direct Talentera Subdomain Check
        if (checkUrl("https://" + cleanSlug + ".talentera.com")) {
            return true;
        }

        // 2. Custom Domain Probes with Footprint Verification
        if (checkUrlAndFootprint("https://careers." + cleanSlug + ".com")) {
            return true;
        }

        if (checkUrlAndFootprint("https://jobs." + cleanSlug + ".com")) {
            return true;
        }

        return false;
    }

    private boolean checkUrl(String url) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(3))
                    .GET()
                    .build();

            HttpResponse<Void> response = httpClient.send(request, HttpResponse.BodyHandlers.discarding());
            int code = response.statusCode();
            return (code >= 200 && code < 400);
        } catch (Exception e) {
            return false;
        }
    }

    private boolean checkUrlAndFootprint(String url) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(4))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 200 && response.statusCode() < 400) {
                String body = response.body().toLowerCase();
                return body.contains("powered by talentera")
                        || body.contains("bayt.com")
                        || body.contains("talentera");
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }
}