CREATE DATABASE analytics_db;

-- Connect to analytics_db before running the rest
\c analytics_db;

DROP TABLE IF EXISTS api_telemetry_logs;

CREATE TABLE api_telemetry_logs (
    id BIGSERIAL PRIMARY KEY,
    correlation_id VARCHAR(255),
    method VARCHAR(10),
    path TEXT,
    status INTEGER,
    latency_ms BIGINT,
    ip VARCHAR(45),
    user_agent TEXT,
    req_bytes BIGINT,
    res_bytes BIGINT,
    api_key VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Fast lookup for correlation tracing
CREATE INDEX idx_telemetry_correlation_id ON api_telemetry_logs(correlation_id);

-- Essential for time-range filtering (e.g. "Last 24 Hours")
CREATE INDEX idx_telemetry_created_at ON api_telemetry_logs(created_at DESC);

-- Speeds up API Key telemetry reporting
CREATE INDEX idx_telemetry_api_key_status ON api_telemetry_logs(api_key, status);