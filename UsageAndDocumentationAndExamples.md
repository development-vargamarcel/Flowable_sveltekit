# Usage, Documentation, and Examples

## Current Version
- **Frontend:** `1.2.0`
- **Backend:** `1.2.0`
- **Semantic versioning update:** `1.1.0` → `1.2.0` (**minor**) because this release adds substantial, backward-compatible workflow and verification improvements without breaking public APIs.

---

## What was implemented in this pass

This implementation focused on full-stack engineering workflow hardening, CI modernization, and release-process reliability:

1. Standardized dependency and environment setup through reusable scripts.
2. Introduced deterministic verification scripts for frontend, backend, and combined full-stack checks.
3. Added a Makefile interface to reduce command drift across developers and CI.
4. Modernized CI to use maintained action versions and split jobs by responsibility.
5. Added a full-stack smoke stage to ensure local verification scripts and CI behavior stay aligned.
6. Expanded backend Java compatibility constraints so modern environments can run tests while preserving a Java 17 minimum baseline.
7. Updated package and artifact versions to `1.2.0` and synchronized project documentation.

---

## New workflow commands

### 1) Bootstrap dependencies
```bash
./scripts/bootstrap.sh
```
What it does:
- Installs frontend npm packages.
- Pre-fetches backend Maven dependencies to speed up subsequent builds.

### 2) Run environment diagnostics
```bash
./scripts/doctor.sh
```
What it prints:
- Repository path
- Node version
- npm version
- Java runtime version
- Maven wrapper version

### 3) Verify frontend quality gates
```bash
./scripts/verify-frontend.sh
```
Runs:
- `npm run format:check`
- `npm run lint`
- `npm run check`
- `npm run test:ci`
- `npm run build`

### 4) Verify backend quality gates
```bash
./scripts/verify-backend.sh
```
Runs:
- `./mvnw -B test`

### 5) Verify entire repository
```bash
./scripts/verify-all.sh
```
Runs:
- `doctor`
- frontend verification
- backend verification

### 6) Makefile shortcuts
```bash
make bootstrap
make doctor
make verify-frontend
make verify-backend
make verify
```

---

## CI/CD behavior

The CI workflow now executes three jobs:

1. **frontend**
   - Installs dependencies with `npm ci`
   - Runs formatting, lint, type-checks, tests, and build

2. **backend**
   - Uses JDK 21
   - Runs backend Maven tests

3. **fullstack-smoke** (depends on frontend + backend)
   - Installs required toolchains
   - Runs `./scripts/verify-all.sh`

This structure improves failure isolation while still validating the repository-level orchestration path.

---

## Important implementation notes

- Backend enforcer policy now accepts Java `[17,26)`.
  - This keeps Java 17 as the minimum supported baseline.
  - It also removes avoidable friction in environments using modern runtimes.
- New scripts intentionally include explanatory comments to help maintainers understand why each gate exists and how it should be used.

---

## Example usage scenarios

### Example A: New contributor setup + full validation
```bash
./scripts/bootstrap.sh
./scripts/verify-all.sh
```

### Example B: Fast local triage for backend-only changes
```bash
./scripts/doctor.sh
./scripts/verify-backend.sh
```

### Example C: Pre-PR frontend verification
```bash
cd frontend
npm run verify:full
```

---

## Testing and verification steps used for this release

```bash
./scripts/doctor.sh
./scripts/verify-frontend.sh
./scripts/verify-backend.sh
./scripts/verify-all.sh
```

All commands above were executed to verify implementation completeness and stability.
