package com.distributed.job_finder.schedulers;

import com.distributed.job_finder.services.GreenhouseScraperService;
import com.distributed.job_finder.services.TalenteraScraperService;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Slf4j
@Component
public class JobScraperScheduler {

    private final GreenhouseScraperService greenhouseScraperService;
    private final TalenteraScraperService talenteraScraperService;

    public JobScraperScheduler(GreenhouseScraperService greenhouseScraperService,
                               TalenteraScraperService talenteraScraperService) {
        this.greenhouseScraperService = greenhouseScraperService;
        this.talenteraScraperService = talenteraScraperService;
    }

    /**
     * TEST TRIGGER: Runs exactly once when the application boots up.
     * (Uncomment during testing if you want to verify both scrapers fire).
     */
    /*@PostConstruct
    public void runOnStartup() {
        log.info("[TEST] Application booted. Firing initial scrape for all platforms...");
        runAllScrapers().subscribe();
    }*/

    /**
     * PRODUCTION TRIGGER: Runs once every day at 03:00 AM.
     * Cron format: Second Minute Hour Day Month Weekday
     */
    @Scheduled(cron = "0 0 3 * * *")
    public void runDailyScrape() {
        log.info("[DAILY CRON] Firing scheduled daily scrape for all platforms...");
        runAllScrapers().subscribe();
    }

    /**
     * Combines all scraper pipelines into a single non-blocking reactive workflow.
     */
    private Mono<Void> runAllScrapers() {
        return Mono.when(
                greenhouseScraperService.scrapeAllConfiguredBoards()
                        .doOnError(e -> log.error("Error during Greenhouse scrape: {}", e.getMessage())),
                talenteraScraperService.scrapeAllConfiguredBoards()
                        .doOnError(e -> log.error("Error during Talentera scrape: {}", e.getMessage()))
        );
    }
}