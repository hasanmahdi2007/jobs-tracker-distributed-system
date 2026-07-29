package com.jobfinder.validator.runner;

import com.jobfinder.validator.service.WorkableValidatorService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.SynchronousSink;

import java.time.Duration;

@Component
@ConditionalOnProperty(name = "scraper.target", havingValue = "workable")
public class WorkableThrottledRunner implements CommandLineRunner {

    private final StringRedisTemplate redisTemplate;
    private final WorkableValidatorService workableValidatorService;

    public WorkableThrottledRunner(StringRedisTemplate redisTemplate, 
                                   WorkableValidatorService workableValidatorService) {
        this.redisTemplate = redisTemplate;
        this.workableValidatorService = workableValidatorService;
    }

    @Override
    public void run(String... args) {
        System.out.println("🚀 Starting Throttled Reactive Workable Validator...");
        
        Flux.generate((SynchronousSink<String> sink) -> {
            String slug = redisTemplate.opsForSet().pop("queue:slugs:workable");
            
            if (slug != null) {
                sink.next(slug);
            } else {
                sink.complete();
            }
        })
        .delayElements(Duration.ofMillis(100)) // Max 10 requests per second
        .flatMap(slug -> 
            workableValidatorService.validateSlug(slug)
                .doOnNext(isValid -> {
                    if (Boolean.TRUE.equals(isValid)) {
                        System.out.println("🔥 FOUND WORKABLE PORTAL: " + slug);
                        redisTemplate.opsForSet().add("valid:workable", slug);
                    }
                }), 
            5 // Max 5 active network calls in parallel
        )
        .doOnComplete(() -> System.out.println("✅ Workable validation queue completely drained!"))
        .blockLast(); 
    }
}