package com.distributed.job_finder.schedulers;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.distributed.job_finder.services.BambooHRScraperService;
import com.distributed.job_finder.services.GreenhouseScraperService;
import com.distributed.job_finder.services.LeverScraperService;
import com.distributed.job_finder.services.SmartRecruitersScraperService;
import com.distributed.job_finder.services.TalenteraScraperService;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Slf4j
@Component
public class JobScraperScheduler {

    private final GreenhouseScraperService greenhouseScraperService;
    private final TalenteraScraperService talenteraScraperService;
    private final LeverScraperService leverScraperService;
    private final SmartRecruitersScraperService smartRecruitersScraperService;
    private final BambooHRScraperService bambooHRScraperService;

    public JobScraperScheduler(GreenhouseScraperService greenhouseScraperService,
                               TalenteraScraperService talenteraScraperService,
                               LeverScraperService leverScraperService,
                               SmartRecruitersScraperService smartRecruitersScraperService,
                               BambooHRScraperService bambooHRScraperService) {
        this.greenhouseScraperService = greenhouseScraperService;
        this.talenteraScraperService = talenteraScraperService;
        this.leverScraperService = leverScraperService;
        this.smartRecruitersScraperService = smartRecruitersScraperService;
        this.bambooHRScraperService = bambooHRScraperService; 
    }

    // Runs once right after the app starts so we don't have to wait until 3 AM to test it
//     @PostConstruct
//     public void runOnStartup() {
//         log.info("[TEST] App booted. Firing initial scrape for ALL platforms...");
//         runAllScrapers().subscribe();
//     }

    // Runs once right after the app starts specifically to test SmartRecruiters
//     @PostConstruct
//     public void runSmartRecruitersOnStartup() {
//         log.info("[TEST] App booted. Firing initial scrape for SmartRecruiters ONLY...");
//         smartRecruitersScraperService.scrapeAllConfiguredBoards()
//                 .doOnError(e -> log.error("Error during startup SmartRecruiters scrape: {}", e.getMessage()))
//                 .subscribe();
//     }
    
    // Runs every day at 3:00 AM in production
    @Scheduled(cron = "0 0 3 * * *")
    public void runDailyScrape() {
        log.info("[DAILY CRON] Firing scheduled daily scrape for all platforms...");
        runAllScrapers().subscribe();
    }

    // Glues all our scrapers together into one non-blocking reactive flow
    private Mono<Void> runAllScrapers() {
        return Mono.when(
                greenhouseScraperService.scrapeAllConfiguredBoards()
                        .doOnError(e -> log.error("Error during Greenhouse scrape: {}", e.getMessage())),
                talenteraScraperService.scrapeAllConfiguredBoards()
                        .doOnError(e -> log.error("Error during Talentera scrape: {}", e.getMessage())),
                leverScraperService.scrapeAllConfiguredBoards()
                        .doOnError(e -> log.error("Error during Lever scrape: {}", e.getMessage())),
                smartRecruitersScraperService.scrapeAllConfiguredBoards()
                        .doOnError(e -> log.error("Error during SmartRecruiters scrape: {}", e.getMessage()))/*,
                bambooHRScraperService.scrapeAllConfiguredBoards() // <-- Added
                        .doOnError(e -> log.error("Error during BambooHR scrape: {}", e.getMessage()))*/
        );
    }
}