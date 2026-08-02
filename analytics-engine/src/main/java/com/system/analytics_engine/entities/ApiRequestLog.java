package com.system.analytics_engine.entities;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "api_telemetry_logs")
public class ApiRequestLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "correlation_id")
    private String correlationId;

    @Column(name = "method", length = 10)
    private String method;

    @Column(name = "path", columnDefinition = "TEXT")
    private String path;

    @Column(name = "status")
    private Integer status;

    @Column(name = "latency_ms")
    private Long latencyMs;

    @Column(name = "ip", length = 45)
    private String ip;

    @Column(name = "user_agent", columnDefinition = "TEXT")
    private String userAgent;

    @Column(name = "req_bytes")
    private Long reqBytes;

    @Column(name = "res_bytes")
    private Long resBytes;

    @Column(name = "api_key")
    private String apiKey;

    @Column(name = "created_at", insertable = false, updatable = false)
    private java.time.OffsetDateTime createdAt;


    // ==========================================
    // GETTERS AND SETTERS
    // ==========================================

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getCorrelationId() { return correlationId; }
    public void setCorrelationId(String correlationId) { this.correlationId = correlationId; }

    public String getMethod() { return method; }
    public void setMethod(String method) { this.method = method; }

    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }

    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }

    public Long getLatencyMs() { return latencyMs; }
    public void setLatencyMs(Long latencyMs) { this.latencyMs = latencyMs; }

    public String getIp() { return ip; }
    public void setIp(String ip) { this.ip = ip; }

    public String getUserAgent() { return userAgent; }
    public void setUserAgent(String userAgent) { this.userAgent = userAgent; }

    public Long getReqBytes() { return reqBytes; }
    public void setReqBytes(Long reqBytes) { this.reqBytes = reqBytes; }

    public Long getResBytes() { return resBytes; }
    public void setResBytes(Long resBytes) { this.resBytes = resBytes; }

    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }

    public java.time.OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(java.time.OffsetDateTime createdAt) { this.createdAt = createdAt; }
}