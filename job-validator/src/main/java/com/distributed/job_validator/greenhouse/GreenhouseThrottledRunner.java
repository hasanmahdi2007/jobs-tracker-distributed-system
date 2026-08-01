package com.distributed.job_validator.greenhouse;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.SynchronousSink;

import java.util.List;

@Component
@ConditionalOnProperty(name = "scraper.target", havingValue = "greenhouse")
public class GreenhouseThrottledRunner implements CommandLineRunner {

    private final StringRedisTemplate redisTemplate;
    private final GreenhouseValidatorService greenhouseValidatorService;

    // Per your instruction, these are intentionally left as 'lever'
    private static final String QUEUE_KEY = "queue:slugs:lever";
    private static final String SUCCESS_KEY = "verified:tokens:lever";

    private static final int BATCH_SIZE = 250;
    private static final int CONCURRENCY = 250; 

    public GreenhouseThrottledRunner(StringRedisTemplate redisTemplate, 
                                     GreenhouseValidatorService greenhouseValidatorService) {
        this.redisTemplate = redisTemplate;
        this.greenhouseValidatorService = greenhouseValidatorService;
    }

    @Override
    public void run(String... args) {
        System.out.println("🚀 Starting Scaled Reactive Greenhouse Validator...");

        Flux.generate((SynchronousSink<List<String>> sink) -> {
            List<String> slugs = redisTemplate.opsForSet().pop(QUEUE_KEY, BATCH_SIZE);

            if (slugs != null && !slugs.isEmpty()) {
                sink.next(slugs);
            } else {
                sink.complete();
            }
        })
        .flatMapIterable(list -> list) // OPTIMIZED: prevents unnecessary Flux object creation
        .flatMap(slug -> {
            long startTime = System.currentTimeMillis();

            return greenhouseValidatorService.validateSlug(slug)
                .onErrorResume(e -> {
                    // ADDED: Fault tolerance. Prevents one network timeout from crashing the whole 250-slug batch.
                    System.err.printf("⚠️ Network error for slug %s: %s%n", slug, e.getMessage());
                    return Mono.empty(); 
                })
                .doOnNext(isValid -> {
                    if (Boolean.TRUE.equals(isValid)) {
                        long durationMs = System.currentTimeMillis() - startTime;
                        double seconds = durationMs / 1000.0;
                        
                        System.out.printf("🎯 BOOM! Found Greenhouse instance: %s (took %.2fs)%n", 
                                slug, seconds);
                        redisTemplate.opsForSet().add(SUCCESS_KEY, slug);
                    } 
                });
        }, CONCURRENCY)
        .doOnComplete(() -> System.out.println("✅ Greenhouse validation queue completely drained!"))
        .blockLast();
    }
}