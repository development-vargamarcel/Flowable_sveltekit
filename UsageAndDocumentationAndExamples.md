# Usage, Documentation, and Examples

## Current Version

- **Frontend:** `1.5.0`
- **Backend:** `1.5.0`
- **Repository release:** `1.5.0` (**minor**) because this release introduces backward-compatible automation runner improvements, deeper staged verification, and enhanced operational documentation.

---

## What was implemented in this pass

This release focused on reliability hardening, diagnosability, and workflow ergonomics for local development and CI:

1. Added runner configuration via environment variables for log level, no-color output, dry-run mode, summary output, and continue-on-error behavior.
2. Added configurable frontend and backend directory roots for non-standard workspace layouts.
3. Added leveled logging (`debug/info/warn/error`) with color-aware fallback behavior.
4. Added reusable file and directory precondition guards.
5. Added a reusable command runner with dry-run support.
6. Added step result collectors for status, duration, and contextual message data.
7. Added `run_step` stage timing wrapper for all key scripts.
8. Added execution summary reporting to each script run.
9. Added optional continue-on-error mode for comprehensive troubleshooting runs.
10. Added shared shell error trap utilities for line-level failure diagnostics.
11. Refactored `doctor.sh` to run staged validations and runtime guidance output.
12. Centralized lockfile and metadata checks via shared validators.
13. Refactored `bootstrap.sh` into staged frontend and backend dependency setup.
14. Refactored `verify-frontend.sh` into explicit quality-gate stages.
15. Refactored `verify-backend.sh` into staged runtime config/test/package checks.
16. Added backend packaging sanity check (`mvn package -DskipTests`) during verification.
17. Refactored `verify-all.sh` orchestration into summarized staged execution.
18. Added Makefile helpers for verbose doctor output, no-color diagnostics, fast verification, dry-run verification, and continue-on-error verification.
19. Added `scripts/test-automation.sh` smoke tests for script syntax, dry-run orchestration, and summary behavior.
20. Updated semantic version metadata and release documentation artifacts.

---

## Updated workflow commands

### 1) Bootstrap dependencies

```bash
./scripts/bootstrap.sh
```

What it does:

- Installs frontend dependencies with `npm ci` using sanitized npm environment variables.
- Resolves backend Maven dependencies for offline and faster repeated runs.
- Prints per-step timing and a final summary.

### 2) Run environment diagnostics

```bash
./scripts/doctor.sh
```

What it checks:

- Required commands (`git`, `node`, `npm`, `java`, backend Maven wrapper)
- Required metadata (`backend/pom.xml`, both lockfiles)
- Git branch and short commit
- Node/npm/Java/Maven wrapper versions
- Preferred Java runtime guidance

### 3) Verify frontend quality gates

```bash
./scripts/verify-frontend.sh
```

Runs in order:

- format check
- lint
- Svelte type-check
- unit tests
- production build

### 4) Verify backend quality gates

```bash
./scripts/verify-backend.sh
```

Runs in order:

- Java runtime configuration check
- Maven test suite
- package sanity build (`-DskipTests`)

### 5) Verify everything end-to-end

```bash
./scripts/verify-all.sh
```

Runs:

- doctor
- frontend verification
- backend verification
- consolidated step summaries

### 6) Automation smoke tests

```bash
./scripts/test-automation.sh
```

Runs:

- bash parse checks for all scripts
- dry-run orchestration check
- summary output smoke check

### 7) Makefile shortcuts

```bash
make help
make bootstrap
make doctor
make doctor-verbose
make doctor-no-color
make check-lockfiles
make verify-frontend
make verify-backend
make verify-fast
make verify-dry-run
make verify-continue
make test-automation
make test-frontend
make test-backend
make verify-strict
make verify
make clean
```

---

## Environment-driven runner controls

All script commands support these optional controls:

```bash
BPM_RUNNER_LOG_LEVEL=debug    # debug|info|warn|error
BPM_RUNNER_NO_COLOR=1         # disable ANSI color output
BPM_RUNNER_DRY_RUN=1          # print commands only
BPM_RUNNER_SUMMARY=0          # suppress summary table
BPM_RUNNER_CONTINUE_ON_ERROR=1 # continue staging despite failures
BPM_FRONTEND_DIR=/custom/frontend
BPM_BACKEND_DIR=/custom/backend
```

Example:

```bash
BPM_RUNNER_LOG_LEVEL=debug BPM_RUNNER_NO_COLOR=1 ./scripts/doctor.sh
```

---

## Testing and validation examples

### Quick confidence pass

```bash
./scripts/test-automation.sh
./scripts/doctor.sh
```

### Full CI-like pass

```bash
./scripts/bootstrap.sh
./scripts/verify-all.sh
```

### Strict verification with frontend coverage

```bash
make verify-strict
```

### Troubleshooting pass without hard-stop

```bash
BPM_RUNNER_CONTINUE_ON_ERROR=1 ./scripts/verify-all.sh
```

---

## Semantic versioning update

- Previous version: `1.4.0`
- Current version: `1.5.0`
- Type: **minor**
- Rationale: backward-compatible feature additions to tooling and verification workflows.
