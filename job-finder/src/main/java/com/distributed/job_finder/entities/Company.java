package com.distributed.job_finder.entities;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.OffsetDateTime;
import java.util.UUID;

@Table("companies")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Company {

    @Id
    private UUID id;

    private String name;

    @Column("ats_type")
    private String atsType;

    @Column("board_token")
    private String boardToken;

    @Column("website_url")
    private String websiteUrl;

    @Column("created_at")
    private OffsetDateTime createdAt;
}