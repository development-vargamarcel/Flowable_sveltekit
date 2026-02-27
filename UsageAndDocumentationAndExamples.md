# Usage, Documentation, and Examples

## Current Version

- **Frontend:** `1.4.0`
- **Backend:** `1.4.0`
- **Repository release:** `1.4.0` (**minor**) because this release adds backward-compatible reliability and developer-workflow improvements across automation scripts and validation flows.

---

## What was implemented in this pass

This release focused on making development and verification workflows safer, cleaner, and easier to diagnose:

1. Introduced a shared shell utility module (`scripts/common.sh`) for reusable logging, command checks, timers, and npm-safe execution.
2. Added timestamped, sectioned script output for easier CI and local debugging.
3. Added reusable repository-root validation to avoid accidental execution from incorrect paths.
4. Added centralized required-command checks and clear failure messaging.
5. Added npm safe wrapper that strips legacy proxy environment keys producing noisy npm warnings.
6. Enhanced `doctor.sh` with git branch/commit diagnostics.
7. Enhanced `doctor.sh` to validate lockfile and backend descriptor presence.
8. Standardized Node/npm/Java/Maven reporting in diagnostics output.
9. Improved `bootstrap.sh` with explicit frontend/backend phases and duration reporting.
10. Improved `verify-frontend.sh` with quality-gate sequencing and stage-level messages.
11. Improved `verify-backend.sh` with a consistent timing and diagnostics structure.
12. Improved `verify-all.sh` orchestration and total duration reporting.
13. Extended Makefile ergonomics with `verify-strict`, `check-lockfiles`, and `clean` targets.
14. Added script comments where behavior requires non-obvious context (notably npm env sanitization).
15. Completed full-stack verification after implementing all updates.

---

## Updated workflow commands

### 1) Bootstrap dependencies

```bash
./scripts/bootstrap.sh
```

What it does:

- Installs frontend dependencies with `npm ci` (through the safe npm wrapper).
- Prefetches backend Maven dependencies for faster offline and CI test runs.
- Prints elapsed duration.

### 2) Run environment diagnostics

```bash
./scripts/doctor.sh
```

What it checks:

- Required commands (`git`, `node`, `npm`, `java`, backend Maven wrapper)
- Current branch and short commit hash
- Node/npm/Java/Maven versions
- Frontend lockfile and backend `pom.xml` presence

### 3) Verify frontend quality gates

```bash
./scripts/verify-frontend.sh
```

Runs in order:

- format check
- lint
- Svelte type-check
- frontend unit tests
- production build

### 4) Verify backend quality gates

```bash
./scripts/verify-backend.sh
```

Runs:

- backend Maven test suite (`./mvnw -B test`)

### 5) Verify everything end-to-end

```bash
./scripts/verify-all.sh
```

Runs:

- doctor
- frontend verification
- backend verification
- total duration summary

### 6) Makefile shortcuts

```bash
make help
make bootstrap
make doctor
make check-lockfiles
make verify-frontend
make verify-backend
make test-frontend
make test-backend
make verify-strict
make verify
make clean
```

---

## Testing and validation examples

### Quick local confidence pass

```bash
./scripts/doctor.sh
./scripts/verify-frontend.sh
./scripts/verify-backend.sh
```

### Full CI-like pass

```bash
./scripts/bootstrap.sh
./scripts/verify-all.sh
```

### Strict frontend test pass with coverage

```bash
make verify-strict
```

---

## Semantic versioning update

- Previous version: `1.3.0`
- Current version: `1.4.0`
- Type: **minor**
- Rationale: significant non-breaking workflow and reliability enhancements were added without changing public API contracts.
