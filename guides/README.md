# Application Guides

Welcome to the documentation for the Flowable BPM + SvelteKit application.

## Guides

- [Setup Guide](./setup.md): Instructions for installing and running the application locally and via Docker.
- [Architecture](./architecture.md): Overview of the backend and frontend architecture, including Flowable integration.
- [Workflows](./workflows.md): Detailed explanation of the available BPMN workflows (Expense, Leave, Task).
- [API Reference](./api.md): Key API endpoints and usage.

## Documentation Log

- **2025-01-14**: Added dashboard usability notes covering search, escalated-only focus, and CSV export for reporting workflows.
- **2025-01-15**: Documented dashboard shareable links and filter persistence behavior.
- **2025-01-19**: Added auto-refresh controls for the dashboard and expanded e2e coverage for the refresh toggle.
- **2025-01-20**: Added auto-refresh countdown/paused state and updated UI coverage for refresh timing.
- **2025-01-21**: Added per-page dashboard summaries (average duration, newest/oldest) and expanded UI checks.

## Useful Notes

- Use the dashboard search and sort controls to slice per-page workflow lists before exporting CSV.
- Enable auto-refresh when you want the dashboard to update on a steady cadence while monitoring live queues.
- Auto-refresh pauses when the browser tab is inactive; the countdown resumes when you return.
- Use the per-page summary line to spot average durations and the newest/oldest process start times at a glance.
- Escalation-only toggles help reviewers isolate high-priority work without altering server-side filters.
- Use the dashboard “Copy link” control to share the current filter state with teammates.
