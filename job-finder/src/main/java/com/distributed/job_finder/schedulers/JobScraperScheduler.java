package com.distributed.job_finder.schedulers;

import com.distributed.job_finder.services.GreenhouseScraperService;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class JobScraperScheduler {

    private final GreenhouseScraperService scraperService;

    public JobScraperScheduler(GreenhouseScraperService scraperService) {
        this.scraperService = scraperService;
    }

    /**
     * TEST TRIGGER: Runs exactly once when the application boots up.
     * (You can delete this method once you confirm data is in PostgreSQL).
     */
    /*@PostConstruct
    public void runOnStartup() {
        log.info("[TEST] Application booted. Firing initial Greenhouse scrape...");
        scraperService.scrapeAllConfiguredBoards().subscribe();
    }*/

    /**
     * PRODUCTION TRIGGER: Runs once every day at exactly midnight (00:00:00).
     * The cron expression format is: Second Minute Hour Day Month Weekday
     */
    @Scheduled(cron = "0 0 3 * * *")
    public void runDailyScrape() {
        log.info("[DAILY CRON] Firing scheduled daily Greenhouse scrape...");
        scraperService.scrapeAllConfiguredBoards().subscribe();
    }
}