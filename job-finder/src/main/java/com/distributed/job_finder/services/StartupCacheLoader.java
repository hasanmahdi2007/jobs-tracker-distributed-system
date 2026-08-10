package com.distributed.job_finder.services;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

// 1. Added the missing import for your enum
import com.distributed.job_finder.enums.JobSort;

@Component
public class StartupCacheLoader {

    private final JobService jobService;

    public StartupCacheLoader(JobService jobService) {
        this.jobService = jobService;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void loadHotFeedsIntoRam() {
        System.out.println("🚀 [Startup] Application Ready. Pre-loading Hot Feeds into RAM...");

        // 2 & 3. Changed to getJobs() and added 0 (page) and 250 (size)
        Mono<Void> loadDiverse = jobService.getJobs(
                null, null, null, null, null, JobSort.DIVERSE, 0, 250)
                .then() // Ignore the return data, we just want it to trigger the Redis save
                .doOnSuccess(v -> System.out.println("✅ DIVERSE feed locked in RAM."));

        Mono<Void> loadRecent = jobService.getJobs(
                null, null, null, null, null, JobSort.RECENT, 0, 250)
                .then()
                .doOnSuccess(v -> System.out.println("✅ RECENT feed locked in RAM."));

        // Execute both immediately
        Mono.when(loadDiverse, loadRecent).subscribe(
                success -> System.out.println("🔥 [Startup] All caches warmed. Ready for traffic!"),
                error -> System.err.println("❌ [Startup] Cache load failed: " + error.getMessage())
        );
    }
}