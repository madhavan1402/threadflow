# Local deployment and run instructions

## Requirements
- Java 21
- Maven 3.9+ (or add the Maven Wrapper)
- Node.js 20+ and npm
- Docker Desktop

## 1. Start the database
From the repository root:
```powershell
docker compose up -d db
```

## 2. Run the backend
```powershell
cd backend
mvn spring-boot:run
```
The API runs at `http://localhost:8080`. Verify it at `http://localhost:8080/api/health`.

## 3. Run the frontend
Open a second terminal:
```powershell
cd frontend
npm install
npm run dev
```
Open `http://localhost:5173`.

## Environment variables
- `SPRING_DATASOURCE_URL`
- `SPRING_DATASOURCE_USERNAME`
- `SPRING_DATASOURCE_PASSWORD`

The values in local configuration are for development only. Use deployment-provider secrets for production.

## Demo access
Authentication and demo accounts are not implemented in this foundation milestone. They will be added before reviewer handoff.
