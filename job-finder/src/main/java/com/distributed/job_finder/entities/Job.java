package com.distributed.job_finder.entities;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

import com.distributed.job_finder.enums.JobStatus;

@Entity
@Table(name = "jobs", uniqueConstraints = {
    @UniqueConstraint(name = "uk_ats_company", columnNames = {"ats_job_id", "company_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Job {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "ats_job_id", nullable = false)
    private String atsJobId;

    @Column(name = "company_id", nullable = false)
    private UUID companyId;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "location")
    private String location;

    @Column(name = "department")
    private String department;

    @Column(name = "apply_url")
    private String applyUrl;

    @Column(name = "description_text", columnDefinition = "TEXT")
    private String descriptionText;

    // --- ADDED MISSING COLUMNS BELOW ---

    @Column(name = "fingerprint_hash")
    private String fingerprintHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", columnDefinition = "job_status")
    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.NAMED_ENUM)
    private JobStatus status;

    @Column(name = "experience_level")
    private String experienceLevel;

    @Column(name = "employment_type")
    private String employmentType;

    @Column(name = "salary_min")
    private Integer salaryMin;

    @Column(name = "salary_max")
    private Integer salaryMax;

    @Column(name = "salary_currency")
    private String salaryCurrency;

    // -----------------------------------

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.status == null) {
            this.status = JobStatus.ACTIVE;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}