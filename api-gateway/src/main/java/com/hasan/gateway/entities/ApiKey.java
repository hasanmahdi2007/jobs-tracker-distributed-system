package com.hasan.gateway.entities;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.ZonedDateTime;
import java.util.UUID;

@Table("api_keys")
public class ApiKey {

    @Id
    private UUID id;

    @Column("client_id")
    private UUID clientId;

    @Column("key_hash")
    private String keyHash;

    @Column("request_limit")
    private Integer requestLimit;

    @Column("current_usage")
    private Integer currentUsage = 0;

    @Version 
    private Long version;

    @Column("tier")
    private String tier = "FREE";

    @Column("is_active")
    private Boolean isActive = true; 

    @Column("last_accessed_minute")
    private ZonedDateTime lastAccessedMinute = ZonedDateTime.now();

    @Column("created_at")
    private ZonedDateTime createdAt = ZonedDateTime.now();

    // --- EXPLICIT GETTERS & SETTERS (Guaranteed to work without Lombok) ---

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getClientId() { return clientId; }
    public void setClientId(UUID clientId) { this.clientId = clientId; }

    public String getKeyHash() { return keyHash; }
    public void setKeyHash(String keyHash) { this.keyHash = keyHash; }

    public Integer getRequestLimit() { return requestLimit; }
    public void setRequestLimit(Integer requestLimit) { this.requestLimit = requestLimit; }

    public Integer getCurrentUsage() { return currentUsage; }
    public void setCurrentUsage(Integer currentUsage) { this.currentUsage = currentUsage; }

    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }

    public String getTier() { return tier != null ? tier : "FREE"; }
    public void setTier(String tier) { this.tier = tier; }

    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }

    public ZonedDateTime getLastAccessedMinute() { return lastAccessedMinute; }
    public void setLastAccessedMinute(ZonedDateTime lastAccessedMinute) { this.lastAccessedMinute = lastAccessedMinute; }

    public ZonedDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(ZonedDateTime createdAt) { this.createdAt = createdAt; }
}