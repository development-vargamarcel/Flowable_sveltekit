# Implementation Plan: Flowable + SvelteKit Demo

## Overview
A production-ready demo application featuring Flowable BPM engine with a SvelteKit (Svelte 5 runes) frontend, deployable to Railway via Docker.

---

## Architecture

```
┌─────────────────────────────────────────────────────────┐
│                    Docker Compose                        │
├─────────────────────────────────────────────────────────┤
│  ┌─────────────────────┐    ┌─────────────────────────┐ │
│  │   SvelteKit App     │    │   Spring Boot + Flowable│ │
│  │   (Node.js)         │───▶│   (Java 17)             │ │
│  │   Port: 3000        │    │   Port: 8080            │ │
│  │                     │    │   H2 Database (embedded)│ │
│  └─────────────────────┘    └─────────────────────────┘ │
└─────────────────────────────────────────────────────────┘
```

---

## User Roles (3 Levels)

| Role | Username | Password | Capabilities |
|------|----------|----------|--------------|
| User | user1 | password | Submit expenses, leave requests, work on assigned tasks |
| Supervisor | supervisor1 | password | Approve user requests, escalate to executive, manage team |
| Executive | executive1 | password | Final approval for high-value items, override decisions |

---

## Workflows

### 1. Expense Approval (Threshold-based)
```
User submits expense
       │
       ▼
   Amount > $500?
   ┌────┴────┐
   No       Yes
   │         │
   ▼         ▼
Supervisor  Supervisor
 Approves    Reviews
   │         │
   │    Approves/Escalates
   │         │
   │         ▼
   │    Executive
   │     Approves
   │         │
   └────┬────┘
        ▼
   Process Complete
```

### 2. Leave Request (Sequential)
```
User submits leave request
       │
       ▼
   Supervisor Reviews
       │
   Approve/Reject/Escalate
       │
       ▼ (if escalated or > 5 days)
   Executive Reviews
       │
   Approve/Reject
       │
       ▼
   Process Complete
```

### 3. Task Assignment (Simple)
```
Any user creates task
       │
       ▼
   Assign to user
       │
       ▼
   Assignee works on task
       │
       ▼
   Mark complete
       │
       ▼
   Process Complete
```

---

## Project Structure

```
/Flowable_sveltekit/
├── docker-compose.yml
├── Dockerfile.frontend
├── Dockerfile.backend
├── railway.json
│
├── /backend/                    # Spring Boot + Flowable
│   ├── pom.xml
│   ├── /src/main/java/com/demo/bpm/
│   │   ├── BpmApplication.java
│   │   ├── /config/
│   │   │   ├── SecurityConfig.java
│   │   │   ├── CorsConfig.java
│   │   │   └── FlowableConfig.java
│   │   ├── /controller/
│   │   │   ├── AuthController.java
│   │   │   ├── TaskController.java
│   │   │   └── ProcessController.java
│   │   ├── /service/
│   │   │   ├── TaskService.java
│   │   │   ├── ProcessService.java
│   │   │   └── UserService.java
│   │   ├── /dto/
│   │   │   ├── TaskDTO.java
│   │   │   ├── ProcessDTO.java
│   │   │   ├── LoginRequest.java
│   │   │   └── UserDTO.java
│   │   └── /delegate/
│   │       ├── ExpenseValidationDelegate.java
│   │       └── NotificationDelegate.java
│   └── /src/main/resources/
│       ├── application.yml
│       └── /processes/
│           ├── expense-approval.bpmn20.xml
│           ├── leave-request.bpmn20.xml
│           └── task-assignment.bpmn20.xml
│
├── /frontend/                   # SvelteKit + Svelte 5
│   ├── package.json
│   ├── svelte.config.js
│   ├── vite.config.ts
│   ├── tailwind.config.js
│   ├── /src/
│   │   ├── app.html
│   │   ├── app.css
│   │   ├── /lib/
│   │   │   ├── /api/
│   │   │   │   └── client.ts
│   │   │   ├── /components/
│   │   │   │   ├── TaskCard.svelte
│   │   │   │   ├── TaskList.svelte
│   │   │   │   ├── ProcessCard.svelte
│   │   │   │   ├── DynamicForm.svelte
│   │   │   │   ├── Navbar.svelte
│   │   │   │   └── Toast.svelte
│   │   │   ├── /stores/
│   │   │   │   └── auth.svelte.ts
│   │   │   └── /types/
│   │   │       └── index.ts
│   │   └── /routes/
│   │       ├── +layout.svelte
│   │       ├── +layout.server.ts
│   │       ├── +page.svelte           # Dashboard
│   │       ├── /login/
│   │       │   └── +page.svelte
│   │       ├── /tasks/
│   │       │   ├── +page.svelte       # Task inbox
│   │       │   └── /[id]/
│   │       │       └── +page.svelte   # Task detail
│   │       └── /processes/
│   │           ├── +page.svelte       # Start new process
│   │           ├── /expense/
│   │           │   └── +page.svelte
│   │           ├── /leave/
│   │           │   └── +page.svelte
│   │           └── /task/
│   │               └── +page.svelte
│   └── /static/
│       └── favicon.png
│
└── /docs/
    └── API.md
```

---

## Implementation Steps

### Phase 1: Backend Setup
1. Create Spring Boot project structure
2. Add Flowable dependency and configuration
3. Create BPMN process definitions (3 workflows)
4. Implement REST controllers (Auth, Tasks, Processes)
5. Configure H2 database and security
6. Add service delegates for business logic

### Phase 2: Frontend Setup
7. Create SvelteKit project with Svelte 5
8. Configure Tailwind CSS
9. Create TypeScript types
10. Build API client
11. Create auth store (Svelte 5 runes)

### Phase 3: Frontend Components
12. Build Navbar component
13. Build TaskCard and TaskList components
14. Build DynamicForm component
15. Build ProcessCard component
16. Build Toast notification component

### Phase 4: Frontend Routes
17. Create layout with auth check
18. Build login page
19. Build dashboard page
20. Build task inbox page
21. Build task detail page
22. Build process starter pages (expense, leave, task)

### Phase 5: Docker & Deployment
23. Create Dockerfile for backend
24. Create Dockerfile for frontend
25. Create docker-compose.yml
26. Create railway.json configuration
27. Test local Docker deployment
28. Document deployment steps

---

## Demo Data

Pre-populated on startup:
- 3 users (user1, supervisor1, executive1)
- 2 sample expense requests (one pending supervisor, one pending executive)
- 1 sample leave request (pending approval)
- 2 sample tasks (one assigned, one unassigned)

---

## Tech Stack

| Component | Technology |
|-----------|------------|
| Backend Runtime | Java 17 |
| Backend Framework | Spring Boot 3.2 |
| BPM Engine | Flowable 7.0 |
| Database | H2 (embedded) |
| Frontend Runtime | Node.js 20 |
| Frontend Framework | SvelteKit 2.x |
| UI Library | Svelte 5 (runes mode) |
| CSS Framework | Tailwind CSS 3.x |
| Containerization | Docker |
| Deployment | Railway |

---

## API Endpoints

### Authentication
- `POST /api/auth/login` - Login
- `POST /api/auth/logout` - Logout
- `GET /api/auth/me` - Get current user

### Tasks
- `GET /api/tasks` - Get user's tasks
- `GET /api/tasks/claimable` - Get claimable tasks
- `GET /api/tasks/{id}` - Get task details
- `POST /api/tasks/{id}/complete` - Complete task
- `POST /api/tasks/{id}/claim` - Claim task

### Processes
- `GET /api/processes` - List available processes
- `POST /api/processes/expense/start` - Start expense process
- `POST /api/processes/leave/start` - Start leave process
- `POST /api/processes/task/start` - Start task assignment
- `GET /api/processes/{id}` - Get process status

---

## Estimated File Count
- Backend: ~20 files
- Frontend: ~25 files
- Config/Docker: ~5 files
- **Total: ~50 files**

---

## Questions Resolved
- ✅ 3 workflows: Expense, Leave, Task
- ✅ H2 database (in-memory)
- ✅ 3 roles: User, Supervisor, Executive
- ✅ Mixed approval: Threshold for expense, Sequential for leave
- ✅ Railway deployment
- ✅ Standard UI with Tailwind

---

## Ready to Implement?

Please review this plan and confirm:
1. Does the workflow logic look correct?
2. Is the project structure acceptable?
3. Any features to add or remove?
