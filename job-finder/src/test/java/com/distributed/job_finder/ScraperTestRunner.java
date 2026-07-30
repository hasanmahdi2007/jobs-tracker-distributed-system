package com.distributed.job_finder;

import com.distributed.job_finder.dtos.JobRecord;
import com.distributed.job_finder.services.DomainResolverService;
import com.distributed.job_finder.services.UniversalJobScraperService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.time.Duration;

@Component
public class ScraperTestRunner implements CommandLineRunner {

    private final DomainResolverService domainResolver;
    private final UniversalJobScraperService scraperService;

    public ScraperTestRunner(DomainResolverService domainResolver, UniversalJobScraperService scraperService) {
        this.domainResolver = domainResolver;
        this.scraperService = scraperService;
    }

    @Override
    public void run(String... args) {
        System.out.println("🚀 Starting 4-Company Scraper Test...");

        String[] testCompanies = {"Careem", "Anghami", "Property Finder", "Chalhoub Group"};

        Flux.fromArray(testCompanies)
                // Rate limit: 1 call every 20 seconds to respect Clearout 3 RPM
                .delayElements(Duration.ofSeconds(20))
                .flatMap(companyName -> {
                    System.out.println("🔍 Testing: " + companyName);

                    return domainResolver.getDomainForCompany(companyName)
                            // 1. Filter out empty/null domains
                            .filter(domain -> domain != null && !domain.isBlank())
                            
                            // 2. Transform directly to the Job Flux (Skipping the intermediate Mono fallback)
                            .flatMapMany(domain -> {
                                System.out.println("✅ Found domain: " + domain + " -> Scanning for jobs...");
                                return scraperService.scrapeJobsFromDomain(domain)
                                        .map(JobRecord::fromJsonLd);
                            })
                            
                            // 3. Catch empty streams safely at the very end. 
                            // (Triggers if domain was empty OR if the domain had 0 jobs)
                            .switchIfEmpty(Flux.defer(() -> {
                                System.out.println("⚠️ No domain or no jobs found for: " + companyName);
                                return Flux.empty();
                            }));
                })
                .doOnNext(job -> {
                    System.out.println("🎯 SCRAPED JOB: " + job.getTitle() + " at " + job.getCompany() + " (" + job.getLocationCountry() + ")");
                })
                .doOnComplete(() -> System.out.println("✅ Test Finished!"))
                .doOnError(e -> System.err.println("❌ Stream error: " + e.getMessage()))
                .blockLast(); 
    }