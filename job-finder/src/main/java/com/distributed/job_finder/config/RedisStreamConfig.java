package com.distributed.job_finder.config;

import com.distributed.job_finder.dtos.JobDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.connection.stream.ObjectRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.stream.StreamReceiver;
import java.nio.ByteBuffer;
import java.time.Duration;

@Slf4j
@Configuration
public class RedisStreamConfig {

    private static final String STREAM_KEY = "job:ingestion:stream";
    private static final String CONSUMER_GROUP = "job-workers-group";

    @Bean
    public StreamReceiver<String, ObjectRecord<String, JobDto>> jobStreamReceiver(ReactiveRedisConnectionFactory factory) {
        
        // Ensure the Consumer Group exists before we try to read from it
        try {
            factory.getReactiveConnection().streamCommands()
       .xGroupCreate(ByteBuffer.wrap(STREAM_KEY.getBytes()), CONSUMER_GROUP, ReadOffset.from("0-0"), true)
       .block();
        } catch (Exception e) {
            log.debug("Consumer group {} already exists", CONSUMER_GROUP);
        }

        StreamReceiver.StreamReceiverOptions<String, ObjectRecord<String, JobDto>> options =
                StreamReceiver.StreamReceiverOptions.builder()
                        .pollTimeout(Duration.ofMillis(100))
                        .targetType(JobDto.class)
                        .build();

        return StreamReceiver.create(factory, options);
    }
}