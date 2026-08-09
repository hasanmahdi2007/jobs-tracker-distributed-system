package com.hasan.gateway.repos;

import com.hasan.gateway.entities.Client;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;
import java.util.UUID;

@Repository
public interface ClientRepo extends ReactiveCrudRepository<Client, UUID> {
    
    Mono<Client> findByEmail(String email);
}