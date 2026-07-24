CREATE DATABASE analytics_db;

-- 2. Switch inside it immediately

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
    api_key VARCHAR(255)
);

-- Adding an index to the correlation_id makes searching for a specific request lightning fast
CREATE INDEX idx_correlation_id ON api_telemetry_logs(correlation_id);