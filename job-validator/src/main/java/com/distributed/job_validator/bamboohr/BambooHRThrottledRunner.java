package com.distributed.job_validator.bamboohr;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.SynchronousSink;

import java.util.List;

@Component
@ConditionalOnProperty(name = "scraper.target", havingValue = "bamboohr")
public class BambooHRThrottledRunner implements CommandLineRunner {

    private final StringRedisTemplate redisTemplate;
    private final BambooHRValidatorService bambooHRValidatorService;

    // Adjust these keys as needed to match your Redis queue ingestion
    private static final String QUEUE_KEY = "queue:slugs:bamboohr";
    private static final String SUCCESS_KEY = "verified:tokens:bamboohr";

    private static final int BATCH_SIZE = 250;
    private static final int CONCURRENCY = 250;

    public BambooHRThrottledRunner(StringRedisTemplate redisTemplate,
                                   BambooHRValidatorService bambooHRValidatorService) {
        this.redisTemplate = redisTemplate;
        this.bambooHRValidatorService = bambooHRValidatorService;
    }

    @Override
    public void run(String... args) {
        System.out.println("🚀 Starting Scaled Reactive BambooHR Validator...");

        Flux.generate((SynchronousSink<List<String>> sink) -> {
            List<String> slugs = redisTemplate.opsForSet().pop(QUEUE_KEY, BATCH_SIZE);

            if (slugs != null && !slugs.isEmpty()) {
                sink.next(slugs);
            } else {
                sink.complete();
            }
        })
        .flatMapIterable(list -> list)
        .flatMap(slug -> {
            long startTime = System.currentTimeMillis();

            return bambooHRValidatorService.validateSlug(slug)
                .onErrorResume(e -> {
                    System.err.printf("⚠️ Network error for slug %s: %s%n", slug, e.getMessage());
                    return Mono.empty();
                })
                .doOnNext(isValid -> {
                    if (Boolean.TRUE.equals(isValid)) {
                        long durationMs = System.currentTimeMillis() - startTime;
                        double seconds = durationMs / 1000.0;

                        System.out.printf("🎯 BOOM! Found BambooHR instance: %s (took %.2fs)%n",
                                slug, seconds);
                        redisTemplate.opsForSet().add(SUCCESS_KEY, slug);
                    }
                });
        }, CONCURRENCY)
        .doOnComplete(() -> System.out.println("✅ BambooHR validation queue completely drained!"))
        .blockLast();
    }
}