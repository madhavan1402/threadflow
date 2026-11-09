# ThreadFlow — Deployment & Operations Guide

Production Order-Tracking Platform for Knitwear Exporters in Tiruppur

- **GitHub Repository**: [https://github.com/madhavan1402/threadflow](https://github.com/madhavan1402/threadflow)
- **Deployment Status**: Not deployed / Local demonstration
- **Primary Technology Stack**: Java 21, Spring Boot 3.5.7, MySQL 8, React 18, TypeScript, Vite

---

## 1. Project Overview

ThreadFlow is an enterprise SaaS application designed for the knitwear export manufacturing ecosystem in Tiruppur, India. It coordinates three essential operational workflows:

1. **Merchandising Operations (Desktop-First Workspace)**: Managing export purchase orders (POs), calculating backward Time & Action (TNA) schedules, tracking line delivery pace, reviewing Tamil vernacular AI classifications, and maintaining immutable ex-factory revision audit trails.
2. **Shop Floor Execution (Mobile-First Workspace)**: Providing supervisors with touch-optimized interfaces (≥48px tap targets) for daily shift entry, Tamil voice/text issue logging, inspection photo uploads, and offline queue synchronization via IndexedDB.
3. **Buyer Portal (Zero-Trust Read-Only Workspace)**: Giving international retail buyers real-time visibility into order milestone progress, confirmed schedule delay warnings, and approved inspection photos with database-enforced tenant isolation.

### Architecture
- **Frontend**: Single Page Application built with React 18, TypeScript, and Vite.
- **Backend**: Spring Boot 3.5.7 REST API on Java 21 LTS with Spring Security 6 (JWT + Buyer Token Filter).
- **Database**: MySQL 8.0 InnoDB with version-controlled Flyway schema migrations (V1 to V7).
- **AI Classification**: Event-driven asynchronous OpenAiCompatible adapter with strict JSON Schema output validation.

---

## 2. Prerequisites

Ensure the following runtimes and tools are installed and available on your system path:

| Software / Tool | Required Version | Verification Command | Notes |
|---|---|---|---|
| **Java Development Kit (JDK)** | **Java 21 LTS** | `java -version` | Java 21 or later required (configured for Spring Boot 3.5.7). |
| **Apache Maven** | **3.9.x+** | `mvn -version` | Required to build and run the backend. |
| **Node.js** | **20.x or 24.x LTS** | `node -version` | Node.js 20+ required for Vite 5. |
| **npm** | **10.x+** | `npm -version` | Bundled with Node.js. |
| **MySQL Database** | **8.0.x** | `mysql --version` | Docker MySQL 8.0 image or standalone local service. |
| **Git** | **2.40+** | `git --version` | Source control. |

---

## 3. Clone Repository

Clone the project from GitHub and navigate into the root directory:

```bash
git clone https://github.com/madhavan1402/threadflow.git
cd threadflow
```

### Repository Structure
```
threadflow/
├── backend/                      # Spring Boot 3.5.7 API (Java 21)
│   ├── src/main/java/com/threadflow/
│   │   ├── auth/                 # JWT Auth, UserDetails, SecurityConfig
│   │   ├── buyer/portal/         # Buyer Token Filter, Safe DTOs, Portal API
│   │   ├── order/                # Orders, TNA Milestones, Variants
│   │   │   ├── audit/            # Append-Only Date Revision Audit Trail
│   │   │   └── warning/          # Merchandiser Confirmed Delay Warnings
│   │   ├── production/           # Floor Updates, Photo Storage, Unit Mappings
│   │   └── issue/                # Tamil NLP Provider, Event Listeners
│   ├── src/main/resources/
│   │   ├── application.yml       # Production/Local configuration
│   │   └── db/migration/         # Flyway Migrations V1__ through V7__
│   └── pom.xml
├── frontend/                     # React 18 + TypeScript + Vite SPA
│   ├── src/
│   │   ├── api/                  # Type-safe Fetch Clients (Orders, Buyer, etc.)
│   │   ├── auth/                 # AuthProvider, Token Storage, Protected Routes
│   │   ├── components/           # Topbar, Sidebar, Dashboard, Table, Modals
│   │   └── styles.css            # Bespoke Manufacturing UI Design System Tokens
│   ├── vite.config.ts
│   └── package.json
├── docker-compose.yml            # Local MySQL 8.0 service
├── .env.example                  # Environment configuration template
└── deployment.md                 # This deployment manual
```

> [!NOTE]
> **Repository vs. Local Working Tree Notice:** The GitHub repository contains commits through Phase 3 (Authentication). The local working tree is ahead of `origin/main` by 9 commits, containing the complete vertical implementations of Phase 4 (Orders & TNA), Phase 5 (Floor Updates & Units), Phase 6 (Delay Intelligence), Phase 7 (Tamil AI NLP), Phase 8 (Buyer Portal & Audit History), and Phase 9 (Premium SaaS UI).

---

## 4. Database Setup

ThreadFlow uses MySQL 8.0 InnoDB with Flyway migrations.

### Option A: Using Docker Compose (Recommended)
From the repository root, start the pre-configured MySQL 8 container:

```powershell
# Windows PowerShell
$env:SPRING_DATASOURCE_USERNAME="threadflow_app"
$env:SPRING_DATASOURCE_PASSWORD="your-strong-db-password"
$env:MYSQL_ROOT_PASSWORD="your-strong-root-password"
docker compose up -d db
```

```bash
# Unix / macOS Bash
export SPRING_DATASOURCE_USERNAME="threadflow_app"
export SPRING_DATASOURCE_PASSWORD="your-strong-db-password"
export MYSQL_ROOT_PASSWORD="your-strong-root-password"
docker compose up -d db
```

### Option B: Using Standalone Local MySQL Service
If running MySQL directly on your host machine:

1. Connect to MySQL as root:
   ```sql
   mysql -u root -p
   ```
2. Create the database and dedicated user:
   ```sql
   CREATE DATABASE threadflow CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
   CREATE USER 'threadflow_app'@'localhost' IDENTIFIED BY 'your-strong-db-password';
   GRANT ALL PRIVILEGES ON threadflow.* TO 'threadflow_app'@'localhost';
   FLUSH PRIVILEGES;
   ```

### Flyway Migration Behavior
When the Spring Boot backend starts, Flyway automatically scans `backend/src/main/resources/db/migration/` and applies pending migrations sequentially:
- `V1__initial_schema.sql`: Foundation baseline.
- `V2__create_auth_tables.sql`: Users, roles, user_roles tables.
- `V3__create_order_management.sql`: Buyers, templates, orders, variants, milestones.
- `V4__production_updates.sql`: Daily production logs and inspection photos.
- `V5__production_unit_supervisor_assignments.sql`: Stitching/printing units and supervisor mappings.
- `V6__production_issue_classification.sql`: Tamil NLP issue records and merchandiser review history.
- `V7__buyer_portal_and_audit_history.sql`: Buyer access tokens, date revision audit log, customer delay warnings.

---

## 5. Backend Configuration & Environment Variables

The backend loads properties from `backend/src/main/resources/application.yml` and overrides them via environment variables.

Copy `.env.example` to create your local environment:
```bash
cp .env.example .env
```

### Required Configuration Variables

| Environment Variable | Description | Example / Recommended Value |
|---|---|---|
| `SPRING_DATASOURCE_URL` | JDBC URL for MySQL connection | `jdbc:mysql://localhost:3306/threadflow?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC` |
| `SPRING_DATASOURCE_USERNAME` | Database username | `threadflow_app` (or `root`) |
| `SPRING_DATASOURCE_PASSWORD` | Database password | *(Your database password)* |
| `THREADFLOW_JWT_SECRET` | 256-bit secret key for signing JWTs | *(Min 32 characters, e.g. `threadflow-production-jwt-signing-key-32-chars`)* |
| `THREADFLOW_JWT_EXPIRATION_MINUTES` | JWT validity duration | `480` (8 hours) |
| `THREADFLOW_COOKIE_SECURE` | HTTPS cookie flag | `true` for prod; `false` for HTTP localhost |
| `THREADFLOW_COOKIE_SAME_SITE` | Cookie SameSite policy | `Lax` (or `Strict`) |
| `SPRING_PROFILES_ACTIVE` | Active Spring profile | `local` (enables sample order seeding) |
| `THREADFLOW_DEMO_MERCHANDISER_EMAIL`| Demo merchandiser account | `merchandiser@local.threadflow` |
| `THREADFLOW_DEMO_MERCHANDISER_PASSWORD`| Demo merchandiser password | *(Min 12 chars, e.g. `<evaluator-merchandiser-password>`)* |
| `THREADFLOW_DEMO_SUPERVISOR_EMAIL` | Demo supervisor account | `supervisor@local.threadflow` |
| `THREADFLOW_DEMO_SUPERVISOR_PASSWORD` | Demo supervisor password | *(Min 12 chars, e.g. `<evaluator-supervisor-password>`)* |
| `THREADFLOW_PRODUCTION_PHOTO_STORAGE_ROOT`| Filesystem directory for floor photos | `${java.io.tmpdir}/threadflow-photos` or `/var/threadflow/photos` |
| `THREADFLOW_AI_BASE_URL` | OpenAI-compatible endpoint | `https://api.openai.com/v1` (or local LLM gateway) |
| `THREADFLOW_AI_API_KEY` | API Key for AI service | *(Your API key or empty for mock mode)* |
| `THREADFLOW_AI_MODEL` | LLM model ID | `gpt-4o-mini` |
| `THREADFLOW_AI_TIMEOUT_MILLIS` | HTTP timeout for NLP inference | `5000` (5 seconds) |

> [!CAUTION]
> Never commit real passwords, JWT secrets, or API keys into version control.

---

## 6. Backend Startup

### Windows PowerShell
```powershell
cd backend

# Set environment variables
$env:SPRING_DATASOURCE_URL="jdbc:mysql://localhost:3306/threadflow?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC"
$env:SPRING_DATASOURCE_USERNAME="threadflow_app"
$env:SPRING_DATASOURCE_PASSWORD="your-strong-db-password"
$env:THREADFLOW_JWT_SECRET="super-secure-production-jwt-signing-key-32-characters"
$env:SPRING_PROFILES_ACTIVE="local"
$env:THREADFLOW_DEMO_MERCHANDISER_EMAIL="merchandiser@local.threadflow"
$env:THREADFLOW_DEMO_MERCHANDISER_PASSWORD="<configure-evaluator-merchandiser-password-min-12chars>"
$env:THREADFLOW_DEMO_SUPERVISOR_EMAIL="supervisor@local.threadflow"
$env:THREADFLOW_DEMO_SUPERVISOR_PASSWORD="<configure-evaluator-supervisor-password-min-12chars>"

# Run with Maven
mvn spring-boot:run
```

### Linux / macOS Bash
```bash
cd backend

export SPRING_DATASOURCE_URL="jdbc:mysql://localhost:3306/threadflow?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC"
export SPRING_DATASOURCE_USERNAME="threadflow_app"
export SPRING_DATASOURCE_PASSWORD="your-strong-db-password"
export THREADFLOW_JWT_SECRET="super-secure-production-jwt-signing-key-32-characters"
export SPRING_PROFILES_ACTIVE="local"
export THREADFLOW_DEMO_MERCHANDISER_EMAIL="merchandiser@local.threadflow"
export THREADFLOW_DEMO_MERCHANDISER_PASSWORD="<configure-evaluator-merchandiser-password-min-12chars>"
export THREADFLOW_DEMO_SUPERVISOR_EMAIL="supervisor@local.threadflow"
export THREADFLOW_DEMO_SUPERVISOR_PASSWORD="<configure-evaluator-supervisor-password-min-12chars>"

mvn spring-boot:run
```

Verify backend health at:
`http://localhost:8080/api/health`

---

## 7. Frontend Setup

In a separate terminal:

```bash
cd frontend

# 1. Install dependencies
npm install

# 2. Configure environment (Optional)
# If backend runs on a different port than 8080, specify VITE_API_BASE_URL:
# export VITE_API_BASE_URL="http://localhost:8080"

# 3. Start development server
npm run dev
```

Open your browser at:
`http://localhost:5173`

### Production Build
To create an optimized production distribution:
```bash
npm run build
```
This runs `tsc -b && vite build`. Output is generated in `frontend/dist/`.

---

## 8. Demo Accounts & Verification Credentials

When running with `SPRING_PROFILES_ACTIVE=local`, ThreadFlow automatically provisions seed accounts and sample orders:

### 1. Merchandiser Workspace Account
- **Email**: `merchandiser@local.threadflow`
- **Configured Password**: Supplied via `$env:THREADFLOW_DEMO_MERCHANDISER_PASSWORD` (must be at least 12 characters, e.g. `<your-evaluator-password>`).
- **Capabilities**: Executive dashboard (`#overview`), Order master (`#orders`), TNA milestone review, Tamil AI classification review (`#issues`), date revision audit modal, and photo approval toggles.

### 2. Supervisor Shop Floor Account
- **Email**: `supervisor@local.threadflow`
- **Configured Password**: Supplied via `$env:THREADFLOW_DEMO_SUPERVISOR_PASSWORD` (must be at least 12 characters, e.g. `<your-evaluator-password>`).
- **Assigned Units**: Stitching Unit 1, Stitching Unit 2, and printing order `C-PO-0488`.
- **Capabilities**: Mobile-first floor view (`/production`), daily output logging, Tamil vernacular issue notes, and inspection photo capture.

### 3. Buyer Portal Tokens (No Password Required)
Access the Buyer Portal directly at `http://localhost:5173/#buyer` or `/buyer`:
- **Preset 1 (Company B, UK)**:
  - Token: `buyer-token-company-b`
  - Scoped Orders: `B-PO-88213` (Navy crew neck), `B-PO-88240` (White polo with confirmed delay alert).
- **Preset 2 (Company C, Germany)**:
  - Token: `buyer-token-company-c`
  - Scoped Orders: `C-PO-0488` (Yellow kids tee).

---

## 9. Production Deployment Guide

For containerized cloud deployment (AWS ECS, Google Cloud Run, Azure App Service, or self-hosted Kubernetes/Docker):

### 1. Backend Containerization
Build the executable JAR:
```bash
cd backend
mvn clean package -DskipTests
```
Run as a lightweight Docker container:
```dockerfile
FROM eclipse-temurin:21-jre-alpine
VOLUME /tmp
ARG JAR_FILE=target/threadflow-api-0.0.1-SNAPSHOT.jar
COPY ${JAR_FILE} app.jar
ENTRYPOINT ["java","-jar","/app.jar"]
```

### 2. Frontend Hosting (Nginx / Cloudflare Pages / AWS S3)
Deploy the contents of `frontend/dist/` to an Nginx web server. Configure reverse proxying for API traffic:
```nginx
server {
    listen 80;
    server_name threadflow.yourdomain.com;
    return 301 https://$host$request_uri;
}

server {
    listen 443 ssl http2;
    server_name threadflow.yourdomain.com;

    ssl_certificate /etc/letsencrypt/live/threadflow.yourdomain.com/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/threadflow.yourdomain.com/privkey.pem;

    root /var/www/threadflow/dist;
    index index.html;

    location / {
        try_files $uri $uri/ /index.html;
    }

    location /api/ {
        proxy_pass http://backend-service:8080/api/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto https;
    }
}
```

### 3. Managed MySQL 8.0 & Security
- Deploy AWS RDS MySQL 8.0 or Google Cloud SQL with automated multi-AZ failover and daily backups.
- Enforce SSL database connections: `jdbc:mysql://...&useSSL=true&requireSSL=true`.
- Set `THREADFLOW_COOKIE_SECURE=true` and `THREADFLOW_COOKIE_SAME_SITE=Lax` to enforce secure HTTPS cookies.
- Restrict CORS origin in production to the specific domain.
- Attach persistent volume storage (AWS EFS, GCP Filestore) for `THREADFLOW_PRODUCTION_PHOTO_STORAGE_ROOT`.

---

## 10. Troubleshooting & Common Issues

| Issue / Error | Root Cause | Solution |
|---|---|---|
| `Access denied for user 'root'@'localhost'` | MySQL password mismatch or missing permissions. | Verify `$env:SPRING_DATASOURCE_USERNAME` and `$env:SPRING_DATASOURCE_PASSWORD`. Ensure user has grant permissions on `threadflow` database. |
| `FlywayException: Validate failed` | Database schema was manually modified outside of Flyway. | Drop the database and allow Flyway to re-apply migrations cleanly from V1 to V7: `DROP DATABASE threadflow; CREATE DATABASE threadflow;`. |
| `Demo account passwords must be at least 12 characters` | Password security rule violated during boot. | Supply passwords with at least 12 characters in `$env:THREADFLOW_DEMO_*_PASSWORD`. |
| `CORS error on API requests` | Frontend URL differs from backend origin. | In development, use the Vite dev server proxy (already configured for port 8080 in `vite.config.ts`). In production, configure Nginx reverse proxy. |
| `AI classification status: AI_UNAVAILABLE` | External OpenAI endpoint unreachable or API key missing. | Expected behavior when `THREADFLOW_AI_API_KEY` is not set. Merchandisers can manually classify issues via the UI (`#issues`). |
| `Buyer portal returns 401 / 403` | Invalid or expired buyer access token. | Use pre-seeded demo tokens: `buyer-token-company-b` or `buyer-token-company-c`. Verify token in database: `SELECT * FROM buyer_access_tokens;`. |
| `Photo upload returns 413 Payload Too Large` | Photo exceeds 10 MB limit. | Ensure photos uploaded from floor are compressed under 10 MB per photo (enforced in `validatePhotos` utility). |
