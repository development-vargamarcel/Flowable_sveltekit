# Usage, Documentation, and Examples

## Current Version

- **Frontend:** `1.8.0`
- **Backend:** `1.8.0`
- **Repository release:** `1.8.0` (**minor**) due to backward-compatible reliability and reporting enhancements in automation scripts.

---

## What was implemented in this pass

1. Hardened environment validation for runner toggles and numeric controls.
2. Added canonical path normalization for configurable frontend/backend directories.
3. Standardized deterministic runtime locale/timezone for reproducible logs.
4. Added richer run metadata capture (run ID, UTC start, host, shell).
5. Added optional file logging for automation runs.
6. Added JSON and Markdown summary artifact modes.
7. Expanded step telemetry with index/status/duration/start/end/message.
8. Added reusable retry helper for transient command failures.
9. Added optional timeout integration where available.
10. Expanded precondition and executable checks in shared helpers.
11. Added frontend stage skip controls and optional coverage execution.
12. Added backend stage skip controls and optional `mvn verify` stage.
13. Added robust dependency bootstrap with retry.
14. Expanded diagnostics for OS/kernel/CPU/memory/disk and lockfile consistency.
15. Added optional bootstrap inclusion during full verification.
16. Added Makefile targets for JSON and Markdown report workflows.
17. Added automation smoke tests for parse, dry-run, retry, JSON summary, and Markdown summary.
18. Added clean-working-tree enforcement mode (`BPM_RUNNER_REQUIRE_CLEAN_GIT`).
19. Added lint warning threshold control (`BPM_FRONTEND_LINT_MAX_WARNINGS`).
20. Suppressed Sentry telemetry during scripted production builds for cleaner CI signal.

---

## Dependency and tool installation

Run the bootstrap script to install all required project dependencies:

```bash
./scripts/bootstrap.sh
```

This command performs:

- `npm ci` in `frontend/` with retry support.
- `./mvnw -DskipTests dependency:go-offline` in `backend/` with retry support.

---

## Core verification workflow

### 1) Diagnostics

```bash
./scripts/doctor.sh
```

### 2) Frontend verification

```bash
./scripts/verify-frontend.sh
```

### 3) Backend verification

```bash
./scripts/verify-backend.sh
```

### 4) Full-stack verification

```bash
./scripts/verify-all.sh
```

With bootstrap included:

```bash
BPM_VERIFY_INCLUDE_BOOTSTRAP=1 ./scripts/verify-all.sh
```

---

## Reporting modes

### JSON summary artifact

```bash
BPM_RUNNER_SUMMARY_FORMAT=json BPM_RUNNER_LOG_TO_FILE=1 ./scripts/verify-all.sh
```

### Markdown summary artifact

```bash
BPM_RUNNER_SUMMARY_FORMAT=markdown BPM_RUNNER_LOG_TO_FILE=1 ./scripts/verify-all.sh
```

---

## Clean-git enforcement

Fail fast if the repository has uncommitted changes:

```bash
BPM_RUNNER_REQUIRE_CLEAN_GIT=1 ./scripts/verify-all.sh
```

---

## Makefile shortcuts

```bash
make help
make bootstrap
make doctor
make doctor-json
make doctor-markdown
make verify
make verify-with-bootstrap
make verify-json-report
make verify-markdown-report
make verify-clean-git
make verify-strict
make test-automation
```

---

## Environment controls (copy-ready)

```bash
BPM_RUNNER_LOG_LEVEL=debug
BPM_RUNNER_NO_COLOR=1
BPM_RUNNER_DRY_RUN=0
BPM_RUNNER_SUMMARY=1
BPM_RUNNER_SUMMARY_FORMAT=table
BPM_RUNNER_CONTINUE_ON_ERROR=0
BPM_RUNNER_REQUIRE_CLEAN_GIT=0
BPM_RUNNER_LOG_TO_FILE=1
BPM_RUNNER_ARTIFACTS_DIR=.automation
BPM_RUNNER_TIMEOUT_SECONDS=1200
BPM_RUNNER_RETRY_COUNT=3
BPM_RUNNER_RETRY_DELAY_SECONDS=3
```

Frontend stage controls:

```bash
BPM_FRONTEND_SKIP_FORMAT=0
BPM_FRONTEND_SKIP_LINT=0
BPM_FRONTEND_SKIP_TYPECHECK=0
BPM_FRONTEND_SKIP_TESTS=0
BPM_FRONTEND_SKIP_BUILD=0
BPM_FRONTEND_ENABLE_COVERAGE=1
BPM_FRONTEND_LINT_MAX_WARNINGS=0
```

Backend stage controls:

```bash
BPM_BACKEND_SKIP_TESTS=0
BPM_BACKEND_SKIP_PACKAGE=0
BPM_BACKEND_ENABLE_VERIFY=1
```

---

## Example end-to-end command sets

### Quick local health check

```bash
./scripts/doctor.sh
./scripts/test-automation.sh
./scripts/verify-all.sh
```

### Strict CI profile

```bash
BPM_RUNNER_SUMMARY_FORMAT=json BPM_RUNNER_LOG_TO_FILE=1 BPM_FRONTEND_ENABLE_COVERAGE=1 BPM_BACKEND_ENABLE_VERIFY=1 ./scripts/verify-all.sh
```

### Protected release check (clean tree + markdown artifact)

```bash
BPM_RUNNER_REQUIRE_CLEAN_GIT=1 BPM_RUNNER_SUMMARY_FORMAT=markdown BPM_RUNNER_LOG_TO_FILE=1 ./scripts/verify-all.sh
```
