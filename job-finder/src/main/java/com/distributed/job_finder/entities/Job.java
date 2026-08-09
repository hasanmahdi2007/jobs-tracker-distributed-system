package com.distributed.job_finder.entities;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

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

    @Column("experience_level")
    private String experienceLevel;

    @Column("employment_type")
    private String employmentType;

    @Column("salary_currency")
    private String salaryCurrency;

    // Defaulting timestamps replaces the need for @PrePersist.
    @Column("last_seen_at")
    private LocalDateTime lastSeenAt;

    @Column("updated_at")
    private LocalDateTime updatedAt;
    
    // Converted to a standard String to avoid R2DBC ENUM mapping crashes
    @Builder.Default
    @Column("status")
    private String status = "ACTIVE";
}