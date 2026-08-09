package com.distributed.job_finder.services;

import com.distributed.job_finder.config.SmartRecruitersConfig;
import com.distributed.job_finder.dtos.JobDto;
import com.distributed.job_finder.dtos.smartrecruiters.SmartRecruitersJobDetails;
import com.distributed.job_finder.dtos.smartrecruiters.SmartRecruitersPageResponse;
import com.distributed.job_finder.dtos.smartrecruiters.SmartRecruitersPosting;
import com.distributed.job_finder.repos.CompanyRepo;
import com.distributed.job_finder.utils.JobDataParser;
import com.distributed.job_finder.utils.LocationNormalizer; // <-- IMPORT
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.stream.ObjectRecord;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.connection.stream.StreamRecords;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class SmartRecruitersScraperService {

    private final WebClient webClient;
    private final ReactiveRedisTemplate<String, JobDto> reactiveRedisTemplate;
    private final CompanyRepo companyRepo;
    private final SmartRecruitersConfig config; 
    private final LocationNormalizer locationNormalizer; // <-- ADDED

    private static final String JOB_INGESTION_STREAM = "job:ingestion:stream";

    @Autowired
    public SmartRecruitersScraperService(ReactiveRedisTemplate<String, JobDto> reactiveRedisTemplate,
                                         CompanyRepo companyRepo,
                                         SmartRecruitersConfig config,
                                         LocationNormalizer locationNormalizer) {
        this.config = config;
        this.locationNormalizer = locationNormalizer;

        org.springframework.web.reactive.function.client.ExchangeStrategies strategies =
                org.springframework.web.reactive.function.client.ExchangeStrategies.builder()
                        .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(16 * 1024 * 1024))
                        .build();

        this.webClient = WebClient.builder()
                .baseUrl(config.getBaseUrl()) 
                .exchangeStrategies(strategies)
                .build();

        this.reactiveRedisTemplate = reactiveRedisTemplate;
        this.companyRepo = companyRepo;
    }

    public Mono<Void> scrapeAllConfiguredBoards() {
        List<String> targetBoards = config.getTargetBoards();
        
        if (targetBoards == null || targetBoards.isEmpty()) {
            log.warn("No SmartRecruiters boards configured in application.yml!");
            return Mono.empty();
        }

        log.info("Starting scrape for {} SmartRecruiters boards...", targetBoards.size());

        return Flux.fromIterable(targetBoards)
                .flatMap(boardToken -> 
                    // 1. Call the repo directly (it returns Mono<Company>)
                    companyRepo.findByBoardTokenIgnoreCase(boardToken)
                        // 2. Handle the "Not Found" case reactively
                        .switchIfEmpty(Mono.error(new RuntimeException("Company not found in DB for board token: " + boardToken)))
                        // 3. Chain to the fetch method
                        .flatMap(company -> fetchAndPushJobs(company.getId(), company.getName(), boardToken))
                , 3)
                .then();
    }

    private Mono<Void> fetchAndPushJobs(UUID companyId, String companyName, String boardToken) {
        log.info("Fetching jobs from SmartRecruiters for board: {}", boardToken);

        return fetchAllPages(boardToken)
                .delayElements(Duration.ofMillis(25)) 
                .flatMap(posting -> enrichWithDetailsAndMap(boardToken, companyId, companyName, posting), 4)
                .flatMap(this::pushToRedisStream, 16) 
                .doOnComplete(() -> log.info("Finished fetching jobs for {}", boardToken))
                .onErrorResume(error -> {
                    log.warn("Skipping board '{}' due to error: {}", boardToken, error.getMessage());
                    return Flux.empty();
                })
                .then();
    }

    private Flux<SmartRecruitersPosting> fetchAllPages(String boardToken) {
        return fetchPage(boardToken, 0)
                .expand(page -> {
                    if (page.offset() + page.limit() < page.totalFound()) {
                        return fetchPage(boardToken, page.offset() + page.limit()); 
                    }
                    return Mono.empty(); 
                })
                .flatMapIterable(page -> page.content() != null ? page.content() : List.of());
    }

    private Mono<SmartRecruitersPageResponse> fetchPage(String boardToken, int offset) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/{company}/postings")
                        .queryParam("limit", 100)
                        .queryParam("offset", offset)
                        .build(boardToken))
                .retrieve()
                .bodyToMono(SmartRecruitersPageResponse.class);
    }

    private Mono<JobDto> enrichWithDetailsAndMap(String boardToken, UUID companyId, String companyName, SmartRecruitersPosting posting) {
        return webClient.get()
                .uri("/{company}/postings/{id}", boardToken, posting.id())
                .retrieve()
                .bodyToMono(SmartRecruitersJobDetails.class)
                .map(details -> buildJobDto(companyId, companyName, posting, details))
                .onErrorResume(e -> {
                    log.warn("Failed to fetch full description for job {}, falling back to summary.", posting.id());
                    return Mono.just(buildJobDto(companyId, companyName, posting, null));
                });
    }

    private JobDto buildJobDto(UUID companyId, String companyName, SmartRecruitersPosting posting, SmartRecruitersJobDetails details) {
        String description = extractDescription(details);

        String department = posting.department() != null ? posting.department().label() : JobDataParser.extractDepartment(posting.name());
        String experience = posting.experienceLevel() != null ? posting.experienceLevel().label() : JobDataParser.extractExperienceLevel(posting.name());
        String employment = posting.typeOfEmployment() != null ? posting.typeOfEmployment().label() : JobDataParser.extractEmploymentType(posting.name(), description);
        
        // <-- NORMALIZER APPLIED HERE
        String rawLocation = formatLocation(posting.location());
        String normalizedLocation = locationNormalizer.normalizeLocationForDatabase(rawLocation);

        String url = String.format("https://jobs.smartrecruiters.com/%s/%s", posting.company().identifier(), posting.id());

        return new JobDto(
                posting.id(),
                companyId,
                companyName,
                posting.name(),
                normalizedLocation,
                department,
                url,
                description,
                experience,
                employment,
                "USD"
        );
    }

    private String extractDescription(SmartRecruitersJobDetails details) {
        if (details == null || details.jobAd() == null || details.jobAd().sections() == null) return "";
        var sections = details.jobAd().sections();
        StringBuilder sb = new StringBuilder();
        if (sections.jobDescription() != null && sections.jobDescription().text() != null) {
            sb.append(sections.jobDescription().text()).append("\n\n");
        }
        if (sections.qualifications() != null && sections.qualifications().text() != null) {
            sb.append(sections.qualifications().text()).append("\n\n");
        }
        return sb.toString().trim();
    }

    private String formatLocation(SmartRecruitersPosting.SmartRecruitersLocation loc) {
        if (loc == null) return "Remote / Unspecified";
        StringBuilder res = new StringBuilder();
        if (Boolean.TRUE.equals(loc.remote())) res.append("Remote ");
        if (loc.city() != null && !loc.city().isBlank()) res.append(res.isEmpty() ? "" : "- ").append(loc.city());
        if (loc.country() != null && !loc.country().isBlank()) res.append(res.isEmpty() ? "" : ", ").append(loc.country().toUpperCase());
        return res.isEmpty() ? "Remote / Unspecified" : res.toString().trim();
    }

    private Mono<RecordId> pushToRedisStream(JobDto jobDto) {
        ObjectRecord<String, JobDto> record = StreamRecords.newRecord()
                .ofObject(jobDto)
                .withStreamKey(JOB_INGESTION_STREAM);

        return reactiveRedisTemplate.opsForStream().add(record)
                .doOnSuccess(recordId -> log.debug("Pushed SmartRecruiters job to Stream: {}", jobDto.atsJobId()));
    }
}