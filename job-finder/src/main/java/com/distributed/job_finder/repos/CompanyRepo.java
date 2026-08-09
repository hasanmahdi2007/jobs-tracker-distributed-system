package com.distributed.job_finder.repos;

import com.distributed.job_finder.entities.Company;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Repository
public interface CompanyRepo extends ReactiveCrudRepository<Company, UUID> {
    
    // Returns Mono instead of Optional
    Mono<Company> findByBoardTokenIgnoreCase(String boardToken);
}