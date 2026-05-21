# Verification entrypoints map

## Canonical command path

Use `./scripts/verify-all.sh` as the **single top-level entrypoint** for contributor verification.

Default behavior (`./scripts/verify-all.sh`) runs full-stack checks. Specialized checks are routed through explicit subcommands:

- `./scripts/verify-all.sh frontend`
- `./scripts/verify-all.sh backend`
- `./scripts/verify-all.sh doctor`
- `./scripts/verify-all.sh automation`
- `./scripts/verify-all.sh bootstrap`

## Entrypoint inventory

| Entrypoint | Purpose | Scope | Owner | Overlap |
| --- | --- | --- | --- | --- |
| `scripts/verify-all.sh` | Canonical orchestrator for repository verification flows and subcommands. | Full stack by default; can target frontend/backend/doctor/automation/bootstrap. | Platform / CI maintainers. | Calls `verify-frontend.sh`, `verify-backend.sh`, `doctor.sh`, and `test-automation.sh`. |
| `scripts/verify-frontend.sh` | Runs frontend quality gates (format, lint, typecheck, tests, build; optional coverage/browser smoke). | Frontend app (`frontend/`) quality and build validation. | Frontend maintainers. | Includes browser smoke via `npm run test:browser-console` which executes `frontend/scripts/browser-console-smoke.mjs`. |
| `scripts/verify-backend.sh` | Runs backend Maven tests, package, optional verify. | Backend app (`backend/`) validation and packaging sanity. | Backend maintainers. | Overlaps with full-stack orchestration when called by `verify-all.sh`. |
| `scripts/test-automation.sh` | Smoke-tests automation helpers (runner behavior, summary rendering, guard rails). | Shell automation scripts under `scripts/`. | Platform / DevEx maintainers. | Partially overlaps `verify-all.sh`/`doctor.sh` behavior because it validates those scripts indirectly. |
| `frontend/scripts/browser-console-smoke.mjs` | Playwright smoke that fails on runtime console/page errors during preview launch. | Browser-runtime sanity of frontend bundle. | Frontend maintainers. | Triggered optionally from `verify-frontend.sh`; not a standalone contributor default. |
| `frontend/verification/verify_changes.py` | Legacy end-to-end manual verification script with screenshots. | Legacy local UI flow checks against running app. | Legacy/Unassigned (deprecated). | Overlaps with modern Playwright/browser smoke checks and frontend verification goals. |

## Deprecation status

- `scripts/test-automation.sh` is marked as deprecated for direct use; run through `./scripts/verify-all.sh automation`.
- `frontend/verification/verify_changes.py` is marked as deprecated and should be removed after remaining migration dependencies are eliminated.

## Contributor guidance

Use these defaults:

- **Primary:** `./scripts/verify-all.sh`
- **Focused:** `./scripts/verify-all.sh frontend` or `./scripts/verify-all.sh backend`
- **Automation runner smoke checks:** `./scripts/verify-all.sh automation`

If using `make`, `make verify` remains the default and maps to `./scripts/verify-all.sh`.
