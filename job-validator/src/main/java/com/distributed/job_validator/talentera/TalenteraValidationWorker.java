package com.distributed.job_validator.talentera;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class TalenteraValidationWorker {

    private static final Logger log = LoggerFactory.getLogger(TalenteraValidationWorker.class);
    
    private final TalenteraHunter talenteraHunter;
    private final StringRedisTemplate redisTemplate;

    // Pointing to your deduplicated Redis Set queue
    private static final String QUEUE_KEY = "queue:slugs:talentera";
    private static final String SUCCESS_KEY = "verified:tokens:talentera";

    public TalenteraValidationWorker(TalenteraHunter talenteraHunter, StringRedisTemplate redisTemplate) {
        this.talenteraHunter = talenteraHunter;
        this.redisTemplate = redisTemplate;
    }

    // Runs constantly with a tiny 100ms pause to not melt your CPU
    @Scheduled(fixedDelay = 100)
    public void processNextSlug() {
        // 1. Pop the next unique slug from the Redis Set
        String slug = redisTemplate.opsForSet().pop(QUEUE_KEY);
        
        if (slug == null) {
            // Queue is empty, fail silently until there is work to do
            return; 
        }

        long startTime = System.currentTimeMillis();

        // 2. Check for the footprint
        boolean found = talenteraHunter.isTalentera(slug);
        
        long timeTaken = System.currentTimeMillis() - startTime;

        if (found) {
            log.info("🎯 BOOM! Found Talentera instance: {} (took {}ms)", slug, timeTaken);
            // 3. Save the success back to Redis
            redisTemplate.opsForSet().add(SUCCESS_KEY, slug);
        } else {
            log.info("Miss: {} (took {}ms)", slug, timeTaken);
        }
    }
}