package com.distributed.job_validator.greenhouse;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.SynchronousSink;

import java.util.List;

@Component
@ConditionalOnProperty(name = "scraper.target", havingValue = "greenhouse")
public class GreenhouseThrottledRunner implements CommandLineRunner {

    private final StringRedisTemplate redisTemplate;
    private final GreenhouseValidatorService greenhouseValidatorService;

    private static final String QUEUE_KEY = "queue:slugs:lever";
    private static final String SUCCESS_KEY = "verified:tokens:lever";

    private static final int BATCH_SIZE = 1000;
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
        .flatMap(Flux::fromIterable) // Removed subscribeOn here
        .flatMap(slug -> {
            long startTime = System.currentTimeMillis();
// ... rest of your code stays exactly the same

            return greenhouseValidatorService.validateSlug(slug)
                .doOnNext(isValid -> {
                    if (Boolean.TRUE.equals(isValid)) {
                        long durationMs = System.currentTimeMillis() - startTime;
                        
                        System.out.printf("🎯 BOOM! Found Greenhouse instance: %s (took %.2fs)%n", 
                                slug, (durationMs / 1000.0));
                        redisTemplate.opsForSet().add(SUCCESS_KEY, slug);
                    } 
                });
        }, CONCURRENCY)
        .doOnComplete(() -> System.out.println("✅ Greenhouse validation queue completely drained!"))
        .blockLast();
    }
}