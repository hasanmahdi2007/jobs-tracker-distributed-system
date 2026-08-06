# Unified Global Job Aggregator (Distributed System)

![Java](https://img.shields.io/badge/Java-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-6DB33F?style=for-the-badge&logo=spring&logoColor=white)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-316192?style=for-the-badge&logo=postgresql&logoColor=white)
![Redis](https://img.shields.io/badge/Redis-DC382D?style=for-the-badge&logo=redis&logoColor=white)
![Docker](https://img.shields.io/badge/Docker-2496ED?style=for-the-badge&logo=docker&logoColor=white)
![React](https://img.shields.io/badge/React-20232A?style=for-the-badge&logo=react&logoColor=61DAFB)

An enterprise-scale, event-driven microservices architecture designed to autonomously discover, validate, scrape, and aggregate job listings from global ATS platforms (Greenhouse, Lever, Talentera, SmartRecruiters) into a high-performance interleaved feed.

## 🏗️ System Architecture

The platform operates across 5 decoupled services utilizing asynchronous messaging (Redis Streams), dual-tier rate limiting, and an Extract, Load, Transform (ELT) data pipeline.

```mermaid
flowchart TD
    %% Styling
    classDef gateway fill:#e1f5fe,stroke:#0288d1,stroke-width:2px
    classDef security fill:#fff9c4,stroke:#fbc02d,stroke-width:2px
    classDef analytics fill:#ffebee,stroke:#d32f2f,stroke-width:2px
    classDef core fill:#e8f5e9,stroke:#388e3c,stroke-width:2px
    classDef external fill:#fce4ec,stroke:#c2185b,stroke-width:2px
    classDef db fill:#f3e5f5,stroke:#7b1fa2,stroke-width:2px
    classDef admin fill:#fff3e0,stroke:#e65100,stroke-width:2px
    classDef validator fill:#ede7f6,stroke:#512da8,stroke-width:2px

    NormalUser((Normal User / Browser))
    MachineClient((Machine / B2B Client)):::admin

    subgraph GatewayCluster [1. API Gateway Cluster]
        LB{{Layer 7 Load Balancer}}
        GatewayNode[Spring Boot Gateway]:::gateway
    end

    subgraph SecurityState [Security & In-Memory State]
        RedisAuth[(Redis: Auth Cache)]:::security
        RedisRate[(Redis: Token Bucket)]:::security
        ClientDB[(PostgreSQL: Clients DB)]:::db
    end

    subgraph AnalyticsPipeline [3. Analytics Engine]
        TelemetryQueue[[Redis Streams: telemetry:stream]]:::security
        AnalyticsConsumer[Spring Boot Consumer]:::analytics
        AnalyticsDB[(PostgreSQL: OLAP)]:::db
    end

    subgraph JobFinder [2. Job Finder Service]
        RegistrationAPI[Client Registration API]:::core
        SearchAPI[Job Search API]:::core
        IngestionWorker[Ingestion Worker]:::core
    end

    subgraph External [External Systems]
        ATS[External ATS: Greenhouse, Lever, Talentera, SmartRecruiters]:::external
    end

    subgraph IngestionState [Messaging & Ingestion Queue]
        IngestionQueue[[Redis Streams: job:ingestion:stream]]:::security
    end

    subgraph CoreDB [Persistence Layer]
        JobDB[(PostgreSQL: jobs + tsvector)]:::db
    end

    subgraph ValidatorCluster [0. Job Validator Pipeline]
        CompanyDB[(PostgreSQL: Raw Company Dataset)]:::db
        SlugGen[Slug Generator]:::validator
        ValidatorRedis[(Redis: queue:slugs & verified)]:::security
        ValRunner[Reactive ATS Validator Engine]:::validator
        PyCleaner[Python Dataset Cleaner]:::validator
    end

    %% Flow Connections
    NormalUser -->|GET /jobs - Public| LB
    MachineClient -->|GET /jobs with API-Key| LB
    LB --> GatewayNode
    
    GatewayNode -->|Auth & Rate Limit Lua| RedisRate
    GatewayNode -->|Sync Search HTTP| SearchAPI
    GatewayNode -.->|Async XADD Fire-and-Forget| TelemetryQueue
    
    SearchAPI -->|Native Window Query| JobDB
    TelemetryQueue -->|Poll Batches| AnalyticsConsumer
    AnalyticsConsumer --> AnalyticsDB

    %% Validator Loop
    CompanyDB --> SlugGen --> ValidatorRedis
    ValRunner -->|Pop batches| ValidatorRedis
    ValRunner -->|Fast 200 OK Check| ATS
    ValRunner -->|Save valid tokens| ValidatorRedis
    PyCleaner -->|Sanitize Dataset| CompanyDB

    %% Ingestion
    ValidatorRedis -->|Verified tokens| IngestionWorker
    IngestionWorker -->|Scrape| ATS
    IngestionWorker -->|Push payload| IngestionQueue
    IngestionQueue -->|Normalize & Upsert| JobDB
```

---

## ⚙️ Core Microservices

### 1. The Secure Entrypoint (`api-gateway`)
* **Framework:** Spring Cloud Gateway
* **Security:** Hashed API key authentication and a custom pre-filter pipeline.
* **Traffic Control:** Dual-tier rate limiting using Redis Lua scripts (IP-based limits for public users, Token Bucket tiers for authenticated B2B machines).
* **Observability:** Emits strict zero-latency telemetry to Redis Streams (`XADD`) on every request.

### 2. Autonomous Reconnaissance (`job-validator`)
* **Discovery Engine:** Dynamically generates slug variations from raw company datasets and queues them via Redis.
* **Reactive Validation:** Uses a high-concurrency Spring WebFlux engine to perform fast HTTP probing on external ATS boards. Only targets returning `200 OK` are advanced.
* **Dataset Sanitization:** A dedicated Python worker scrubs dead links from the database to ensure processing efficiency.

### 3. The Core Business & ELT Hub (`job-finder`)
* **Ingestion Pipeline:** Non-blocking scrapers poll verified endpoints. Raw job payloads are published to `job:ingestion:stream`.
* **Transformation:** A dedicated consumer polls the stream, standardizes data via a custom 110+ country Location Normalizer, deduplicates, and upserts to PostgreSQL using strict Universal UUIDs.
* **Interleaved Feed:** The Search API bypasses standard ORM querying, executing native PostgreSQL window functions (`ROW_NUMBER() OVER PARTITION BY company_id`) to prevent high-volume enterprise companies from dominating search results. Full-text search leverages `tsvector`.

### 4. Observability Engine (`analytics-engine`)
* **Asynchronous Telemetry:** Consumes batched gateway telemetry from Redis Streams without blocking standard user traffic. 
* **Storage:** Aggregates metrics into an OLAP PostgreSQL schema to power the frontend health monitoring dashboards.

### 5. Frontend Dashboard (`react-ui`)
* **UI/UX:** A tailored, responsive React application.
* **Features:** Master-detail split-pane architecture for fast browsing, instant location/department filtering, and live system metrics monitoring API health and ingestion volumes.

---

## 🚀 Getting Started

### Prerequisites
* Docker & Docker Compose
* Java 17+
* Node.js & npm

### Running Locally
1. Clone the repository:
   ```bash
   git clone [https://github.com/hasanmahdi2007/my-distributed-system.git](https://github.com/hasanmahdi2007/my-distributed-system.git)
   cd my-distributed-system
   ```
2. Create a `.env` file in the root directory (do not commit this file) with your required database credentials:
   ```env
   POSTGRES_USER=postgres
   POSTGRES_PASSWORD=your_secure_password
   ```
3. Boot the infrastructure (PostgreSQL, Redis) and the microservices via Docker Compose:
   ```bash
   docker compose up -d
   ```

---

## 🗺️ Roadmap & Learnings

Building this 5-service architecture was a massive undertaking focused on enterprise scalability. Key software engineering (SWE) principles applied include strictly enforced **Separation of Concerns (SOC)**, robust domain-driven packaging, and fully decoupled event-driven messaging. 

**Next Steps:**
- [ ] Migrate infrastructure and deploy to AWS (ECS, RDS, ElastiCache).
- [ ] Implement CI/CD pipelines via GitHub Actions.
- [ ] Introduce Elasticsearch for advanced fuzzy matching.

---
*Developed by Hasan Mahdi.*