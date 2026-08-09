package com.hasan.gateway.entities;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.ZonedDateTime;
import java.util.UUID;

@Table("clients")
public class Client {
    
    @Id
    private UUID id;

    @Column("company_name")
    private String companyName;

    @Column("email")
    private String email;

    @Column("tier_type")
    private String tierType;

    @Column("created_at")
    private ZonedDateTime createdAt = ZonedDateTime.now();

    // --- EXPLICIT GETTERS & SETTERS ---

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getCompanyName() { return companyName; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getTierType() { return tierType; }
    public void setTierType(String tierType) { this.tierType = tierType; }

    public ZonedDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(ZonedDateTime createdAt) { this.createdAt = createdAt; }
}