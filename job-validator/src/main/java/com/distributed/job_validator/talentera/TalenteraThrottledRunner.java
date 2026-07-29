package com.distributed.job_validator.talentera;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.SynchronousSink;

import java.time.Duration;

@Component
// 👇 This ensures it only runs when scraper.target=talentera in your application.yml
@ConditionalOnProperty(name = "scraper.target", havingValue = "talentera")
public class TalenteraThrottledRunner implements CommandLineRunner {

    private final StringRedisTemplate redisTemplate;
    private final TalenteraHunter talenteraHunter;

    private static final String QUEUE_KEY = "queue:slugs:talentera";
    private static final String SUCCESS_KEY = "verified:tokens:talentera";

    public TalenteraThrottledRunner(StringRedisTemplate redisTemplate, TalenteraHunter talenteraHunter) {
        this.redisTemplate = redisTemplate;
        this.talenteraHunter = talenteraHunter;
    }

    @Override
    public void run(String... args) {
        System.out.println("🚀 Starting Throttled Reactive Talentera Validator...");

        Flux.generate((SynchronousSink<String> sink) -> {
            String slug = redisTemplate.opsForSet().pop(QUEUE_KEY);
            if (slug != null) {
                sink.next(slug);
            } else {
                sink.complete();
            }
        })
        .delayElements(Duration.ofMillis(100))
        .flatMap(slug -> 
            talenteraHunter.isTalentera(slug)
                .doOnNext(isValid -> {
                    if (Boolean.TRUE.equals(isValid)) {
                        System.out.println("🎯 BOOM! Found Talentera instance: " + slug);
                        redisTemplate.opsForSet().add(SUCCESS_KEY, slug);
                    }
                }),
            5 // Max 5 active network requests simultaneously
        )
        .doOnComplete(() -> System.out.println("✅ Talentera validation queue completely drained!"))
        .blockLast();
    }
}