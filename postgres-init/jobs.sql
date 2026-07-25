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
('Careem', 'GREENHOUSE', 'careem', 'https://www.careem.com'),
('Nana', 'GREENHOUSE', 'nana.direct', 'https://nana.direct'),
('Salla', 'GREENHOUSE', 'salla', 'https://salla.com'),
('Zid', 'GREENHOUSE', 'zid', 'https://zid.sa'),
('Sary', 'GREENHOUSE', 'sary', 'https://sary.com'),
('Floward', 'GREENHOUSE', 'floward', 'https://floward.com'),
('Eyewa', 'GREENHOUSE', 'eyewa', 'https://eyewa.com'),
('Tabby', 'GREENHOUSE', 'tabby.ai', 'https://tabby.ai'),
('Tamara', 'GREENHOUSE', 'tamara.co', 'https://tamara.co'),
('Paymob', 'GREENHOUSE', 'paymob', 'https://paymob.com'),
('Thndr', 'GREENHOUSE', 'thndr', 'https://thndr.app'),
('Sarwa', 'GREENHOUSE', 'sarwa.co', 'https://www.sarwa.co'),
('BitOasis', 'GREENHOUSE', 'bitoasis', 'https://bitoasis.net'),
('NymCard', 'GREENHOUSE', 'nymcard', 'https://nymcard.com'),
('Tarabut Gateway', 'GREENHOUSE', 'tarabutgateway', 'https://tarabutgateway.com'),
('MNT-Halan', 'GREENHOUSE', 'mnthalan', 'https://mnt-halan.com'),
('Checkout.com', 'GREENHOUSE', 'checkoutcom', 'https://www.checkout.com'),
('Stripe MENA', 'GREENHOUSE', 'stripe', 'https://stripe.com'),
('Property Finder', 'GREENHOUSE', 'propertyfinder', 'https://www.propertyfinder.ae'),
('Bayut', 'GREENHOUSE', 'bayut', 'https://www.bayut.com'),
('Dubizzle', 'GREENHOUSE', 'dubizzle', 'https://dubizzle.com'),
('Huspy', 'GREENHOUSE', 'huspy', 'https://huspy.com'),
('Nawy', 'GREENHOUSE', 'nawy', 'https://www.nawy.com'),
('TruKKer', 'GREENHOUSE', 'trukker.com', 'https://trukker.com'),
('Trella', 'GREENHOUSE', 'trella', 'https://www.trella.app'),
('Cafu', 'GREENHOUSE', 'cafu', 'https://www.cafu.com'),
('Swvl', 'GREENHOUSE', 'swvl', 'https://swvl.com'),
('Vezeeta', 'GREENHOUSE', 'vezeeta', 'https://www.vezeeta.com'),
('Abwaab', 'GREENHOUSE', 'abwaab', 'https://abwaab.com'),
('Instabug', 'GREENHOUSE', 'instabug', 'https://instabug.com'),
('Unifonic', 'GREENHOUSE', 'unifonic', 'https://www.unifonic.com'),
('Foodics', 'GREENHOUSE', 'foodics', 'https://www.foodics.com'),
('Mozn', 'GREENHOUSE', 'mozn', 'https://mozn.sa'),
('Penny Software', 'GREENHOUSE', 'penny', 'https://penny.software'),
('Qoyod', 'GREENHOUSE', 'qoyod', 'https://www.qoyod.com'),
('Mawdoo3', 'GREENHOUSE', 'mawdoo3', 'https://mawdoo3.com'),
('Sumerge', 'GREENHOUSE', 'sumerge', 'https://sumerge.com'),
('Robusta', 'GREENHOUSE', 'robusta', 'https://robusta.studio'),
('Anghami', 'GREENHOUSE', 'anghami', 'https://www.anghami.com'),
('Starzplay', 'GREENHOUSE', 'starzplay', 'https://starzplay.com'),
('Playhera', 'GREENHOUSE', 'playhera', 'https://playhera.com'),
('Sandsoft Games', 'GREENHOUSE', 'sandsoft', 'https://sandsoft.com'),
('Wego', 'GREENHOUSE', 'wego', 'https://www.wego.com'),
('Xische', 'GREENHOUSE', 'xische', 'https://xische.com'),
('Tap Payments', 'GREENHOUSE', 'tappayments', 'https://www.tap.company'),
('HALA', 'GREENHOUSE', 'hala', 'https://hala.com'),
('RemotePass', 'GREENHOUSE', 'remotepass', 'https://www.remotepass.com'),
('EMPG', 'GREENHOUSE', 'empg', 'https://www.empg.com'),
('Yassir', 'GREENHOUSE', 'yassir', 'https://yassir.com'),
('Rain', 'GREENHOUSE', 'rain', 'https://www.rain.bh'),
('Calo', 'GREENHOUSE', 'calo', 'https://calo.app'),
('Eat App', 'GREENHOUSE', 'eatapp', 'https://eatapp.co'),
('Stake', 'GREENHOUSE', 'stake', 'https://getstake.com'),
('Postpay', 'GREENHOUSE', 'postpay', 'https://postpay.io'),
('Cashew', 'GREENHOUSE', 'cashewpayments', 'https://cashewpayments.com'),
('Astra Tech', 'GREENHOUSE', 'astratech', 'https://astratech.ae'),
('Almosafer', 'GREENHOUSE', 'almosafer', 'https://almosafer.com'),
('Seera Group', 'GREENHOUSE', 'seeragroup', 'https://seera.sa'),
('Bosta', 'GREENHOUSE', 'bosta', 'https://bosta.co'),
('MoneyFellows', 'GREENHOUSE', 'moneyfellows', 'https://moneyfellows.com'),
('Tamatem', 'GREENHOUSE', 'tamatem', 'https://tamatem.co'),
('OpenSooq', 'GREENHOUSE', 'opensooq', 'https://opensooq.com')
ON CONFLICT (name) DO NOTHING;