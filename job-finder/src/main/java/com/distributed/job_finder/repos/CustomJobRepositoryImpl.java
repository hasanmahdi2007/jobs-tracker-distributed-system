package com.distributed.job_finder.repos;

import java.util.UUID;

import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;

import com.distributed.job_finder.dtos.JobDto;
import com.distributed.job_finder.enums.JobSort;

import io.r2dbc.spi.Row;
import io.r2dbc.spi.RowMetadata;
import reactor.core.publisher.Flux;

@Repository
public class CustomJobRepositoryImpl implements CustomJobRepository {

    private final DatabaseClient databaseClient;

    public CustomJobRepositoryImpl(DatabaseClient databaseClient) {
        this.databaseClient = databaseClient;
    }

    @Override
    public Flux<JobDto> findJobsDynamically(
            String search, String location, String type, String company, 
            String category, JobSort sort, 
            int page, int limit 
    ) {
        boolean isDiverse = (sort == JobSort.DIVERSE);
        StringBuilder sql = new StringBuilder();

        if (isDiverse) {
            sql.append("WITH fast_feed AS (SELECT * FROM jobs WHERE 1=1");
        } else {
            sql.append("SELECT * FROM jobs WHERE 1=1");
        }

        if (location != null) sql.append(" AND location ILIKE :location");
        if (company != null) sql.append(" AND company_name ILIKE :company"); 
        if (type != null) sql.append(" AND employment_type ILIKE :type");
        if (category != null) sql.append(" AND department ILIKE :category"); 
        if (search != null) sql.append(" AND search_vector @@ plainto_tsquery('english', :search)");

        if (isDiverse) {
            sql.append(" ORDER BY updated_at DESC, id ASC LIMIT 1000) ");
            sql.append("SELECT * FROM fast_feed ");
            sql.append("ORDER BY ROW_NUMBER() OVER (PARTITION BY company_id ORDER BY updated_at DESC), updated_at DESC");
        } else {
            sql.append(getSortSql(sort));
        }

        sql.append(" LIMIT :limit OFFSET :offset");

        DatabaseClient.GenericExecuteSpec spec = databaseClient.sql(sql.toString());

        if (location != null) spec = spec.bind("location", "%" + location + "%");
        if (company != null) spec = spec.bind("company", "%" + company + "%");
        if (type != null) spec = spec.bind("type", "%" + type + "%");
        if (category != null) spec = spec.bind("category", "%" + category + "%");
        if (search != null) spec = spec.bind("search", search);
        
        int offset = page * limit;
        spec = spec.bind("limit", limit);
        spec = spec.bind("offset", offset);

        return spec.map((Row row, RowMetadata rowMetadata) -> new JobDto(
                row.get("ats_job_id", String.class),       
                row.get("company_id", UUID.class),
                row.get("company_name", String.class),
                row.get("title", String.class),
                row.get("location", String.class),
                row.get("department", String.class),
                row.get("apply_url", String.class),        
                row.get("description_text", String.class), 
                row.get("experience_level", String.class),
                row.get("employment_type", String.class),
                row.get("salary_currency", String.class)
        )).all();
    }

    private String getSortSql(JobSort sort) {
        return switch (sort) {
            case RECENT -> " ORDER BY updated_at DESC, id ASC";
            default -> " ORDER BY updated_at DESC, id ASC";
        };
    }
}