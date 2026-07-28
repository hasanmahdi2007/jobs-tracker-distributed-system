package com.distributed.job_validator.validators;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

@Component
public class WorkableValidationWorker {

    private final StringRedisTemplate redisTemplate;
    private final RestTemplate restTemplate = new RestTemplate();

    public WorkableValidationWorker(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Scheduled(fixedDelay = 100)
    public void processNextSlug() {
        String slug = redisTemplate.opsForList().rightPop("workable:validation:queue");

        if (slug == null) {
            return;
        }

        String endpoint = "https://apply.workable.com/api/v3/accounts/" + slug + "/jobs";

        try {
            System.out.print("🔍 Testing slug: " + slug + " ... ");
            
            restTemplate.getForObject(endpoint, String.class);
            
            System.out.println("✅ SUCCESS!");
            redisTemplate.opsForList().leftPush("verified:tokens", slug);

        } catch (HttpClientErrorException.NotFound e) {
            System.out.println("❌ Miss");
        } catch (Exception e) {
            System.out.println("⚠️ Network Error: " + e.getMessage());
        }
    }
}