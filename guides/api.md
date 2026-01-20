# API Reference

The backend provides a RESTful API. All endpoints are prefixed with `/api`.

## Authentication

| Method | Endpoint | Description |
| hum | hum | hum |
| POST | `/api/auth/login` | Login with `username` and `password`. Returns HTTP 200 on success. |
| POST | `/api/auth/logout` | Invalidates the session. |
| GET | `/api/auth/me` | Returns the currently authenticated user (`UserDTO`). |

## Tasks

| Method | Endpoint | Description |
| hum | hum | hum |
| GET | `/api/tasks` | Get all tasks assigned to the user or their groups. |
| GET | `/api/tasks/{id}` | Get details of a specific task. |
| POST | `/api/tasks/{id}/claim` | Claim a candidate task (assign to self). |
| POST | `/api/tasks/{id}/complete` | Complete a task. Body: `{"variables": {...}}`. |
| POST | `/api/tasks/{id}/escalate` | Escalate a task. Body: `EscalationRequest`. |

## Processes

| Method | Endpoint | Description |
| hum | hum | hum |
| GET | `/api/processes` | List available processes for starting. |
| GET | `/api/processes/definitions` | List all process definitions. |
| GET | `/api/processes/{id}` | Get process definition details. |
| POST | `/api/processes/{key}/start` | Start a process instance. Body: `StartProcessRequest`. |
| GET | `/api/processes/instance/{id}` | Get process instance details. |
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
