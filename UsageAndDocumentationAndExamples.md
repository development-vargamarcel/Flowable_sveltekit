# Usage, Documentation, and Examples

## Current Version

- **Frontend:** `1.6.0`
- **Backend:** `1.6.0`
- **Repository release:** `1.6.0` (**minor**) because this release introduces backward-compatible automation diagnostics, reporting, and verification controls.

---

## What was implemented in this pass

1. Added strict validation for automation environment variables to prevent invalid runner configuration.
2. Added canonical path normalization for overridden frontend/backend paths.
3. Added deterministic locale/timezone exports for reproducible logs in CI.
4. Added structured run metadata (`run id`, `start time`, `host`, `shell`).
5. Added optional persistent run log files under `.automation/`.
6. Added summary format control with `table` and `json` options.
7. Added JSON-safe summary generation helpers for machine-consumable reporting.
8. Added step indexing and richer status model with `PASS`, `FAIL`, and `SKIP`.
9. Added reusable retry helper for network-sensitive commands.
10. Added optional timeout support using `timeout` where available.
11. Added executable-file precondition checks in shared script utilities.
12. Added frontend stage skip controls (`format`, `lint`, `typecheck`, `tests`, `build`).
13. Added optional frontend coverage execution in `verify-frontend.sh`.
14. Added backend stage skip controls and optional `mvn verify` stage.
15. Hardened `bootstrap.sh` using retry-aware dependency installation.
16. Expanded `doctor.sh` with system diagnostics (OS/kernel/CPU/memory/disk).
17. Expanded `doctor.sh` with automation script executability checks.
18. Added lockfile consistency verification via `npm ls --package-lock-only`.
19. Expanded `verify-all.sh` with optional bootstrap inclusion and artifact-aware logging.
20. Added Makefile report-centric targets (`doctor-json`, `verify-json-report`, `verify-with-bootstrap`).
21. Expanded `scripts/test-automation.sh` with JSON summary and retry helper smoke tests.
22. Updated all release docs/version metadata for semantic version `1.6.0`.

---

## Updated workflow commands

### 1) Bootstrap dependencies

```bash
./scripts/bootstrap.sh
```

What it does:

- Installs frontend dependencies with retryable `npm ci`.
- Resolves backend Maven dependencies with retryable `dependency:go-offline`.
- Prints step timing and a final summary.

### 2) Run environment diagnostics

```bash
./scripts/doctor.sh
```

What it checks:

- Required command/tool availability
- Repository metadata and toolchain versions
- Script executability and lockfiles
- Lockfile consistency (`npm ls --package-lock-only`)
- Host/runtime diagnostics (OS/kernel/CPU/memory/disk)

### 3) Verify frontend quality gates

```bash
./scripts/verify-frontend.sh
```

Default stages:

- format check
- lint
- Svelte type-check
- unit tests
- production build

Optional:

```bash
BPM_FRONTEND_ENABLE_COVERAGE=1 ./scripts/verify-frontend.sh
```

### 4) Verify backend quality gates

```bash
./scripts/verify-backend.sh
```

Default stages:

- Java runtime configuration
- Maven test suite
- packaging sanity build

Optional:

```bash
BPM_BACKEND_ENABLE_VERIFY=1 ./scripts/verify-backend.sh
```

### 5) Verify everything end-to-end

```bash
./scripts/verify-all.sh
```

Optional bootstrap-inclusive run:

```bash
BPM_VERIFY_INCLUDE_BOOTSTRAP=1 ./scripts/verify-all.sh
```

### 6) Automation smoke tests

```bash
./scripts/test-automation.sh
```

Runs:

- bash parse checks
- dry-run orchestration check
- JSON summary smoke test
- retry helper smoke test

### 7) Makefile shortcuts

```bash
make help
make bootstrap
make doctor
make doctor-json
make verify
make verify-with-bootstrap
make verify-json-report
make verify-strict
make test-automation
```

---

## Environment-driven runner controls

```bash
BPM_RUNNER_LOG_LEVEL=debug
BPM_RUNNER_NO_COLOR=1
BPM_RUNNER_DRY_RUN=1
BPM_RUNNER_SUMMARY=1
BPM_RUNNER_SUMMARY_FORMAT=json
BPM_RUNNER_CONTINUE_ON_ERROR=1
BPM_RUNNER_LOG_TO_FILE=1
BPM_RUNNER_ARTIFACTS_DIR=.automation
BPM_RUNNER_TIMEOUT_SECONDS=1200
BPM_RUNNER_RETRY_COUNT=3
BPM_RUNNER_RETRY_DELAY_SECONDS=3
BPM_FRONTEND_DIR=/custom/frontend
BPM_BACKEND_DIR=/custom/backend
```

Stage controls:

```bash
BPM_FRONTEND_SKIP_FORMAT=1
BPM_FRONTEND_SKIP_LINT=1
BPM_FRONTEND_SKIP_TYPECHECK=1
BPM_FRONTEND_SKIP_TESTS=1
BPM_FRONTEND_SKIP_BUILD=1
BPM_FRONTEND_ENABLE_COVERAGE=1
BPM_BACKEND_SKIP_TESTS=1
BPM_BACKEND_SKIP_PACKAGE=1
BPM_BACKEND_ENABLE_VERIFY=1
```

---

## Testing and validation examples

### Standard local verification

```bash
./scripts/test-automation.sh
./scripts/doctor.sh
./scripts/verify-all.sh
```

### CI-style verification with machine-readable artifacts

```bash
BPM_RUNNER_SUMMARY_FORMAT=json BPM_RUNNER_LOG_TO_FILE=1 ./scripts/verify-all.sh
```

### Strict verification

```bash
make verify-strict
```

---

## Semantic versioning update

- Previous version: `1.5.0`
- Current version: `1.6.0`
- Type: **minor**
- Rationale: backward-compatible enhancement of automation diagnostics/reporting and verification flexibility.
