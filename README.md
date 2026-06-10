# CodeSage AI

> AI-powered Developer Intelligence Platform — Modular Monolith

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Backend | Java 24, Spring Boot 3.5.x, Spring Security, Spring Data JPA |
| Database | PostgreSQL 17, Flyway, Redis 7 |
| AI | Gemini API, LangChain4j, ChromaDB |
| Frontend | React 19, TypeScript, Tailwind CSS 4, Vite |
| DevOps | Docker, Docker Compose, Nginx |

---

## Quick Start

### 1. Clone and configure environment

```bash
git clone <repo-url>
cd codeSage
cp .env.example .env
# Edit .env with your values
```

### 2. Start all infrastructure with Docker Compose

```bash
docker-compose up -d
```

This starts:
- PostgreSQL 17 on port `5432`
- Redis 7 on port `6379`
- ChromaDB on port `8000`
- Backend (Spring Boot) on port `8080`
- Nginx on port `80`

### 3. Run backend locally (without Docker)

```bash
cd backend
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

### 4. Run frontend locally

```bash
cd frontend
npm install
npm run dev
```

Frontend: http://localhost:5173  
API Docs: http://localhost:8080/swagger-ui.html  
Health:   http://localhost:8080/api/v1/health

---

## Project Structure

```
codeSage/
├── backend/                     # Spring Boot Modular Monolith
│   └── src/main/java/com/codesage/
│       ├── config/              # Spring configuration
│       ├── security/            # Spring Security
│       ├── exception/           # Global exception handling
│       ├── common/              # Shared DTOs, utils, constants
│       ├── infrastructure/      # Base entities, persistence
│       └── domain/              # Feature modules
│           └── health/          # Health module (Sprint 1)
├── frontend/                    # React 19 SPA
│   └── src/
│       ├── features/            # Domain feature slices
│       ├── components/          # Shared UI components
│       ├── lib/                 # Axios instance
│       ├── router/              # React Router
│       ├── types/               # TypeScript API types
│       └── utils/               # Utilities
├── docker/
│   ├── backend/Dockerfile       # Multi-stage JDK→JRE build
│   └── nginx/nginx.conf         # Reverse proxy config
├── docker-compose.yml           # Full dev stack
├── .env.example                 # Environment template
└── README.md
```

---

## Architecture

- **Pattern**: Modular Monolith with clear microservice extraction points
- **API Base**: `/api/v1`
- **Auth**: GitHub OAuth + JWT in HttpOnly cookies (Sprint 2)
- **Schema**: Flyway migrations only — no Hibernate DDL
- **Caching**: Redis mandatory — no in-memory fallback
- **Threading**: Java Virtual Threads enabled (`spring.threads.virtual.enabled=true`)
- **Docs**: OpenAPI at `/swagger-ui.html`

See `CodeSage AI Architecture Specification v1.0.docx` for the full spec.

---

## Development Roadmap

| Sprint | Feature |
|--------|---------|
| ✅ 1 | Infrastructure (current) |
| 2 | GitHub OAuth + JWT Authentication |
| 3 | Repository Integration (GitHub APIs) |
| 4 | Repository Ingestion (Clone, Parse, Chunk) |
| 5 | AI Foundation (Gemini, LangChain4j, ChromaDB) |
| 6 | Repository Chat (RAG) |
| 7 | Technical Debt Analyzer |
| 8 | AI Pull Request Reviewer |
| 9 | Documentation Generator |
| 10 | Developer Intelligence Dashboard |

---

## API Endpoints

| Method | Path | Description | Auth |
|--------|------|-------------|------|
| GET | `/api/v1/health` | Infrastructure health check | Public |
| GET | `/actuator/health` | Spring actuator health | Public |
| GET | `/swagger-ui.html` | API documentation | Public |

---

## Database

PostgreSQL schema managed by Flyway. Migrations in `backend/src/main/resources/db/migration/`.

Current tables (V1):
- `users` — GitHub OAuth users
- `repositories` — Connected GitHub repos
- `repository_files` — Ingested source files
- `repository_metrics` — Analytics snapshots
- `vector_chunks` — ChromaDB chunk metadata
- `chat_sessions` — AI chat conversations
- `chat_messages` — Chat message history
- `pr_reviews` — AI PR review records
- `tech_debt_reports` — Technical debt analysis
- `security_findings` — Security vulnerability findings
- `audit_log` — Immutable audit trail
