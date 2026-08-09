package com.system.analytics_engine.entities;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.OffsetDateTime;
import java.util.UUID;

@Table(name = "api_telemetry_logs")
public class ApiRequestLog {

    @Id
    private UUID id;

    @Column("correlation_id")
    private String correlationId;

    @Column("method")
    private String method;

    @Column("path")
    private String path;

    @Column("status")
    private Integer status;

    @Column("latency_ms")
    private Long latencyMs;

    @Column("ip")
    private String ip;

    @Column("user_agent")
    private String userAgent;

    @Column("req_bytes")
    private Long reqBytes;

    @Column("res_bytes")
    private Long resBytes;

    @Column("api_key")
    private String apiKey;

    @Column("created_at")
    private OffsetDateTime createdAt;

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

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
}