# Usage, Documentation, and Examples

## Scope of this implementation pass

This pass focused on **quality hardening across the existing codebase**:

- Resolving blocking lint issues in Svelte feature files.
- Aligning ESLint behavior with Svelte 5-generated UI wrapper patterns.
- Re-validating frontend and backend test gates.
- Updating contributor documentation so local verification is reproducible.

---

## What changed

### 1) Linting reliability improvements

The frontend lint flow had error-level violations that blocked `npm run verify`. This pass:

- Applied safe auto-fixes for style/const/escape issues.
- Corrected blocked components/routes with stale mutable declarations and unused symbols.
- Added a **targeted ESLint override** for generated `src/lib/components/ui/**/*.svelte` wrappers.

Why the override exists:

- The wrapper layer uses Svelte 5 `$props()` passthrough patterns.
- Those files intentionally rely on rest props and declaration patterns that conflict with strict generic lint rules.
- The override is scoped narrowly to generated UI wrappers so feature code remains fully lint-enforced.

### 2) Verification workflow refresh

The quality workflow was re-run after code changes:

- Frontend lint
- Frontend Svelte/TypeScript check
- Frontend unit tests
- Frontend combined verify script
- Backend Maven tests

This confirms the repository is in a verified state after the implementation pass.

### 3) Documentation synchronization

Documentation now reflects the updated day-to-day workflow and expected verification order.

---

## Recommended developer workflow

From `frontend/`:

1. `npm install`
2. `npm run lint`
3. `npm run check`
4. `npm run test:ci`
5. `npm run verify`

From `backend/`:

6. `./mvnw test`

From repository root (optional review command):

7. `git diff --stat`

---

## Examples

### Example: fast local validation before commit

```bash
cd frontend
npm run lint && npm run check && npm run test:ci
```

### Example: full repository quality sweep

```bash
cd frontend && npm run verify
cd ../backend && ./mvnw test
```

---

## Notes for maintainers

- Keep strict linting in feature/business logic paths.
- Keep Svelte 5 wrapper overrides tightly scoped to generated UI passthrough components.
- If additional generated wrapper folders are introduced, apply the same narrowly-scoped override pattern rather than weakening global rules.
