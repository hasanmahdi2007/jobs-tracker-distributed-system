package com.distributed.job_finder;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;

public class FindAllHiddenTokens {
    public static void main(String[] args) {
        List<String> globalCompanies = Arrays.asList(
            "visioneers", "asico", "ebra", "meraki-global", "gsstech-group", 
            "mondia", "hudabeauty", "drivenproperties", "dubizzle", "servme", 
            "imdad", "agility", "propertyfinder", "bayzat", "careem", "talabat",
            "openai", "stripe", "revolut", "doordash", "figma", "notion"
        );

        String[] suffixes = {"", "hq", "app", "inc", "group", "global", "careers", "tech", "team"};
        String[] prefixes = {"", "get", "join", "weare"};

        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .executor(Executors.newFixedThreadPool(15))
                .build();
        
        int totalPermutations = globalCompanies.size() * suffixes.length * prefixes.length;
        System.out.println("Generated " + totalPermutations + " permutations across " + globalCompanies.size() + " companies.");
        System.out.println("Blasting Workable Public API endpoints safely... Please wait.\n");

        List<CompletableFuture<Void>> futures = new ArrayList<>();

        // Standard loops avoid all compiler type-inference and red underline issues
        for (String company : globalCompanies) {
            for (String prefix : prefixes) {
                for (String suffix : suffixes) {
                    String guess = prefix + company + suffix;
                    
                    HttpRequest req = HttpRequest.newBuilder()
                        .uri(URI.create("https://www.workable.com/api/accounts/" + guess + "?details=false"))
                        .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                        .header("Accept", "application/json")
                        .GET().build();

                    CompletableFuture<Void> future = client.sendAsync(req, HttpResponse.BodyHandlers.discarding())
                        .handle((res, ex) -> {
                            if (ex == null && res.statusCode() == 200) {
                                System.out.println("✅ FOUND LIVE WORKABLE TOKEN: \"" + guess + "\" (matched: " + company + ")");
                            }
                            return null;
                        });

                    futures.add(future);
                }
            }
        }

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        System.out.println("\nWorkable token scan complete!");
        System.exit(0);
    }
}