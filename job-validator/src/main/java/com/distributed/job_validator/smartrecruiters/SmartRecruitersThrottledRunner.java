package com.distributed.job_validator.smartrecruiters;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.SynchronousSink;

import java.util.List;

@Component
@ConditionalOnProperty(name = "scraper.target", havingValue = "smartrecruiters")
public class SmartRecruitersThrottledRunner implements CommandLineRunner {

    private final StringRedisTemplate redisTemplate;
    private final SmartRecruitersValidatorService validatorService;

    // Hooking into your generic queue system
    private static final String QUEUE_KEY = "queue:slugs:smartrecruiters";
    private static final String SUCCESS_KEY = "verified:tokens:smartrecruiters";

    private static final int BATCH_SIZE = 250;
    private static final int CONCURRENCY = 200; // slightly lower than GH because SmartRecruiters rate-limits faster

    public SmartRecruitersThrottledRunner(StringRedisTemplate redisTemplate, 
                                          SmartRecruitersValidatorService validatorService) {
        this.redisTemplate = redisTemplate;
        this.validatorService = validatorService;
    }

    @Override
    public void run(String... args) {
        System.out.println("🚀 Starting Scaled Reactive SmartRecruiters Validator...");

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

            return validatorService.validateSlug(slug)
                .onErrorResume(e -> {
                    System.err.printf("⚠️ Network error for slug %s: %s%n", slug, e.getMessage());
                    return Mono.empty(); 
                })
                .doOnNext(isValid -> {
                    if (Boolean.TRUE.equals(isValid)) {
                        long durationMs = System.currentTimeMillis() - startTime;
                        System.out.printf("🎯 BOOM! Found SmartRecruiters instance: %s (took %.2fs)%n", 
                                slug, durationMs / 1000.0);
                        redisTemplate.opsForSet().add(SUCCESS_KEY, slug);
                    } 
                });
        }, CONCURRENCY)
        .doOnComplete(() -> System.out.println("✅ SmartRecruiters validation queue completely drained!"))
        .blockLast();
    }
}