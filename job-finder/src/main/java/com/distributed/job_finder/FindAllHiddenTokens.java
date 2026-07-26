package com.distributed.job_finder;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public class FindAllHiddenTokens {
    public static void main(String[] args) {
        // Massive list of global tech, AI, infrastructure, and fintech giants
        List<String> globalCompanies = Arrays.asList(
            "openai", "anthropic", "scale", "cohere", "huggingface", "midjourney", "perplexity",
            "stripe", "plaid", "brex", "ramp", "chime", "robinhood", "coinbase", "kraken",
            "gemini", "revolut", "monzo", "n26", "qonto", "wise", "adyen", "checkout",
            "airbnb", "doordash", "instacart", "lyft", "uber", "discord", "twitch",
            "reddit", "pinterest", "dropbox", "figma", "asana", "gitlab", "github",
            "databricks", "snowflake", "palantir", "mongodb", "elastic", "datadog",
            "cloudflare", "fastly", "hashicorp", "confluent", "postman", "docker",
            "canva", "notion", "airtable", "linear", "loom", "framer", "webflow",
            "hubspot", "zendesk", "docusign", "box", "smartsheet", "pagerduty",
            "crowdstrike", "zscaler", "okta", "snyk", "lacework", "wiz", "1password",
            "shopify", "squarespace", "wix", "etsy", "wayfair", "peloton", "strava",
            "duolingo", "coursera", "udemy", "masterclass", "roblox", "unity", "epicgames",
            "canonical", "remote", "deel", "papaya", "multiplier", "safetywing"
        );

        String[] suffixes = {"", "hq", "app", "inc", "group", "global", "careers", "tech", "team"};
        String[] prefixes = {"", "get", "join", "weare"};

        // 40 parallel threads to make short work of thousands of URLs
        HttpClient client = HttpClient.newBuilder().executor(Executors.newFixedThreadPool(40)).build();
        
        int totalPermutations = globalCompanies.size() * suffixes.length * prefixes.length;
        System.out.println("Generated " + totalPermutations + " permutations across " + globalCompanies.size() + " global companies.");
        System.out.println("Blasting Greenhouse Global API endpoints... Please wait.\n");

        List<CompletableFuture<Void>> futures = globalCompanies.stream().flatMap(company -> 
            Arrays.stream(prefixes).flatMap(prefix -> 
                Arrays.stream(suffixes).map(suffix -> {
                    String guess = prefix + company + suffix;
                    HttpRequest req = HttpRequest.newBuilder()
                        .uri(URI.create("https://boards-api.greenhouse.io/v1/boards/" + guess + "/jobs"))
                        .GET().build();

                    return client.sendAsync(req, HttpResponse.BodyHandlers.discarding())
                        .thenAccept(res -> {
                            if (res.statusCode() == 200) {
                                System.out.println("✅ FOUND LIVE TOKEN: \"" + guess + "\" (matched: " + company + ")");
                            }
                        });
                })
            )
        ).collect(Collectors.toList());

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        System.out.println("\nGlobal scan complete! Copy the active tokens straight into your application.yml.");
        System.exit(0);
    }
}