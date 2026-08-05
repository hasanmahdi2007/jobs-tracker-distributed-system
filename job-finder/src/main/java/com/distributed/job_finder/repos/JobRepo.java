package com.distributed.job_finder.repos;

import com.distributed.job_finder.entities.Job;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface JobRepo extends JpaRepository<Job, UUID> {

    Optional<Job> findByAtsJobIdAndCompanyId(String atsJobId, UUID companyId);

    // Search jobs by title/company, location, employment type, exact company, and department (category)
    @Query("SELECT j FROM Job j WHERE " +
           "(:search = '' OR LOWER(j.title) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(j.companyName) LIKE LOWER(CONCAT('%', :search, '%'))) AND " +
           "(:location = '' OR LOWER(j.location) LIKE LOWER(CONCAT('%', :location, '%'))) AND " +
           "(:type = '' OR j.employmentType = :type) AND " +
           "(:company = '' OR j.companyName = :company) AND " +
           "(:category = '' OR j.department = :category)")
    Page<Job> searchJobs(@Param("search") String search, 
                         @Param("location") String location, 
                         @Param("type") String type,
                         @Param("company") String company,
                         @Param("category") String category,
                         Pageable pageable);

    // The Interleaved Feed: Pulls top jobs from distinct companies using PostgreSQL Window Functions
    // The countQuery is strictly required for Spring to return a Page<Job> from a native query
    @Query(value = """
            SELECT * FROM (
                SELECT j.*, 
                       ROW_NUMBER() OVER(PARTITION BY j.company_id ORDER BY j.posted_at DESC) as company_job_rank 
                FROM jobs j
            ) ranked_jobs 
            ORDER BY company_job_rank ASC, posted_at DESC
            """, 
            countQuery = "SELECT COUNT(*) FROM jobs", 
            nativeQuery = true)
    Page<Job> findDiversifiedFeed(Pageable pageable);
}