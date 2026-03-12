# Usage, Documentation, and Examples (Release 1.8.3)

## Version alignment
- **Frontend:** `1.8.3`
- **Backend:** `1.8.3`
- **Repository release:** `1.8.3` (**patch**) for frontend robustness, safer typing, and stronger browser/runtime verification automation.

---

## Setup instructions

### Prerequisites
- Node.js 20+
- npm 10+
- Java 17+ (Java 21 preferred by verification scripts)

### One-time bootstrap
```bash
./scripts/bootstrap.sh
```

This installs frontend dependencies (`npm ci`) and warms backend Maven dependencies for offline usage.

### Manual setup (alternative)
```bash
cd frontend && npm ci
cd ../backend && ./mvnw -B -q -DskipTests dependency:go-offline
```

---

## Usage instructions

### Start local development
```bash
# terminal 1
cd backend && ./mvnw spring-boot:run

# terminal 2
cd frontend && npm run dev
```

### Access key routes
- Dashboard: `/dashboard`
- Process instances: `/process-instances`
- Document type list: `/document-definitions/types`
- Document type designer: `/document-definitions/types/designer`

---

## Testing and validation steps

### Core full-stack validation
```bash
./scripts/verify-all.sh
```

### Frontend-only validation
```bash
./scripts/verify-frontend.sh
```

### Backend-only validation
```bash
./scripts/verify-backend.sh
```

### Strict validation profile
```bash
BPM_FRONTEND_ENABLE_COVERAGE=1 BPM_BACKEND_ENABLE_VERIFY=1 ./scripts/verify-all.sh
```

### Browser console smoke validation (new)
```bash
BPM_FRONTEND_ENABLE_BROWSER_SMOKE=1 ./scripts/verify-frontend.sh
```

### Automation script smoke tests
```bash
./scripts/test-automation.sh
```

---

## Important implementation notes

1. Route-level frontend error handling now centralizes unknown error normalization through `getErrorMessage` to avoid `any` catches and inconsistent user messaging.
2. Process instances and document type listing pages now include explicit loading/error/empty states for predictable UX.
3. Document type designer schema state now uses `ProcessFieldLibrary` typing with safe parsing guards before binding into editor/preview components.
4. Sentry Vite plugin source map upload is now conditional on required env vars (`SENTRY_AUTH_TOKEN`, `SENTRY_ORG`, `SENTRY_PROJECT`) so local/CI builds without Sentry credentials remain clean.
5. Browser smoke checking can be enabled in frontend verification to fail on uncaught page errors/console runtime errors.

---

## Environment controls

### Runner controls
```bash
BPM_RUNNER_LOG_LEVEL=info
BPM_RUNNER_NO_COLOR=0
BPM_RUNNER_DRY_RUN=0
BPM_RUNNER_SUMMARY=1
BPM_RUNNER_SUMMARY_FORMAT=table
BPM_RUNNER_LOG_TO_FILE=0
BPM_RUNNER_REQUIRE_CLEAN_GIT=0
BPM_RUNNER_ARTIFACTS_DIR=.automation
```

### Frontend controls
```bash
BPM_FRONTEND_SKIP_FORMAT=0
BPM_FRONTEND_SKIP_LINT=0
BPM_FRONTEND_SKIP_TYPECHECK=0
BPM_FRONTEND_SKIP_TESTS=0
BPM_FRONTEND_SKIP_BUILD=0
BPM_FRONTEND_ENABLE_COVERAGE=0
BPM_FRONTEND_ENABLE_BROWSER_SMOKE=0
BPM_FRONTEND_LINT_MAX_WARNINGS=-1
```

### Backend controls
```bash
BPM_BACKEND_SKIP_TESTS=0
BPM_BACKEND_SKIP_PACKAGE=0
BPM_BACKEND_ENABLE_VERIFY=0
```

---

## Copy-ready examples

### Simple example: quick health check
```bash
./scripts/doctor.sh
./scripts/verify-frontend.sh
```

### Simple example: targeted browser safety check
```bash
cd frontend && npm run test:browser-console
```

### Complete example: CI-like, strict, artifacted run
```bash
BPM_RUNNER_SUMMARY_FORMAT=json \
BPM_RUNNER_LOG_TO_FILE=1 \
BPM_FRONTEND_ENABLE_COVERAGE=1 \
BPM_FRONTEND_ENABLE_BROWSER_SMOKE=1 \
BPM_BACKEND_ENABLE_VERIFY=1 \
./scripts/verify-all.sh
```

### Complete example: Makefile workflow
```bash
make bootstrap
make doctor-json
make verify-browser-smoke
make verify-strict
```
