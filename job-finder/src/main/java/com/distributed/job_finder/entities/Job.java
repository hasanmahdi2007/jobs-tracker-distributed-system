package com.distributed.job_finder.entities;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import com.distributed.job_finder.enums.JobStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Table("jobs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Job {

    @Id
    private UUID id;

    @Column("ats_job_id")
    private String atsJobId;

    @Column("company_id")
    private UUID companyId;

    @Column("company_name")
    private String companyName;

    private String title;
    
    private String location;
    
    private String department;

    @Column("apply_url")
    private String applyUrl;

    @Column("description_text")
    private String descriptionText;

    // R2DBC maps Enums to Postgres strings/enums automatically. 
    // Defaulting to ACTIVE replaces the need for @PrePersist.
    @Builder.Default
    private JobStatus status = JobStatus.ACTIVE;

    @Column("experience_level")
    private String experienceLevel;

    @Column("employment_type")
    private String employmentType;

    @Column("salary_currency")
    private String salaryCurrency;

    // Defaulting timestamps replaces the need for @PrePersist.
    // For updates, you will need to manually set updatedAt = LocalDateTime.now() in your service layer before saving.
    @Column("created_at")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column("updated_at")
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();
}