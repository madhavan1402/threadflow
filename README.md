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

## Current scope
This starter establishes the frontend/backend/database connection. Authentication, orders, TNA milestones, production updates, delay projection, buyer access, AI classification and offline sync will be implemented in subsequent milestones.
