package com.distributed.job_validator.lever;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.SynchronousSink;

@Component
@ConditionalOnProperty(name = "scraper.target", havingValue = "lever")
public class LeverThrottledRunner implements CommandLineRunner {

    private final StringRedisTemplate redisTemplate;
    private final LeverValidatorService leverValidatorService;
    
    private static final String QUEUE_KEY = "queue:slugs:lever";
    private static final String SUCCESS_KEY = "verified:tokens:lever";

    public LeverThrottledRunner(StringRedisTemplate redisTemplate, 
                                LeverValidatorService leverValidatorService) {
        this.redisTemplate = redisTemplate;
        this.leverValidatorService = leverValidatorService;
    }

    @Override
    public void run(String... args) {
        System.out.println("🚀 Starting Throttled Reactive Lever Validator...");
        
        Flux.generate((SynchronousSink<String> sink) -> {
            String slug = redisTemplate.opsForSet().pop(QUEUE_KEY);
            
            if (slug != null) {
                sink.next(slug);
            } else {
                sink.complete();
            }
        })
        .flatMap(slug -> {
            long startTime = System.currentTimeMillis();
            
            return leverValidatorService.validateSlug(slug)
                .doOnNext(isValid -> {
                    long durationMs = System.currentTimeMillis() - startTime;
                    double seconds = durationMs / 1000.0;

                    if (Boolean.TRUE.equals(isValid)) {
                        System.out.printf("🎯 BOOM! Found Lever instance: %s (took %.2fs / %dms)%n", 
                                slug, seconds, durationMs);
                        redisTemplate.opsForSet().add(SUCCESS_KEY, slug);
                    } else {
                        System.out.printf("❌ Failed: %s (took %.2fs / %dms)%n", 
                                slug, seconds, durationMs);
                    }
                });
        }, 20) // Concurrency setting (up to 20 parallel requests)
        .doOnComplete(() -> System.out.println("✅ Lever validation queue completely drained!"))
        .blockLast(); 
    }
}