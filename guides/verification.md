# Verification entrypoints map

## Canonical command path

Use `./scripts/verify-all.sh` as the **single top-level entrypoint** for contributor verification.

Default behavior (`./scripts/verify-all.sh`) runs full-stack checks. Specialized checks are routed through explicit subcommands:

- `./scripts/verify-all.sh frontend`
- `./scripts/verify-all.sh backend`
- `./scripts/verify-all.sh doctor`
- `./scripts/verify-all.sh automation`
- `./scripts/verify-all.sh bootstrap`

## Cleanup-safe verification matrix

Use the smallest command that covers the cleanup scope, then run the full command for cross-cutting changes:

| Cleanup scope                  | Command                              |
| ------------------------------ | ------------------------------------ |
| Documentation-only updates     | `./scripts/verify-all.sh doctor`     |
| Frontend source/config updates | `./scripts/verify-all.sh frontend`   |
| Backend source/config updates  | `./scripts/verify-all.sh backend`    |
| Automation/script updates      | `./scripts/verify-all.sh automation` |
| Cross-cutting cleanup          | `./scripts/verify-all.sh`            |

## Entrypoint inventory

| Entrypoint                   | Purpose                                                                                               | Scope                                                                           | Owner                         | Overlap                                                                                                                |
| ---------------------------- | ----------------------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------- | ----------------------------- | ---------------------------------------------------------------------------------------------------------------------- |
| `scripts/verify-all.sh`      | Canonical orchestrator for repository verification flows and subcommands.                             | Full stack by default; can target frontend/backend/doctor/automation/bootstrap. | Platform / CI maintainers.    | Calls `verify-frontend.sh`, `verify-backend.sh`, `doctor.sh`, and `test-automation.sh`.                                |
| `scripts/verify-frontend.sh` | Runs frontend quality gates (format, lint, typecheck, tests, build; optional coverage/browser smoke). | Frontend app (`frontend/`) quality and build validation.                        | Frontend maintainers.         | Includes browser smoke via `npm run test:browser-console` which executes `frontend/scripts/browser-console-smoke.mjs`. |
| `scripts/verify-backend.sh`  | Runs backend Maven tests, package, optional verify.                                                   | Backend app (`backend/`) validation and packaging sanity.                       | Backend maintainers.          | Overlaps with full-stack orchestration when called by `verify-all.sh`.                                                 |
| `scripts/test-automation.sh` | Smoke-tests automation helpers (runner behavior, summary rendering, guard rails).                     | Shell automation scripts under `scripts/`.                                      | Platform / DevEx maintainers. | Routed through `verify-all.sh automation`; direct use is deprecated.                                                   |

## Deprecation status

- `scripts/test-automation.sh` is deprecated for direct use; run through `./scripts/verify-all.sh automation`.
- The legacy `frontend/verification/verify_changes.py` Playwright helper and its stored screenshots were removed during cleanup because browser/runtime validation is now covered by the canonical frontend verification flow and optional browser console smoke check.

## Contributor guidance

Use these defaults:

- **Primary:** `./scripts/verify-all.sh`
- **Focused:** `./scripts/verify-all.sh frontend` or `./scripts/verify-all.sh backend`
- **Automation runner smoke checks:** `./scripts/verify-all.sh automation`

If using `make`, `make verify` remains the default and maps to `./scripts/verify-all.sh`.
