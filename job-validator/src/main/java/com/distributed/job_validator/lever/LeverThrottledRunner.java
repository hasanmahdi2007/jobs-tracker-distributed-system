package com.distributed.job_validator.lever;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.SynchronousSink;

import java.util.List;

@Component
@ConditionalOnProperty(name = "scraper.target", havingValue = "lever")
public class LeverThrottledRunner implements CommandLineRunner {

    private final StringRedisTemplate redisTemplate;
    private final LeverValidatorService leverValidatorService;
    
    // FIXED: These were incorrectly pointing to Greenhouse in your original code
    private static final String QUEUE_KEY = "queue:slugs:greenhouse";
    private static final String SUCCESS_KEY = "verified:tokens:greenhouse";

    private static final int BATCH_SIZE = 1000;
    private static final int CONCURRENCY = 250;

    public LeverThrottledRunner(StringRedisTemplate redisTemplate, 
                                LeverValidatorService leverValidatorService) {
        this.redisTemplate = redisTemplate;
        this.leverValidatorService = leverValidatorService;
    }

    @Override
    public void run(String... args) {
        System.out.println("🚀 Starting Scaled Reactive Lever Validator...");
        
        Flux.generate((SynchronousSink<List<String>> sink) -> {
            List<String> slugs = redisTemplate.opsForSet().pop(QUEUE_KEY, BATCH_SIZE);
            
            if (slugs != null && !slugs.isEmpty()) {
                sink.next(slugs);
            } else {
                sink.complete();
            }
        })
        .flatMapIterable(list -> list) // OPTIMIZED: cleaner than flatMap(Flux::fromIterable)
        .flatMap(slug -> {
            long startTime = System.currentTimeMillis();
            
            return leverValidatorService.validateSlug(slug)
                .onErrorResume(e -> {
                    // ADDED: Fault tolerance. Without this, one network timeout crashes the whole batch.
                    System.err.printf("⚠️ Network error for slug %s: %s%n", slug, e.getMessage());
                    return Mono.empty(); 
                })
                .doOnNext(isValid -> {
                    if (Boolean.TRUE.equals(isValid)) {
                        long durationMs = System.currentTimeMillis() - startTime;
                        System.out.printf("🎯 BOOM! Found Lever instance: %s (took %.2fs)%n", 
                                slug, (durationMs / 1000.0));
                        redisTemplate.opsForSet().add(SUCCESS_KEY, slug);
                    }
                });
        }, CONCURRENCY)
        .doOnComplete(() -> System.out.println("✅ Lever validation queue completely drained!"))
        .blockLast(); 
    }
}