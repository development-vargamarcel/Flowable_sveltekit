# Architecture Overview

## Backend (Spring Boot + Flowable)

The backend is built with Spring Boot 3.2 and integrates Flowable 7.0 as an embedded BPM engine.

### Key Components

- **Controllers (`com.demo.bpm.controller`)**: Handle HTTP requests.
- **Services (`com.demo.bpm.service`)**: Contain business logic.
  - `WorkflowService`: Manages task execution, escalation, and variables.
  - `WorkflowHistoryService`: Reads historical data (audit trail).
  - `HistoryRecorder`: A helper service that explicitly records actions (approvals, escalations) into process variables for persistence.
- **DTOs (`com.demo.bpm.dto`)**: Data Transfer Objects for API requests/responses.
- **Flowable Engine**:
  - **RepositoryService**: Manages process definitions (`.bpmn20.xml`).
  - **RuntimeService**: Manages running process instances.
  - **TaskService**: Manages user tasks.
  - **HistoryService**: Queries historical activities.

### Data Flow
1. API Request -> Controller
2. Controller -> Service (e.g., `WorkflowService`)
3. Service -> Flowable API (start process, complete task)
4. Flowable Engine executes BPMN logic.
5. Service uses `HistoryRecorder` to append action details to `escalationHistory` or `approvals` variables.

## Frontend (SvelteKit)

The frontend uses SvelteKit 2 with Svelte 5 (Runes).

### Key Components
- **Routes (`src/routes`)**: File-based routing.
- **Components (`src/lib/components`)**: Reusable UI components.
  - `Modal.svelte`: Base for dialogs.
  - `ProcessDetailsModal.svelte`: Visualizes the process history.
- **Stores/State**: Uses Svelte 5 `$state` runes for reactivity.
- **API Client (`src/lib/api/client.ts`)**: Centralized Axios-wrapper for backend communication.
- **Session Utils (`src/lib/utils/session-utils.ts`)**: Handles session cookies and backend health checks.

## Flowable Integration

Flowable is embedded, meaning it shares the same database transaction as the business logic.
- **Process Definitions**: Located in `backend/src/main/resources/processes/`.
- **Database**: Tables starting with `ACT_` are Flowable internal tables.
