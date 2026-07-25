CREATE DATABASE job_finder_db;

-- (Remember to connect to job_finder_db in your GUI before running the rest!)

-- Drop everything cleanly in the correct order (respecting foreign key constraints)
DROP TRIGGER IF EXISTS trg_jobs_search_vector_update ON jobs;
DROP FUNCTION IF EXISTS update_job_search_vector();
DROP PROCEDURE IF EXISTS expire_stale_jobs(UUID);
DROP TABLE IF EXISTS jobs CASCADE;
DROP TABLE IF EXISTS companies CASCADE;
DROP TYPE IF EXISTS job_status CASCADE;

-- Enable required extension
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Define custom ENUM for job lifecycle management
CREATE TYPE job_status AS ENUM ('ACTIVE', 'STALE', 'EXPIRED', 'ARCHIVED');

-- 1. Companies Table
CREATE TABLE companies (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL UNIQUE,
    ats_type VARCHAR(50) NOT NULL,            
    board_token VARCHAR(255) NOT NULL,        
    website_url TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 2. Jobs Table
CREATE TABLE jobs (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    company_id UUID NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    company_name VARCHAR(255) NOT NULL,
    title VARCHAR(255) NOT NULL,
    
    -- Role & Compensation
    experience_level VARCHAR(50),              -- e.g., 'Junior', 'Mid', 'Senior', 'Lead'
    employment_type VARCHAR(50),               -- e.g., 'Full-time', 'Contract', 'Internship'
    salary_min INTEGER,                        -- e.g., 90000
    salary_max INTEGER,                        -- e.g., 130000
    salary_currency VARCHAR(10) DEFAULT 'USD', -- e.g., 'USD', 'EUR', 'AED'
    
    location VARCHAR(255),
    department VARCHAR(255),
    description_text TEXT,
    apply_url TEXT NOT NULL,
    ats_job_id VARCHAR(100),                  
    
    -- Idempotency & Deduplication Key
    fingerprint_hash VARCHAR(64), 
    
    -- Lifecycle State
    status job_status DEFAULT 'ACTIVE',
    
    -- Timestamps
    posted_at TIMESTAMP WITH TIME ZONE,
    last_seen_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    closed_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    
    -- PostgreSQL Full-Text Search Vector
    search_vector tsvector
);

-- 3. Indexes for Sub-Millisecond Search Performance
CREATE INDEX idx_jobs_search_vector ON jobs USING GIN (search_vector);
CREATE INDEX idx_jobs_active_posted ON jobs(status, posted_at DESC);
CREATE INDEX idx_jobs_company_id ON jobs(company_id);

-- 🚀 Partial index heavily optimized for the expiration sweep
CREATE INDEX idx_jobs_sweep_optimizer ON jobs(company_id, last_seen_at) WHERE status = 'ACTIVE';

-- 4. Automated Search Vector Trigger
CREATE OR REPLACE FUNCTION update_job_search_vector() RETURNS trigger AS $$
BEGIN
    NEW.search_vector :=
        setweight(to_tsvector('english', coalesce(NEW.title, '')), 'A') ||
        setweight(to_tsvector('english', coalesce(NEW.experience_level, '')), 'A') || 
        setweight(to_tsvector('english', coalesce(NEW.department, '')), 'B') ||
        setweight(to_tsvector('english', coalesce(NEW.location, '')), 'B') ||
        setweight(to_tsvector('english', coalesce(NEW.description_text, '')), 'C');
    RETURN NEW;
END
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_jobs_search_vector_update
BEFORE INSERT OR UPDATE ON jobs
FOR EACH ROW EXECUTE FUNCTION update_job_search_vector();

-- 5. 🚀 The Sweeper Procedure
CREATE OR REPLACE PROCEDURE expire_stale_jobs(target_company_id UUID)
LANGUAGE plpgsql
AS $$
BEGIN
    UPDATE jobs 
    SET status = 'EXPIRED', 
        closed_at = CURRENT_TIMESTAMP
    WHERE company_id = target_company_id 
      AND status = 'ACTIVE' 
      -- Expires anything not updated in the last 24 hours
      AND last_seen_at < (CURRENT_TIMESTAMP - INTERVAL '24 hours');
END;
$$;

SELECT current_database();

INSERT INTO companies (name, ats_type, board_token, website_url) VALUES 
('GitHub', 'GREENHOUSE', 'github', 'https://github.com'),
('Twitch', 'GREENHOUSE', 'twitch', 'https://twitch.tv'),
('Discord', 'GREENHOUSE', 'discord', 'https://discord.com'),
('Stripe', 'GREENHOUSE', 'stripe', 'https://stripe.com'),
('Careem', 'GREENHOUSE', 'careem', 'https://www.careem.com'),
('Anghami', 'GREENHOUSE', 'anghami', 'https://www.anghami.com'),
('Instabug', 'GREENHOUSE', 'instabug', 'https://instabug.com'),
('Swvl', 'GREENHOUSE', 'swvl', 'https://swvl.com'),
('Kitopi', 'GREENHOUSE', 'kitopi', 'https://www.kitopi.com'),
('Tabby', 'GREENHOUSE', 'tabby.ai', 'https://tabby.ai'),
('Tamara', 'GREENHOUSE', 'tamara.co', 'https://tamara.co'),
('Sarwa', 'GREENHOUSE', 'sarwa.co', 'https://www.sarwa.co'),
('TruKKer', 'GREENHOUSE', 'trukker.com', 'https://trukker.com'),
('Unifonic', 'GREENHOUSE', 'unifonic', 'https://www.unifonic.com'),
('Nana', 'GREENHOUSE', 'nana.direct', 'https://nana.direct'),
('Eyewa', 'GREENHOUSE', 'eyewa', 'https://eyewa.com'),
('Figma', 'GREENHOUSE', 'figma', 'https://www.figma.com'),
('Coinbase', 'GREENHOUSE', 'coinbase', 'https://www.coinbase.com'),
('Reddit', 'GREENHOUSE', 'reddit', 'https://www.reddit.com'),
('Robinhood', 'GREENHOUSE', 'robinhood', 'https://robinhood.com');