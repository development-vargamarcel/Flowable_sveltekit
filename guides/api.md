# API Reference

The backend provides a RESTful API. All endpoints are prefixed with `/api`.

## Frontend API Client Usage

The Svelte frontend uses `fetchApi` from `frontend/src/lib/api/core.ts` for consistent request behavior (credentials, typed responses, retries, and standardized `ApiError` handling).

### Common options

- `responseType`: `'json'` (default), `'blob'`, or `'text'`.
- `timeoutMs`: optional timeout in milliseconds for slow network protection.

### Example

```ts
import { fetchApi } from "$lib/api/client";

const report = await fetchApi<string>("/api/processes/instance/123/export", {
  responseType: "text",
  timeoutMs: 8000,
});
```

### Timeout behavior

- When `timeoutMs` is omitted, existing behavior is preserved (no forced timeout).
- When a timeout is reached, the request is aborted and an `ApiError` is thrown with:
  - `message`: `"Request timeout"`
  - `status`: `408`
  - `statusText`: `"Request Timeout"`

This makes slow-network failures explicit and easier to diagnose.

## Authentication

| Method | Endpoint | Description |
| hum | hum | hum |
| POST | `/api/auth/login` | Login with `username` and `password`. Returns HTTP 200 on success. |
| POST | `/api/auth/logout` | Invalidates the session. |
| GET | `/api/auth/me` | Returns the currently authenticated user (`UserDTO`). |

## Users

| Method | Endpoint             | Description                                                |
| :----- | :------------------- | :--------------------------------------------------------- |
| GET    | `/api/users/{id}`    | Get user details by ID.                                    |
| PUT    | `/api/users/profile` | Update current user profile. Body: `UpdateProfileRequest`. |

## Tasks

| Method | Endpoint | Description |
| hum | hum | hum |
| GET | `/api/tasks` | Get tasks with optional filtering. Params: `text` (name), `assignee` (username or 'unassigned'), `priority` (number). |
| GET | `/api/tasks/{id}` | Get details of a specific task. |
| POST | `/api/tasks/{id}/claim` | Claim a candidate task (assign to self). |
| POST | `/api/tasks/{id}/unclaim` | Unclaim a task (remove assignment). |
| POST | `/api/tasks/{id}/complete` | Complete a task. Body: `{"variables": {...}}`. |
| POST | `/api/tasks/{id}/escalate` | Escalate a task. Body: `EscalationRequest`. |
| POST | `/api/tasks/{id}/delegate` | Delegate (reassign) a task to another user. Body: `{"targetUserId": "..."}`. |

## Processes

| Method | Endpoint | Description |
| hum | hum | hum |
| GET | `/api/processes` | List available processes for starting. |
| GET | `/api/processes/definitions` | List all process definitions. |
| GET | `/api/processes/{id}` | Get process definition details. |
| POST | `/api/processes/{key}/start` | Start a process instance. Body: `StartProcessRequest`. |
| GET | `/api/processes/instance/{id}` | Get process instance details. |
| DELETE | `/api/processes/instance/{id}` | Cancel (delete) a process instance. Params: `reason`. |
| GET | `/api/processes/instance/{id}/export` | Export process instance details to CSV. |
| GET | `/api/processes/my-processes` | List processes started by the current user. |
| POST | `/api/processes/deploy` | Deploy a new process (BPMN XML). |

## Dashboard

| Method | Endpoint | Description |
| hum | hum | hum |
| GET | `/api/dashboard` | Returns `DashboardDTO` with statistics (active tasks, completed processes, etc.). |

## SLA

| Method | Endpoint | Description |
| hum | hum | hum |
| GET | `/api/slas/stats` | Returns SLA statistics (`SlaStatsDTO`). |

## Database

| Method | Endpoint | Description |
| hum | hum | hum |
| POST | `/api/database/seed` | Seed the database with initial data (users, groups, document types). |

## Documents

| Method | Endpoint | Description |
| hum | hum | hum |
| GET | `/api/document-types` | List all available document types. |
