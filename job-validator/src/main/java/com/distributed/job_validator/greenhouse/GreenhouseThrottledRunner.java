package com.distributed.job_validator.greenhouse;

import java.util.concurrent.ThreadLocalRandom;
import org.springframework.boot.CommandLineRunner;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.SynchronousSink;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import java.time.Duration;

@Component
@ConditionalOnProperty(name = "scraper.target", havingValue = "greenhouse")
public class GreenhouseThrottledRunner implements CommandLineRunner {

    private final StringRedisTemplate redisTemplate;
    private final GreenhouseValidatorService greenhouseValidatorService;
    private static final String SUCCESS_KEY = "verified:tokens:greenhouse";

    public GreenhouseThrottledRunner(StringRedisTemplate redisTemplate, 
                                     GreenhouseValidatorService greenhouseValidatorService) {
        this.redisTemplate = redisTemplate;
        this.greenhouseValidatorService = greenhouseValidatorService;
    }

    @Override
    public void run(String... args) {
        System.out.println("🚀 Starting Throttled Reactive Greenhouse Validator...");
        
        Flux.generate((SynchronousSink<String> sink) -> {
            String slug = redisTemplate.opsForSet().pop("queue:slugs:greenhouse");
            
            if (slug != null) {
                sink.next(slug);
            } else {
                sink.complete();
            }
        })
        // Greenhouse is more forgiving, so we can go a bit faster (300ms to 800ms jitter)
        .flatMap(slug -> {
            long startTime = System.currentTimeMillis();
            
            return greenhouseValidatorService.validateSlug(slug)
                .doOnNext(isValid -> {
                    long durationMs = System.currentTimeMillis() - startTime;
                    double seconds = durationMs / 1000.0;

                    if (Boolean.TRUE.equals(isValid)) {
                        System.out.printf("🎯 BOOM! Found Greenhouse instance: %s (took %.2fs / %dms)%n", 
                                slug, seconds, durationMs);
                        redisTemplate.opsForSet().add(SUCCESS_KEY, slug);
                    } else {
                        System.out.printf("❌ Failed: %s (took %.2fs / %dms)%n", 
                                slug, seconds, durationMs);
                    }
                });
        }, 20) // Concurrency setting
        .doOnComplete(() -> System.out.println("✅ Greenhouse validation queue completely drained!"))
        .blockLast(); 
    }
}