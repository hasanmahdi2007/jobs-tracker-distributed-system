package com.hasan.gateway.repos;

import com.hasan.gateway.entities.ApiKey;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;
import java.util.UUID;

@Repository
public interface ApiKeyRepo extends ReactiveCrudRepository<ApiKey, UUID> {
    
    Mono<ApiKey> findByKeyHash(String keyHash);
}