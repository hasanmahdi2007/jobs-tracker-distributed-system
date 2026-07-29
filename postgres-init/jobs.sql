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

-- Clean insert for Middle East Talentera Portals
INSERT INTO companies (name, ats_type, board_token, website_url) VALUES 
('Abdullah Abdulghani & Bros (AAB)', 'TALENTERA', 'aab', 'https://aab.talentera.com'),
('Telecommunications Regulatory Authority', 'TALENTERA', 'tra', 'https://tra.talentera.com'),
('Royal Jordanian', 'TALENTERA', 'royaljordanian', 'https://royaljordanian.talentera.com'),
('Mobica', 'TALENTERA', 'mobica', 'https://mobica.talentera.com'),
('Masar', 'TALENTERA', 'masar', 'https://masar.talentera.com'),
('Tamimi Markets', 'TALENTERA', 'tamimi', 'https://tamimi.talentera.com'),
('Mobily', 'TALENTERA', 'mobily', 'https://mobily.talentera.com'),
('Orange Bed & Bath', 'TALENTERA', 'orangebedbath', 'https://orangebedbath.talentera.com'),
('Magrabi', 'TALENTERA', 'magrabi', 'https://magrabi.talentera.com'),
('Rawabi Holding', 'TALENTERA', 'rawabi', 'https://rawabi.talentera.com'),
('Itqan', 'TALENTERA', 'itqan', 'https://itqan.talentera.com'),
('Council of Health Insurance (CHI)', 'TALENTERA', 'chi', 'https://chi.talentera.com'),
('Radwa Food Products', 'TALENTERA', 'radwa', 'https://radwa.talentera.com'),
('Talentera Careers', 'TALENTERA', 'careers', 'https://careers.talentera.com'),
('Integral', 'TALENTERA', 'integral', 'https://integral.talentera.com'),
('National Center for Environmental Compliance (NCEC)', 'TALENTERA', 'ncec', 'https://ncec.talentera.com'),
('Panda Retail Company', 'TALENTERA', 'panda', 'https://panda.talentera.com'),
('BEAM', 'TALENTERA', 'beam', 'https://beam.talentera.com'),
('Care National Hospital', 'TALENTERA', 'care', 'https://care.talentera.com'),
('Badruddin', 'TALENTERA', 'badruddin', 'https://badruddin.talentera.com'),
('Saudi Investment Bank (SAIB)', 'TALENTERA', 'saib', 'https://saib.talentera.com'),
('General Organization for Social Insurance (GOSI)', 'TALENTERA', 'gosi', 'https://gosi.talentera.com'),
('Al-Dawaa Pharmacies', 'TALENTERA', 'al-dawaa', 'https://al-dawaa.talentera.com'),
('Save the Children', 'TALENTERA', 'savethechildren', 'https://savethechildren.talentera.com'),
('Taiba Investments', 'TALENTERA', 'taiba', 'https://taiba.talentera.com'),
('Concrete', 'TALENTERA', 'concrete', 'https://concrete.talentera.com'),
('Maliks', 'TALENTERA', 'maliks', 'https://maliks.talentera.com'),
('Ejada', 'TALENTERA', 'ejada', 'https://ejada.talentera.com'),
('Talentera Demo', 'TALENTERA', 'demo', 'https://demo.talentera.com'),
('Saudi Bar Association (SBA)', 'TALENTERA', 'sba', 'https://sba.talentera.com'),
('Choithrams', 'TALENTERA', 'choithrams', 'https://choithrams.talentera.com'),
('Alfardan Group', 'TALENTERA', 'alfardan', 'https://alfardan.talentera.com'),
('Gulf International Bank (GIB)', 'TALENTERA', 'gib', 'https://gib.talentera.com'),
('MSD', 'TALENTERA', 'msd', 'https://msd.talentera.com'),
('SAL Saudi Logistics Services', 'TALENTERA', 'sal', 'https://sal.talentera.com'),
('Saudi Petroleum Services Polytechnic (SPSP)', 'TALENTERA', 'spsp', 'https://spsp.talentera.com'),
('Batelco', 'TALENTERA', 'batelco', 'https://batelco.talentera.com'),
('National Water Company (NWC)', 'TALENTERA', 'nwc', 'https://nwc.talentera.com'),
('Capital Market Authority (CMA)', 'TALENTERA', 'cma', 'https://cma.talentera.com'),
('MODON', 'TALENTERA', 'modon', 'https://modon.talentera.com'),
('Al Rashed', 'TALENTERA', 'alrashed', 'https://alrashed.talentera.com')
ON CONFLICT (board_token) DO UPDATE 
SET name = EXCLUDED.name, ats_type = EXCLUDED.ats_type;