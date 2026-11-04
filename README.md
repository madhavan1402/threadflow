# ThreadFlow

Production order tracking for knitwear exporters.

## Stack
- Frontend: React + TypeScript + Vite
- Backend: Java 21 + Spring Boot 3.5
- Database: MySQL 8

## Prerequisites
- Java 21
- Node.js 20+
- npm
- Git

## Start MySQL
Create the local `threadflow` database, then configure the datasource with environment variables:
```powershell
$env:SPRING_DATASOURCE_URL="jdbc:mysql://localhost:3306/threadflow?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC"
$env:SPRING_DATASOURCE_USERNAME="threadflow_app"
$env:SPRING_DATASOURCE_PASSWORD="change_me_locally"
```

## Start backend
```powershell
cd backend
.\mvnw.cmd spring-boot:run
```
If the Maven wrapper has not been generated yet, use an installed Maven:
```powershell
mvn spring-boot:run
```
Backend: http://localhost:8080
Health check: http://localhost:8080/api/v1/health

## Start frontend
In a second terminal:
```powershell
cd frontend
npm install
npm run dev
```
Frontend: http://localhost:5173

## Environment
Backend defaults are for local development only. See `.env.example`.
Never commit real credentials or API keys.

## Local demo accounts
Demo users are not public registration accounts. They are provisioned only when the backend
starts with environment variables:

```powershell
$env:THREADFLOW_DEMO_MERCHANDISER_EMAIL="merchandiser@local.threadflow"
$env:THREADFLOW_DEMO_MERCHANDISER_PASSWORD="<local-password-at-least-12-characters>"
$env:THREADFLOW_DEMO_SUPERVISOR_EMAIL="supervisor@local.threadflow"
$env:THREADFLOW_DEMO_SUPERVISOR_PASSWORD="<local-password-at-least-12-characters>"
$env:THREADFLOW_JWT_SECRET="<local-random-secret-at-least-32-characters>"
$env:THREADFLOW_COOKIE_SECURE="false"
```

- `MERCHANDISER`: opens the Orders workspace and has backend access to merchandiser routes only.
- `SUPERVISOR`: opens the Production workspace and has backend access to supervisor routes only.
- Accounts are created or updated by `DemoUserInitializer` at application startup.
- The passwords are stored only as BCrypt hashes and must never be committed.
- For production deployments, use managed credentials, a strong JWT secret, and
  `THREADFLOW_COOKIE_SECURE=true`.

The "Insufficient role" API response means an authenticated user attempted to access a workspace
route restricted to a different role. It is an authorization denial, not an authentication failure.
The frontend separately displays a workspace access-denied screen for role mismatch.

## Current scope
The foundation includes the frontend, Spring Boot/MySQL/Flyway backend, and JWT-cookie authentication
with MERCHANDISER and SUPERVISOR roles. Orders, TNA milestones, production updates, delay projection,
buyer access, AI classification and offline sync remain future milestones.
