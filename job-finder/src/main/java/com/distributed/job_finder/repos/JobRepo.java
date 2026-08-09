package com.distributed.job_finder.repos;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;

import com.distributed.job_finder.entities.Job;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface JobRepo extends ReactiveCrudRepository<Job, UUID>, CustomJobRepository {

    Mono<Job> findByAtsJobIdAndCompanyId(String atsJobId, UUID companyId);

    // 1. RECENT SEARCH (Keyset Pagination)
    // Converted to Native PostgreSQL
    @Query("SELECT * FROM jobs WHERE " +
           "(:search = '' OR LOWER(title) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(company_name) LIKE LOWER(CONCAT('%', :search, '%'))) AND " +
           "(:location = '' OR LOWER(location) LIKE LOWER(CONCAT('%', :location, '%'))) AND " +
           "(:type = '' OR LOWER(employment_type) = LOWER(:type)) AND " + 
           "(:company = '' OR company_name = :company) AND " +
           "(:category = '' OR LOWER(department) = LOWER(:category)) AND " +
           "(CAST(:lastCreatedAt AS timestamp) IS NULL OR " +
           "  created_at < :lastCreatedAt OR " +
           "  (created_at = :lastCreatedAt AND id < CAST(:lastId AS uuid))) " +
           "ORDER BY created_at DESC, id DESC")
    Flux<Job> searchJobsRecent(@Param("search") String search, 
                               @Param("location") String location, 
                               @Param("type") String type,
                               @Param("company") String company,
                               @Param("category") String category,
                               @Param("lastCreatedAt") LocalDateTime lastCreatedAt,
                               @Param("lastId") UUID lastId,
                               Pageable pageable);

    // 2. RELEVANT SEARCH (Sorting by Title ASC)
    // Converted to Native PostgreSQL
    @Query("SELECT * FROM jobs WHERE " +
           "(:search = '' OR LOWER(title) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(company_name) LIKE LOWER(CONCAT('%', :search, '%'))) AND " +
           "(:location = '' OR LOWER(location) LIKE LOWER(CONCAT('%', :location, '%'))) AND " +
           "(:type = '' OR LOWER(employment_type) = LOWER(:type)) AND " + 
           "(:company = '' OR company_name = :company) AND " +
           "(:category = '' OR LOWER(department) = LOWER(:category)) AND " +
           "(CAST(:lastTitle AS text) IS NULL OR " +
           "  title > :lastTitle OR " +
           "  (title = :lastTitle AND id > CAST(:lastId AS uuid))) " +
           "ORDER BY title ASC, id ASC")
    Flux<Job> searchJobsRelevant(@Param("search") String search, 
                                 @Param("location") String location, 
                                 @Param("type") String type,
                                 @Param("company") String company,
                                 @Param("category") String category,
                                 @Param("lastTitle") String lastTitle,
                                 @Param("lastId") UUID lastId,
                                 Pageable pageable);

    // 3. DIVERSE SEARCH (Keyset Pagination via Subquery)
    // nativeQuery = true is removed as it's implicit in R2DBC
    @Query("""
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
            """)
    Flux<Job> findDiversifiedFeed(@Param("search") String search, 
                                  @Param("location") String location, 
                                  @Param("type") String type,
                                  @Param("company") String company,
                                  @Param("category") String category,
                                  @Param("lastCreatedAt") LocalDateTime lastCreatedAt,
                                  @Param("lastId") UUID lastId,
                                  Pageable pageable);
}