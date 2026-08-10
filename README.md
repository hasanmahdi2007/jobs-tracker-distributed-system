# Unified Global Job Aggregator (Distributed System)

An enterprise-scale, event-driven microservices architecture designed to autonomously discover, validate, scrape, and aggregate job listings from global ATS platforms (Greenhouse, Lever, Talentera, SmartRecruiters) into a high-performance interleaved feed.

## 🏗️ System Architecture

The platform operates across 5 decoupled services utilizing asynchronous messaging (Redis Streams), dual-tier rate limiting, and an Extract, Load, Transform (ELT) data pipeline.

```mermaid
graph TD
    %% Styling
    classDef gateway fill:#e1f5fe,stroke:#0288d1,stroke-width:2px
    classDef security fill:#fff9c4,stroke:#fbc02d,stroke-width:2px
    classDef analytics fill:#ffebee,stroke:#d32f2f,stroke-width:2px
    classDef core fill:#e8f5e9,stroke:#388e3c,stroke-width:2px
    classDef external fill:#fce4ec,stroke:#c2185b,stroke-width:2px
    classDef db fill:#f3e5f5,stroke:#7b1fa2,stroke-width:2px
    classDef admin fill:#fff3e0,stroke:#e65100,stroke-width:2px
    classDef validator fill:#ede7f6,stroke:#512da8,stroke-width:2px

    %% --------------------------------------------------------
    %% 1. ACTORS & ENTRYPOINT
    %% --------------------------------------------------------
    NormalUser((Normal User / Browser))
    MachineClient((Machine / B2B Client)):::admin

    subgraph GatewayCluster [1. The API Gateway Cluster]
        LB{{Layer 7 Load Balancer}}
        GatewayNode["Spring Boot Gateway (Netty)"]:::gateway
    end

    %% --------------------------------------------------------
    %% 2. SECURITY & STATE
    %% --------------------------------------------------------
    subgraph SecurityState [Security & In-Memory State]
        RedisAuth[("Redis: Auth Cache")]:::security
        RedisRate[("Redis: Token Bucket IP and Tier Limit")]:::security
        RedisJobCache[("Redis: Top 250 Hot Feeds RAM Cache")]:::security
        ClientDB[("PostgreSQL: Clients DB")]:::db
    end

    %% --------------------------------------------------------
    %% 3. TELEMETRY & ANALYTICS
    %% --------------------------------------------------------
    subgraph AnalyticsPipeline [3. The Analytics Engine]
        TelemetryQueue[[Redis Streams: telemetry stream]]:::security
        AnalyticsConsumer[Spring Boot Ingestion Consumer]:::analytics
        AnalyticsDB[("PostgreSQL: Analytics / OLAP")]:::db
    end

    %% --------------------------------------------------------
    %% 4. CORE BUSINESS BACKEND
    %% --------------------------------------------------------
    subgraph JobFinder [2. Job Finder Service - Reactive WebFlux]
        RegistrationAPI[Client Registration API]:::core
        SearchAPI["Job Search API (R2DBC)"]:::core
        StartupLoader[Startup Cache Warmer]:::core
        IngestionWorker[Ingestion Worker: WebClient]:::core
    end

    %% --------------------------------------------------------
    %% 5. EXTERNAL SYSTEMS & INGESTION QUEUE
    %% --------------------------------------------------------
    subgraph External [External Systems]
        ATS[External ATS APIs: Greenhouse, Lever, etc.]:::external
    end

    subgraph IngestionState [Messaging & Ingestion Queue]
        IngestionQueue[[Redis Streams: Job Events]]:::security
    end

    %% --------------------------------------------------------
    %% 6. PERSISTENCE LAYER
    %% --------------------------------------------------------
    subgraph CoreDB [Persistence Layer]
        JobDB[("PostgreSQL: companies & jobs + tsvector + UUIDs")]:::db
    end

    %% --------------------------------------------------------
    %% 7. JOB VALIDATOR PIPELINE
    %% --------------------------------------------------------
    subgraph ValidatorCluster [0. Job Validator Pipeline]
        CompanyDB[("PostgreSQL: Raw Company Dataset")]:::db
        SlugGen[Slug Generator]:::validator
        ValidatorRedis[("Redis: queue slugs & verified tokens")]:::security
        ValRunner[Reactive ATS Validator Engine]:::validator
        PyCleaner[Python Dataset Cleaner]:::validator
    end

    %% ========================================================
    %% CONNECTIONS & FLOW
    %% ========================================================

    %% Normal users just search
    NormalUser -->|1. HTTP GET /jobs/search - Public| LB
    
    %% Machines register for keys, then search
    MachineClient -->|Day 1: POST /register - Request API Key| LB
    MachineClient -->|Day 2: GET /jobs/search with X-API-Key| LB

    LB -->|Distribute Load| GatewayNode

    %% Registration Flow for Machines
    GatewayNode -->|Route Registration| RegistrationAPI
    RegistrationAPI -->|Generate and Hash Key| ClientDB
    RegistrationAPI -->|Return API Key| GatewayNode
    GatewayNode -.->|Give Key to Machine| MachineClient

    %% Security Flow with Cache Miss Logic
    GatewayNode -->|1. Check API Key first in Cache| RedisAuth
    RedisAuth -.->|2. Cache Miss: Fetch Key from DB| ClientDB
    ClientDB -.->|3. Populate Auth Cache with Key| RedisAuth

    GatewayNode -->|If Normal User - Rate Limit by IP| RedisRate
    GatewayNode -->|If Machine - Rate Limit by Token Tier and IP| RedisRate

    %% Synchronous API Flow & Hot Feed Cache Logic
    GatewayNode -->|Forward HTTP with X-Internal-Secret| SearchAPI
    
    StartupLoader -->|1. Pre-load Top 250 Jobs at Boot| JobDB
    StartupLoader -->|2. Lock Hot Feeds into RAM| RedisJobCache

    SearchAPI -->|1. Check hot:feed:sort:* O-1| RedisJobCache
    RedisJobCache -.->|2. Cache Miss or Filtered Query| JobDB
    JobDB -.->|3. Fallback and Populate RAM| RedisJobCache

    SearchAPI -->|Return HTTP 200 JSON Stream| GatewayNode
    GatewayNode -->|Return JSON Response| NormalUser
    GatewayNode -->|Return JSON Response| MachineClient

    %% Telemetry Flow
    GatewayNode -.->|Async XADD Fire and Forget| TelemetryQueue
    TelemetryQueue -->|Poll Batches from Stream| AnalyticsConsumer
    AnalyticsConsumer -->|Calculate Metrics and Bulk Insert| AnalyticsDB

    %% ========================================================
    %% JOB VALIDATOR & SCRAPING FLOW
    %% ========================================================
    
    %% Validation Phase 
    CompanyDB -->|V1. Fetch raw company names| SlugGen
    SlugGen -->|V2. Generate token variations| ValidatorRedis
    ValRunner -->|V3. Pop batches from queue| ValidatorRedis
    ValRunner -->|V4. Fast HTTP GET 200 OK Check| ATS
    ValRunner -->|V5. Save valid ATS tokens| ValidatorRedis
    PyCleaner -.->|V6. Read validated targets| ValidatorRedis
    PyCleaner -->|V7. Remove processed from dataset| CompanyDB
    PyCleaner -->|V8. Repeat for next ATS| SlugGen

    %% Ingestion Phase
    ValidatorRedis -->|A. Feed verified tokens| IngestionWorker
    IngestionWorker -->|B. Poll verified ATS endpoints| ATS
    IngestionWorker -->|C. Publish raw job events| IngestionQueue
    IngestionQueue -->|D. Consume, hash, and deduplicate| JobDB
```

## ⚙️ Core Microservices

### 1. The Secure Entrypoint (api-gateway)
*   **Framework:** Spring Cloud Gateway
*   **Security:** Hashed API key authentication and a custom pre-filter pipeline with fallback database checks.
*   **Traffic Control:** Dual-tier rate limiting using Redis Lua scripts (IP-based limits for public users, Token Bucket tiers for authenticated B2B machines).
*   **Observability:** Emits strict zero-latency telemetry to Redis Streams (`XADD`) on every request.

### 2. Autonomous Reconnaissance (job-validator)
*   **Discovery Engine:** Dynamically generates slug variations from raw company datasets and queues them via Redis.
*   **Reactive Validation:** Uses a high-concurrency Spring WebFlux engine to perform fast HTTP probing on external ATS boards. Only targets returning `200 OK` are advanced.
*   **Dataset Sanitization:** A dedicated Python worker scrubs dead links from the database to ensure processing efficiency.

### 3. The Core Business & ELT Hub (job-finder)
*   **Reactive Architecture:** 100% non-blocking data retrieval leveraging **Spring WebFlux** and **R2DBC**, maximizing throughput on constrained hardware via an optimized 5-connection pool.
*   **Startup Cache Warming:** Eradicates database cache stampedes (thundering herds) by triggering a boot-time sequence that permanently locks the Top 250 diverse/recent job feeds directly into a Redis Hot-Feed RAM cache.
*   **Ingestion Pipeline:** Non-blocking scrapers poll verified endpoints. Raw job payloads are published to `job:ingestion:stream`.
*   **Transformation:** A dedicated consumer polls the stream, standardizes data via a custom 110+ country Location Normalizer, deduplicates, and upserts to PostgreSQL using strict **Universal UUIDs**.
*   **Interleaved Feed:** The Search API bypasses standard ORM querying, executing native PostgreSQL window functions (`ROW_NUMBER() OVER PARTITION BY company_id`) to prevent high-volume enterprise companies from dominating search results. Full-text search leverages `tsvector`.

### 4. Observability Engine (analytics-engine)
*   **Asynchronous Telemetry:** Consumes batched gateway telemetry from Redis Streams without blocking standard user traffic.
*   **Storage:** Aggregates metrics into an OLAP PostgreSQL schema to power the frontend health monitoring dashboards.

### 5. Frontend Dashboard (job-board-ui)
*   **UI/UX:** A tailored, responsive React application.
*   **Features:** Master-detail split-pane architecture for fast browsing, instant location/department filtering, and live system metrics monitoring API health and ingestion volumes.

## 🚀 Getting Started

**Prerequisites**
*   Docker & Docker Compose
*   Java 17+
*   Node.js & npm

**Running Locally**

1. Clone the repository:
```bash
git clone [https://github.com/hasanmahdi2007/jobs-tracker-distributed-system.git](https://github.com/hasanmahdi2007/jobs-tracker-distributed-system.git)
cd jobs-tracker-distributed-system
```

2. Create a `.env` file in the root directory (do not commit this file) with your required database credentials:
```env
POSTGRES_USER=postgres
POSTGRES_PASSWORD=your_secure_password
```

3. Boot the infrastructure (PostgreSQL, Redis) via Docker Compose:
```bash
docker compose up -d
```

4. Run the individual microservices via Maven (Example: Scraper target):
```powershell
$env:POSTGRES_PASSWORD="your_secure_password"
mvn spring-boot:run '-Dspring-boot.run.arguments=--server.port=8089 --scraper.target=smartrecruiters'
```

## 🗺️ Roadmap & Learnings

Building this 5-service architecture was a massive undertaking focused on enterprise scalability. Key software engineering (SWE) principles applied include strictly enforced Separation of Concerns (SOC), robust domain-driven packaging, and fully decoupled event-driven messaging.

**Next Steps:**
*   Migrate infrastructure and deploy to AWS (ECS, RDS, ElastiCache).
*   Implement CI/CD pipelines via GitHub Actions.
*   Introduce Elasticsearch for advanced fuzzy matching.

*Developed by Hasan Mahdi.*