# Flowable + SvelteKit BPM Demo

A production-ready Business Process Management (BPM) demo application featuring:
- **Backend**: Spring Boot 3.2 + Flowable 7.0 (embedded BPM engine)
- **Frontend**: SvelteKit 2.x + Svelte 5 (runes mode) + Tailwind CSS
- **Deployment**: Docker + Railway

## Features

### 3 Workflow Types
1. **Expense Approval** (Threshold-based)
   - Amounts ≤ $500: Supervisor approval
   - Amounts > $500: Supervisor review → Executive approval

2. **Leave Request** (Sequential)
   - ≤ 5 days: Supervisor approval
   - > 5 days: Supervisor → Executive approval

3. **Task Assignment** (Simple)
   - Create → Assign/Claim → Complete

### 3 User Roles
| Username | Password | Role | Capabilities |
|----------|----------|------|--------------|
| user1 | password | User | Submit requests, work on tasks |
| supervisor1 | password | Supervisor | Approve ≤$500 expenses, first-level leave |
| executive1 | password | Executive | Final approval for high-value items |

## Quick Start

### Option 1: Docker Compose (Recommended)

```bash
# Build and run
docker-compose up --build

# Access the application
# Frontend: http://localhost:3000
# Backend API: http://localhost:8080
```

### Option 2: Manual Development

**Backend (requires Java 17+, Maven):**
```bash
cd backend
./mvnw spring-boot:run
# API available at http://localhost:8080
```

**Frontend (requires Node.js 20+):**
```bash
cd frontend
npm install
npm run dev
# App available at http://localhost:5173
```

## Project Structure

```
├── backend/                 # Spring Boot + Flowable
│   ├── src/main/java/      # Java source code
│   ├── src/main/resources/ # Config + BPMN files
│   └── Dockerfile
├── frontend/               # SvelteKit + Svelte 5
│   ├── src/lib/           # Components, stores, API
│   ├── src/routes/        # Page routes
│   └── Dockerfile
├── docker-compose.yml
└── railway.json           # Railway deployment config
```

## API Endpoints

### Authentication
- `POST /api/auth/login` - Login with username/password
- `POST /api/auth/logout` - Logout
- `GET /api/auth/me` - Get current user

### Tasks
- `GET /api/tasks` - Get all accessible tasks
- `GET /api/tasks/{id}` - Get task details
- `POST /api/tasks/{id}/claim` - Claim a task
- `POST /api/tasks/{id}/complete` - Complete a task

### Processes
- `GET /api/processes` - List available process definitions
- `POST /api/processes/{key}/start` - Start a new process
- `GET /api/processes/my-processes` - Get user's active processes

## Deployment to Railway

1. Push code to GitHub
2. Connect Railway to your repository
3. Railway auto-detects docker-compose.yml
4. Configure environment variables if needed
5. Deploy!

## Technology Stack

| Component | Technology |
|-----------|------------|
| Backend | Java 17, Spring Boot 3.2, Flowable 7.0 |
| Database | H2 (embedded, in-memory) |
| Frontend | SvelteKit 2.x, Svelte 5, Tailwind CSS |
| Build | Maven, Vite, Node.js 20 |
| Container | Docker, Docker Compose |

## Demo Data

The application auto-loads demo data on startup:
- 2 pending expense requests
- 1 pending leave request
- 1 sample task

## License

MIT
