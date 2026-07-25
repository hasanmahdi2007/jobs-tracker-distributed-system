package com.distributed.job_finder.config;

import com.distributed.job_finder.dtos.JobDto;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {

    @Bean
    public ReactiveRedisTemplate<String, JobDto> reactiveRedisTemplate(ReactiveRedisConnectionFactory factory) {
        Jackson2JsonRedisSerializer<JobDto> serializer = new Jackson2JsonRedisSerializer<>(JobDto.class);

        RedisSerializationContext.RedisSerializationContextBuilder<String, JobDto> builder =
                RedisSerializationContext.newSerializationContext(new StringRedisSerializer());

        RedisSerializationContext<String, JobDto> context = builder.value(serializer).build();

        return new ReactiveRedisTemplate<>(factory, context);
    }
}