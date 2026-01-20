# Flowable + SvelteKit BPM Demo

A production-ready Business Process Management (BPM) demo application featuring:
- **Backend**: Spring Boot 3.2 + Flowable 7.0 (embedded BPM engine)
- **Frontend**: SvelteKit 2.x + Svelte 5 (runes mode) + Tailwind CSS
- **Deployment**: Docker + Railway

## Documentation

Detailed documentation is available in the `guides/` directory:
- [Setup Guide](guides/setup.md)
- [Architecture](guides/architecture.md)
- [Workflows](guides/workflows.md)
- [API Reference](guides/api.md)
- [Frontend Components](guides/frontend-components.md)

## Features

### 5 Workflow Types
1. **Expense Approval** (Threshold-based)
2. **Leave Request** (Sequential)
3. **Task Assignment** (Simple)
4. **Purchase Request** (Multi-level approval)
5. **Project Approval** (Parallel review)

See [Workflows Guide](guides/workflows.md) for detailed diagrams and logic.

### Key Implementation Details
- **Workflow Service**: Centralized logic for task completion, escalation, and history tracking.
- **History Recording**: `HistoryRecorder` service encapsulates audit trail creation for escalations, handoffs, and approvals.
- **Workflow Constants**: Standardized variable names (e.g., `currentLevel`, `escalationHistory`) are maintained in `WorkflowConstants.java`.
- **Error Handling**: Custom exceptions (`InvalidOperationException`) map to HTTP 400 Bad Request for better frontend error reporting.
- **Logging**: Enhanced SLF4J logging includes `ProcessInstanceId` context for easier troubleshooting.
- **Frontend State**: Svelte 5 runes (`$state`) are used for reactive state management.
- **Session Management**: Robust cookie handling and backend health checks in `session-utils.ts` to prevent login issues.

### 3 User Roles
| Username | Password | Role | Capabilities |
|----------|----------|------|--------------|
| user1 | password | User | Submit requests, work on tasks |
| supervisor1 | password | Supervisor | Approve ≤$500 expenses, first-level leave |
| executive1 | password | Executive | Final approval for high-value items |
| admin | admin | Admin | Full access |

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
./mvnw clean install
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

### Environment Variables

The application uses an `.env` file for configuration. Copy `.env.example` to `.env` and adjust as needed.

**Key Variables:**
- `POSTGRES_DB`: Database name (default: `flowable`)
- `POSTGRES_USER`: Database user (default: `postgres`)
- `POSTGRES_PASSWORD`: Database password
- `SPRING_MAIL_HOST`: SMTP host for emails
- `SPRING_MAIL_USERNAME`: SMTP username
- `SPRING_MAIL_PASSWORD`: SMTP password

### Testing

**Backend Tests:**
```bash
cd backend
./mvnw test
```

**Frontend Verification:**
```bash
cd frontend
npm run check
npm run test:unit
```

## Project Structure

```
├── backend/                 # Spring Boot + Flowable
│   ├── src/main/java/      # Java source code
│   │   ├── service/       # Business logic
│   │   ├── service/helpers # Shared helper services (HistoryRecorder, etc.)
│   │   └── util/          # Constants and utilities
│   ├── src/main/resources/ # Config + BPMN files
│   └── Dockerfile
├── frontend/               # SvelteKit + Svelte 5
│   ├── src/lib/           # Components, stores, API
│   │   └── utils/         # Utility functions (session-utils.ts)
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
- `POST /api/tasks/{id}/escalate` - Escalate a task

### Processes
- `GET /api/processes` - List available process definitions
- `POST /api/processes/{key}/start` - Start a new process
- `GET /api/processes/my-processes` - Get user's active processes

### Dashboard
- `GET /api/dashboard` - Get dashboard statistics (optimized)

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
| Database | PostgreSQL |
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
