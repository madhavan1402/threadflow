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
$env:SPRING_DATASOURCE_PASSWORD="<local-database-password>"
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
starts with the `local` Spring profile and matching environment variables:

```powershell
$env:SPRING_PROFILES_ACTIVE="local"
$env:THREADFLOW_DEMO_MERCHANDISER_EMAIL="merchandiser@local.threadflow"
$env:THREADFLOW_DEMO_MERCHANDISER_PASSWORD="<local-password-at-least-12-characters>"
$env:THREADFLOW_DEMO_SUPERVISOR_EMAIL="supervisor@local.threadflow"
$env:THREADFLOW_DEMO_SUPERVISOR_PASSWORD="<local-password-at-least-12-characters>"
$env:THREADFLOW_JWT_SECRET="<local-random-secret-at-least-32-characters>"
$env:THREADFLOW_COOKIE_SECURE="false"
```

- `MERCHANDISER`: opens the Orders workspace and has backend access to merchandiser routes only.
- `SUPERVISOR`: opens the Production workspace and has backend access to supervisor routes only.
- With `SPRING_PROFILES_ACTIVE=local`, accounts are created if absent or updated if present by
  `DemoUserInitializer` at application startup. BCrypt passwords replace the stored `password_hash`
  on each local startup; no hash is read, printed, or committed.
- Without both email and password variables, each corresponding account is left unchanged.
- The initializer is excluded outside the `local` profile and by the Spring `test` profile when tests run.
- The passwords are stored only as BCrypt hashes and must never be committed.
- For production deployments, use managed credentials, a strong JWT secret, and
  `THREADFLOW_COOKIE_SECURE=true`.

### Log in through the API
Start the backend after setting the variables above. In a second PowerShell session:

```powershell
$session = New-Object Microsoft.PowerShell.Commands.WebRequestSession
$csrf = Invoke-RestMethod -Uri "http://localhost:8080/api/v1/auth/csrf" -WebSession $session
$body = @{ email = $env:THREADFLOW_DEMO_MERCHANDISER_EMAIL; password = $env:THREADFLOW_DEMO_MERCHANDISER_PASSWORD } |
  ConvertTo-Json
$login = Invoke-RestMethod `
  -Uri "http://localhost:8080/api/v1/auth/login" `
  -Method Post `
  -WebSession $session `
  -Headers @{ $csrf.headerName = $csrf.token } `
  -ContentType "application/json" `
  -Body $body
$login

# For the supervisor account, use THREADFLOW_DEMO_SUPERVISOR_EMAIL and
# THREADFLOW_DEMO_SUPERVISOR_PASSWORD for the email and password values above.
```

The "Insufficient role" API response means an authenticated user attempted to access a workspace
route restricted to a different role. It is an authorization denial, not an authentication failure.
The frontend separately displays a workspace access-denied screen for role mismatch.

The frontend login flow uses `/api/v1/auth/csrf` plus the `X-XSRF-TOKEN` header, keeps the JWT in an
HttpOnly cookie, and redirects successful authentication to `/merchandiser` or `/supervisor` based
only on the role returned by the backend.

## Current scope
The foundation includes the frontend, Spring Boot/MySQL/Flyway backend, and JWT-cookie authentication
with MERCHANDISER and SUPERVISOR roles. Orders, TNA milestones, production updates, delay projection,
buyer access, AI classification and offline sync remain future milestones.
