package com.distributed.job_finder.repos;

import com.distributed.job_finder.entities.Company;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CompanyRepo extends JpaRepository<Company, UUID> {
    
    // This allows your service to find the company by its board token (like "figma" or "github")
    Optional<Company> findByBoardTokenIgnoreCase(String boardToken);
}