# Usage, Documentation, and Examples

## Current Version

- **Frontend:** `1.3.0`
- **Backend:** `1.3.0`
- **Repository release:** `1.3.0` (**minor**) because this release adds backward-compatible reliability improvements, stronger verification workflows, and test-stability fixes without breaking public APIs.

---

## What was implemented in this pass

This release focused on implementation hardening and verification depth:

1. Hardened environment diagnostics with explicit prerequisite checks.
2. Made dependency bootstrap deterministic by using `npm ci`.
3. Improved frontend/backend verification script readability and feedback.
4. Added elapsed-time reporting for the full verification entrypoint.
5. Expanded Makefile discoverability (`help`) and test-only targets.
6. Added stricter frontend verification script options (`verify:strict`).
7. Added targeted frontend test script for session utility logic (`test:session-utils`).
8. Strengthened cookie diagnostics and oversized-cookie warning behavior.
9. Improved retry-delay sanitization and timeout logging for backend health checks.
10. Deduplicated cookie clearing and encoded cookie names for safer invalidation.
11. Expanded frontend unit tests to validate the new resilience behavior.
12. Fixed backend test instability on Java 25+ by switching Mockito mock maker strategy.
13. Updated Maven Surefire runtime args for better modern-JDK behavior.

---

## Updated workflow commands

### 1) Bootstrap dependencies

```bash
./scripts/bootstrap.sh
```

What it does:

- Installs frontend dependencies with `npm ci`.
- Prefetches backend Maven dependencies for offline/test speed.

### 2) Run environment diagnostics

```bash
./scripts/doctor.sh
```

What it checks:

- Repository path
- Node/npm versions
- Java version
- Maven wrapper version
- Presence of required runtime commands

### 3) Verify frontend quality gates

```bash
./scripts/verify-frontend.sh
```

Runs:

- formatting check
- lint
- Svelte type-check
- frontend unit tests
- production build

### 4) Verify backend quality gates

```bash
./scripts/verify-backend.sh
```

Runs:

- backend Maven tests (`./mvnw -B test`)

### 5) Verify entire repository

```bash
./scripts/verify-all.sh
```

Runs:

- doctor
- frontend verification
- backend verification
- reports total execution duration

### 6) Makefile shortcuts

```bash
make help
make bootstrap
make doctor
make verify-frontend
make verify-backend
make test-frontend
make test-backend
make verify
```

### 7) Frontend package scripts added in this release

```bash
cd frontend
npm run test:session-utils
npm run verify:strict
```

---

## Backend test stability note

To eliminate Java 25+ Mockito inline instrumentation failures, backend tests now use:

- `mock-maker-subclass` through `backend/src/test/resources/mockito-extensions/org.mockito.plugins.MockMaker`
- updated Surefire `argLine` including `-XX:+EnableDynamicAgentLoading`

This preserves test behavior while improving modern JDK compatibility.

---

## Usage examples

### Example A: Clean setup and full validation

```bash
./scripts/bootstrap.sh
./scripts/verify-all.sh
```

### Example B: Frontend-only change verification

```bash
cd frontend
npm run verify:full
npm run test:session-utils
```

### Example C: Backend test triage

```bash
./scripts/doctor.sh
./scripts/verify-backend.sh
```

---

## Testing and verification steps executed for this release

```bash
./scripts/doctor.sh
cd frontend && npm run verify
cd frontend && npm run test:session-utils
cd backend && ./mvnw -B test
./scripts/verify-all.sh
```

All commands above were run successfully in this implementation pass.
