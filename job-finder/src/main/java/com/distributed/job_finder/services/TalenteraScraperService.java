package com.distributed.job_finder.services;

import com.distributed.job_finder.config.TalenteraConfig;
import com.distributed.job_finder.dtos.JobDto;
import com.distributed.job_finder.repos.CompanyRepo;
import com.distributed.job_finder.utils.JobDataParser;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.parser.Parser;
import org.jsoup.select.Elements;
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
import java.util.UUID;

@Slf4j
@Service
public class TalenteraScraperService {

    private final WebClient webClient;
    private final ReactiveRedisTemplate<String, JobDto> reactiveRedisTemplate;
    private final TalenteraConfig config;
    private final CompanyRepo companyRepo;

    private static final String JOB_INGESTION_STREAM = "job:ingestion:stream";

    @Autowired
    public TalenteraScraperService(ReactiveRedisTemplate<String, JobDto> reactiveRedisTemplate,
                                   TalenteraConfig config,
                                   CompanyRepo companyRepo) {

        // 16MB buffer to handle massive XML feeds
        org.springframework.web.reactive.function.client.ExchangeStrategies strategies =
                org.springframework.web.reactive.function.client.ExchangeStrategies.builder()
                        .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(16 * 1024 * 1024))
                        .build();

        this.webClient = WebClient.builder()
                .exchangeStrategies(strategies)
                .build();

        this.reactiveRedisTemplate = reactiveRedisTemplate;
        this.config = config;
        this.companyRepo = companyRepo;
    }

    public Mono<Void> scrapeAllConfiguredBoards() {
        log.info("Starting scrape for {} configured Talentera boards...", config.getTargetBoards().size());

        return Flux.fromIterable(config.getTargetBoards())
                .flatMap(boardToken ->
                        // 1. Look up the company exactly like you do for Greenhouse
                        Mono.fromCallable(() -> companyRepo.findByBoardTokenIgnoreCase(boardToken)
                                .orElseThrow(() -> new RuntimeException("Company not found in DB for board token: " + boardToken)))
                                .subscribeOn(reactor.core.scheduler.Schedulers.boundedElastic())
                                // 2. Pass the DB ID to the fetcher
                                .flatMap(company -> fetchAndPushJobs(company.getId(), company.getName(), boardToken))
                                // Wait 1 second between companies so you don't get IP banned
                                .delayElement(Duration.ofSeconds(1))
                , 1) // Do this 1 board at a time to be polite to the WAFs
                .then();
    }

    private Mono<Void> fetchAndPushJobs(UUID companyId, String companyName, String boardToken) {
        String rssUrl = String.format("https://%s.talentera.com/en/rss.xml", boardToken);
        log.info("Fetching jobs from Talentera RSS: {}", rssUrl);

        return webClient.get()
                .uri(rssUrl)
                .retrieve()
                .bodyToMono(String.class)
                .subscribeOn(reactor.core.scheduler.Schedulers.boundedElastic())
                .flatMapMany(xml -> {
                    // Parse the XML string into JSoup Elements
                    Document doc = Jsoup.parse(xml, "", Parser.xmlParser());
                    Elements items = doc.select("item");
                    return Flux.fromIterable(items);
                })
                .map(item -> {
                    String jobTitle = item.select("title").text();
                    String jobDescription = item.select("description").text();

                    return new JobDto(
                            item.select("guid").text(),                 // atsJobId (RSS guid)
                            companyId,                                  // DB Company UUID!
                            companyName,                                // DB Company Name
                            jobTitle,
                            "Remote / Unspecified",                     // Location (usually hidden in RSS)
                            JobDataParser.extractDepartment(jobTitle),
                            item.select("link").text(),                 // jobUrl
                            jobDescription,
                            JobDataParser.extractExperienceLevel(jobTitle),
                            JobDataParser.extractEmploymentType(jobTitle, jobDescription),
                            null,
                            null,
                            "USD"
                    );
                })
                .delayElements(Duration.ofMillis(5)) // Protect Redis Stream
                .flatMap(jobDto -> pushToRedisStream(jobDto), 16)
                .doOnComplete(() -> log.info("Finished fetching jobs for {}", boardToken))
                .onErrorResume(error -> {
                    log.warn("Skipping board '{}' due to error: {}", boardToken, error.getMessage());
                    return Flux.empty();
                })
                .then();
    }

    private Mono<RecordId> pushToRedisStream(JobDto jobDto) {
        ObjectRecord<String, JobDto> record = StreamRecords.newRecord()
                .ofObject(jobDto)
                .withStreamKey(JOB_INGESTION_STREAM);

        return reactiveRedisTemplate.opsForStream().add(record)
                .doOnSuccess(recordId -> log.debug("Pushed Talentera ticket to Redis Stream: {}", jobDto.atsJobId()));
    }
}