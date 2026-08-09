package com.system.analytics_engine.services;

import com.system.analytics_engine.entities.ApiRequestLog;
import com.system.analytics_engine.repos.ApiRequestLogRepo;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.stream.StreamReceiver;
import org.springframework.stereotype.Service;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@Service
public class TelemetryStreamConsumer {

    private static final Logger log = LoggerFactory.getLogger(TelemetryStreamConsumer.class);

    private final ReactiveRedisConnectionFactory redisConnectionFactory;
    private final ApiRequestLogRepo repository;

    public TelemetryStreamConsumer(ReactiveRedisConnectionFactory redisConnectionFactory, ApiRequestLogRepo repository) {
        this.redisConnectionFactory = redisConnectionFactory;
        this.repository = repository;
    }

    @PostConstruct
    public void setup() {
        redisConnectionFactory.getReactiveConnection()
            .streamCommands()
            .xGroupCreate(
                java.nio.ByteBuffer.wrap("telemetry:stream".getBytes()), 
                "analytics-group", 
                ReadOffset.from("0"), 
                true
            )
            .onErrorResume(e -> {
                log.info("Consumer group 'analytics-group' already exists. Safe to proceed.");
                return reactor.core.publisher.Mono.empty();
            })
            .doOnSuccess(success -> startListening())
            .subscribe();
    }

    private void startListening() {
        StreamReceiver.StreamReceiverOptions<String, MapRecord<String, String, String>> options = 
                StreamReceiver.StreamReceiverOptions.builder()
                .pollTimeout(Duration.ofMillis(100))
                .build();

        StreamReceiver<String, MapRecord<String, String, String>> receiver = 
                StreamReceiver.create(redisConnectionFactory, options);

        log.info("Analytics Engine started listening to telemetry:stream for batches (Manual ACK enabled)...");

        receiver.receive(
                Consumer.from("analytics-group", "engine-instance-1"), 
                StreamOffset.create("telemetry:stream", ReadOffset.lastConsumed())
        )
        .bufferTimeout(50, Duration.ofSeconds(3))
        .publishOn(Schedulers.boundedElastic())
        .doOnNext(this::processAndAckBatch)
        .subscribe();
    }

    // ==========================================
    // REACTIVE TRANSACTIONAL PROCESSING
    // ==========================================

    private void processAndAckBatch(List<MapRecord<String, String, String>> batch) {
        if (batch == null || batch.isEmpty()) {
            return;
        }

        List<ApiRequestLog> entities = batch.stream()
                .map(this::convertRecordToEntity)
                .toList();

        // Reactive pipeline: Save to Postgres -> Collect -> Acknowledge in Redis
        repository.saveAll(entities)
                .collectList()
                .flatMap(saved -> {
                    log.info("Successfully bulk inserted {} records into PostgreSQL.", saved.size());

                    String[] recordIds = batch.stream()
                            .map(record -> record.getId().getValue())
                            .toArray(String[]::new);

                    return redisConnectionFactory.getReactiveConnection()
                            .streamCommands()
                            .xAck(java.nio.ByteBuffer.wrap("telemetry:stream".getBytes()), "analytics-group", recordIds);
                })
                .doOnError(e -> log.error("Database save failed! Logs remain in Redis PEL. Error: {}", e.getMessage()))
                .subscribe();
    }

    // ==========================================
    // HELPER METHODS
    // ==========================================

    private ApiRequestLog convertRecordToEntity(MapRecord<String, String, String> record) {
        Map<String, String> map = record.getValue();
        ApiRequestLog logEntry = new ApiRequestLog();
        
        logEntry.setCorrelationId(map.get("correlationId"));
        logEntry.setMethod(map.get("method"));
        logEntry.setPath(map.get("path"));
        logEntry.setStatus(Integer.parseInt(map.getOrDefault("status", "0")));
        logEntry.setLatencyMs(Long.parseLong(map.getOrDefault("latencyMs", "0")));
        logEntry.setIp(map.get("ip"));
        logEntry.setUserAgent(map.get("userAgent"));
        logEntry.setReqBytes(Long.parseLong(map.getOrDefault("reqBytes", "0")));
        logEntry.setResBytes(Long.parseLong(map.getOrDefault("resBytes", "0")));
        logEntry.setApiKey(map.get("apiKey"));
        
        return logEntry;
    }
}