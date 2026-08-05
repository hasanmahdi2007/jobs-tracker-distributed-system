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

    // 1. STANDARD SEARCH (Used for sorting by Salary, Relevance, etc.)
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

    // 2. DIVERSE SEARCH (Interleaved feed that ALSO respects your filters!)
    // The Interleaved Feed (Now perfectly mapped to your PostgreSQL schema)
    @Query(value = """
            SELECT j.* FROM jobs j
            INNER JOIN (
                SELECT id, 
                       ROW_NUMBER() OVER(PARTITION BY company_id ORDER BY created_at DESC) as rnk 
                FROM jobs
                WHERE (:search = '' OR LOWER(title) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(company_name) LIKE LOWER(CONCAT('%', :search, '%')))
                  AND (:location = '' OR LOWER(location) LIKE LOWER(CONCAT('%', :location, '%')))
                  AND (:type = '' OR employment_type = :type)
                  AND (:company = '' OR company_name = :company)
                  AND (:category = '' OR department = :category)
            ) ranked ON j.id = ranked.id
            ORDER BY ranked.rnk ASC, j.created_at DESC
            """, 
            countQuery = """
            SELECT COUNT(*) FROM jobs 
            WHERE (:search = '' OR LOWER(title) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(company_name) LIKE LOWER(CONCAT('%', :search, '%')))
              AND (:location = '' OR LOWER(location) LIKE LOWER(CONCAT('%', :location, '%')))
              AND (:type = '' OR employment_type = :type)
              AND (:company = '' OR company_name = :company)
              AND (:category = '' OR department = :category)
            """, 
            nativeQuery = true)
    Page<Job> findDiversifiedFeed(@Param("search") String search, 
                                  @Param("location") String location, 
                                  @Param("type") String type,
                                  @Param("company") String company,
                                  @Param("category") String category,
                                  Pageable pageable);
}