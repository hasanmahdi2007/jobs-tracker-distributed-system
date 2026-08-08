package com.distributed.job_finder.repos;

import com.distributed.job_finder.entities.Job;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface JobRepo extends JpaRepository<Job, UUID> {

    Optional<Job> findByAtsJobIdAndCompanyId(String atsJobId, UUID companyId);

    // 1. RECENT SEARCH (Keyset Pagination)
    // PUTTING CAST() BACK IN: Postgres needs to know the type of the null parameter
    @Query("SELECT j FROM Job j WHERE " +
           "(:search = '' OR LOWER(j.title) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(j.companyName) LIKE LOWER(CONCAT('%', :search, '%'))) AND " +
           "(:location = '' OR LOWER(j.location) LIKE LOWER(CONCAT('%', :location, '%'))) AND " +
           "(:type = '' OR LOWER(j.employmentType) = LOWER(:type)) AND " + 
           "(:company = '' OR j.companyName = :company) AND " +
           "(:category = '' OR LOWER(j.department) = LOWER(:category)) AND " +
           "(cast(:lastCreatedAt as timestamp) IS NULL OR " +
           "  j.createdAt < :lastCreatedAt OR " +
           "  (j.createdAt = :lastCreatedAt AND j.id < :lastId)) " +
           "ORDER BY j.createdAt DESC, j.id DESC")
    List<Job> searchJobsRecent(@Param("search") String search, 
                               @Param("location") String location, 
                               @Param("type") String type,
                               @Param("company") String company,
                               @Param("category") String category,
                               @Param("lastCreatedAt") LocalDateTime lastCreatedAt,
                               @Param("lastId") UUID lastId,
                               Pageable pageable);

    // 2. RELEVANT SEARCH (Sorting by Title ASC)
    @Query("SELECT j FROM Job j WHERE " +
           "(:search = '' OR LOWER(j.title) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(j.companyName) LIKE LOWER(CONCAT('%', :search, '%'))) AND " +
           "(:location = '' OR LOWER(j.location) LIKE LOWER(CONCAT('%', :location, '%'))) AND " +
           "(:type = '' OR LOWER(j.employmentType) = LOWER(:type)) AND " + 
           "(:company = '' OR j.companyName = :company) AND " +
           "(:category = '' OR LOWER(j.department) = LOWER(:category)) AND " +
           "(cast(:lastTitle as text) IS NULL OR " +
           "  j.title > :lastTitle OR " +
           "  (j.title = :lastTitle AND j.id > :lastId)) " +
           "ORDER BY j.title ASC, j.id ASC")
    List<Job> searchJobsRelevant(@Param("search") String search, 
                                 @Param("location") String location, 
                                 @Param("type") String type,
                                 @Param("company") String company,
                                 @Param("category") String category,
                                 @Param("lastTitle") String lastTitle,
                                 @Param("lastId") UUID lastId,
                                 Pageable pageable);

    // 3. DIVERSE SEARCH (Keyset Pagination via Subquery)
    @Query(value = """
            SELECT j.* FROM jobs j
            INNER JOIN (
                SELECT id, 
                       ROW_NUMBER() OVER(PARTITION BY company_id ORDER BY created_at DESC) as rnk 
                FROM jobs
                WHERE (:search = '' OR LOWER(title) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(company_name) LIKE LOWER(CONCAT('%', :search, '%')))
                  AND (:location = '' OR LOWER(location) LIKE LOWER(CONCAT('%', :location, '%')))
                  AND (:type = '' OR LOWER(employment_type) = LOWER(:type)) 
                  AND (:company = '' OR company_name = :company)
                  AND (:category = '' OR LOWER(department) = LOWER(:category)) 
                  AND (CAST(:lastCreatedAt AS timestamp) IS NULL OR created_at < CAST(:lastCreatedAt AS timestamp) OR (created_at = CAST(:lastCreatedAt AS timestamp) AND id < CAST(:lastId AS uuid)))
            ) ranked ON j.id = ranked.id
            ORDER BY ranked.rnk ASC, j.created_at DESC
            """, 
            nativeQuery = true)
    List<Job> findDiversifiedFeed(@Param("search") String search, 
                                  @Param("location") String location, 
                                  @Param("type") String type,
                                  @Param("company") String company,
                                  @Param("category") String category,
                                  @Param("lastCreatedAt") LocalDateTime lastCreatedAt,
                                  @Param("lastId") UUID lastId,
                                  Pageable pageable);
}