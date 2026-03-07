# Setup Guide

This guide covers how to set up and run the application locally.

## Prerequisites

- **Java 17+** (for Backend)
- **Node.js 20+** (for Frontend)
- **Docker & Docker Compose** (for full stack)
- **Maven** (optional, wrapper provided)

## Environment Configuration

1. Copy `.env.example` to `.env`:
   ```bash
   cp .env.example .env
   ```
2. Edit `.env` and set your configuration variables. Important ones include:
   - `SPRING_PROFILES_ACTIVE`
   - `SPRING_DATASOURCE_URL`
   - `SPRING_MAIL_*` (for email notifications)

## Running with Docker Compose (Recommended)

To run the full stack (Backend + Frontend):

```bash
docker-compose up --build
```

- Frontend: [http://localhost:3000](http://localhost:3000)
- Backend: [http://localhost:8080](http://localhost:8080)

## Manual Development

### 1. Backend
Navigate to the `backend` directory:
```bash
cd backend
```

Build and run:
```bash
./mvnw clean install
./mvnw spring-boot:run
```
The backend will start on port 8080.

### 2. Frontend
Navigate to the `frontend` directory:
```bash
cd frontend
```

Install dependencies:
```bash
npm install
```

Run the development server:
```bash
npm run dev
```
The frontend will start on [http://localhost:5173](http://localhost:5173).

## Troubleshooting

- **Database Connection**: Ensure your H2 datasource variables in `.env` match `application.yml` defaults (or override via environment variables).
- **Frontend API**: The frontend proxies requests to `http://localhost:8080` by default. If your backend is on a different port, update `vite.config.ts`.
