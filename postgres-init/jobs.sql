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
    
    -- Lifecycle State
    status job_status DEFAULT 'ACTIVE',
    
    -- Timestamps
    last_seen_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    
    -- PostgreSQL Full-Text Search Vector
    search_vector tsvector
);

-- 3. Indexes for Sub-Millisecond Search Performance
CREATE INDEX idx_jobs_search_vector ON jobs USING GIN (search_vector);
-- UPDATED: Now uses created_at instead of posted_at
CREATE INDEX idx_jobs_active_created ON jobs(status, created_at DESC);
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
    SET status = 'EXPIRED'
    -- REMOVED closed_at update
    WHERE company_id = target_company_id 
      AND status = 'ACTIVE' 
      -- Expires anything not updated in the last 24 hours
      AND last_seen_at < (CURRENT_TIMESTAMP - INTERVAL '24 hours');
END;
$$;

SELECT current_database();

INSERT INTO companies (name, ats_type, board_token, website_url) VALUES 
-- Regional MENA
('Careem', 'GREENHOUSE', 'careem', 'https://www.careem.com'),
('Stripe', 'GREENHOUSE', 'stripe', 'https://stripe.com'),
('Hala', 'GREENHOUSE', 'hala', 'https://www.halapay.com'),
('Tamara', 'GREENHOUSE', 'tamara', 'https://tamara.co'),
-- Global Tech & Unicorns
('Airbnb', 'GREENHOUSE', 'airbnb', 'https://www.airbnb.com'),
('Anthropic', 'GREENHOUSE', 'anthropic', 'https://www.anthropic.com'),
('Asana', 'GREENHOUSE', 'asana', 'https://asana.com'),
('Adyen', 'GREENHOUSE', 'adyen', 'https://www.adyen.com'),
('Airtable', 'GREENHOUSE', 'airtable', 'https://airtable.com'),
('Box', 'GREENHOUSE', 'boxinc', 'https://www.box.com'),
('Brex', 'GREENHOUSE', 'brex', 'https://www.brex.com'),
('Canonical', 'GREENHOUSE', 'canonical', 'https://canonical.com'),
('Chime', 'GREENHOUSE', 'chime', 'https://www.chime.com'),
('Cloudflare', 'GREENHOUSE', 'cloudflare', 'https://www.cloudflare.com'),
('Coinbase', 'GREENHOUSE', 'coinbase', 'https://www.coinbase.com'),
('Coursera', 'GREENHOUSE', 'coursera', 'https://www.coursera.com'),
('Databricks', 'GREENHOUSE', 'databricks', 'https://databricks.com'),
('Datadog', 'GREENHOUSE', 'datadog', 'https://www.datadoghq.com'),
('Discord', 'GREENHOUSE', 'discord', 'https://discord.com'),
('DoorDash', 'GREENHOUSE', 'doordashglobal', 'https://www.doordash.com'),
('Dropbox', 'GREENHOUSE', 'dropbox', 'https://www.dropbox.com'),
('Duolingo', 'GREENHOUSE', 'duolingo', 'https://www.duolingo.com'),
('Elastic', 'GREENHOUSE', 'elastic', 'https://www.elastic.co'),
('Epic Games', 'GREENHOUSE', 'epicgames', 'https://www.epicgames.com'),
('Fastly', 'GREENHOUSE', 'fastly', 'https://www.fastly.com'),
('Figma', 'GREENHOUSE', 'figma', 'https://www.figma.com'),
('Gemini', 'GREENHOUSE', 'gemini', 'https://www.gemini.com'),
('GitLab', 'GREENHOUSE', 'gitlab', 'https://about.gitlab.com'),
('HubSpot', 'GREENHOUSE', 'hubspot', 'https://www.hubspot.com'),
('Instacart', 'GREENHOUSE', 'instacart', 'https://www.instacart.com'),
('Lyft', 'GREENHOUSE', 'lyft', 'https://www.lyft.com'),
('Masterclass', 'GREENHOUSE', 'masterclass', 'https://www.masterclass.com'),
('MongoDB', 'GREENHOUSE', 'mongodb', 'https://www.mongodb.com'),
('Monzo', 'GREENHOUSE', 'monzo', 'https://monzo.com'),
('N26', 'GREENHOUSE', 'n26', 'https://n26.com'),
('Okta', 'GREENHOUSE', 'okta', 'https://www.okta.com'),
('PagerDuty', 'GREENHOUSE', 'pagerduty', 'https://www.pagerduty.com'),
('Papaya Global', 'GREENHOUSE', 'papaya', 'https://papayaglobal.com'),
('Peloton', 'GREENHOUSE', 'peloton', 'https://www.onepeloton.com'),
('Pinterest', 'GREENHOUSE', 'pinterest', 'https://www.pinterest.com'),
('Postman', 'GREENHOUSE', 'postman', 'https://www.postman.com'),
('Reddit', 'GREENHOUSE', 'reddit', 'https://www.reddit.com'),
('Remote', 'GREENHOUSE', 'remote', 'https://remote.com'),
('Roblox', 'GREENHOUSE', 'roblox', 'https://www.roblox.com'),
('Robinhood', 'GREENHOUSE', 'robinhood', 'https://robinhood.com'),
('Scale AI', 'GREENHOUSE', 'getscale', 'https://scale.com'),
('Smartsheet', 'GREENHOUSE', 'smartsheet', 'https://www.smartsheet.com'),
('Squarespace', 'GREENHOUSE', 'squarespace', 'https://www.squarespace.com'),
('Twitch', 'GREENHOUSE', 'twitch', 'https://www.twitch.tv'),
('Udemy', 'GREENHOUSE', 'udemy', 'https://www.udemy.com'),
('Webflow', 'GREENHOUSE', 'webflow', 'https://webflow.com'),
('Wiz', 'GREENHOUSE', 'wizinc', 'https://www.wiz.io'),
('Zscaler', 'GREENHOUSE', 'zscaler', 'https://www.zscaler.com')
ON CONFLICT (name) DO UPDATE 
SET board_token = EXCLUDED.board_token, ats_type = EXCLUDED.ats_type;