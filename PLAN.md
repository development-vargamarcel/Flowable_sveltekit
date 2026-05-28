# Consolidated Improvement Todo Plan

> Scope: prioritized engineering improvements with implementation notes.  
> Status legend: `[x] done`, `[~] in progress`, `[ ] queued`.

- [x] **1. Consolidate planning artifacts into one actionable plan file.**  
       **Notes:** Removed legacy plan markdown files and replaced them with this single source of truth to reduce drift.

- [x] **2. Harden variable display amount parsing in shared frontend utilities.**  
       **Notes:** Added safe finite-number parsing so malformed values no longer render as `$NaN`.

- [x] **3. Improve date formatting resilience for invalid payloads.**  
       **Notes:** `formatDate` now returns `Invalid date` when parsing fails rather than surfacing raw invalid locale output.

- [x] **4. Correct duration formatting edge cases (0, negative, day boundary).**  
       **Notes:** `formatDuration` now treats `0` as valid, rejects negatives, and uses `>=24h` day conversion.

- [x] **5. Extract pure CSV serialization utility for safer reuse/testing.**  
       **Notes:** Added `toCSV` helper to separate serialization from browser download side effects.

- [x] **6. Prevent memory leaks during CSV export downloads.**  
       **Notes:** Added `URL.revokeObjectURL` call after click lifecycle.

- [x] **7. Add focused unit tests for shared utility regressions.**  
       **Notes:** Added coverage for amount parsing, invalid dates, duration edge cases, and CSV escaping.

- [x] **8. Publish implementation notes for team handoff in `notes.md`.**  
       **Notes:** Added concise team-facing guidance, risk notes, and follow-up recommendations.

- [ ] **9. Introduce centralized i18n/date-time formatting strategy.**  
       **Notes:** Queued; current utility improvements are locale-ready but not yet locale-configurable.

- [ ] **10. Add frontend error-boundary UX for API failures.**  
       **Notes:** Queued for route-level fallback standardization.

- [ ] **11. Add optimistic-update rollback helpers in API client layer.**  
       **Notes:** Queued; requires mutation catalog and rollback contracts.

- [ ] **12. Implement reusable table virtualization for large datasets.**  
       **Notes:** Queued pending UX acceptance criteria.

- [ ] **13. Add backend request/response correlation IDs for tracing.**  
       **Notes:** Queued; requires logging filter + propagation through services.

- [ ] **14. Expand backend integration tests around process escalation paths.**  
       **Notes:** Queued; requires deterministic BPM fixtures.

- [ ] **15. Add audit event schema validation before persistence.**  
       **Notes:** Queued to reduce malformed history records.

- [ ] **16. Add security regression tests for role-protected endpoints.**  
       **Notes:** Queued with negative-path matrix.

- [ ] **17. Optimize dashboard query fan-out in analytics service.**  
       **Notes:** Queued after baseline instrumentation is collected.

- [ ] **18. Add frontend accessibility pass for form field components.**  
       **Notes:** Queued; target ARIA labeling and keyboard flows.

- [ ] **19. Add CI gate for formatting/lint/test split reporting.**  
       **Notes:** Queued to improve failure diagnostics in pipelines.

- [ ] **20. Create deployment readiness checklist with rollback playbook.**  
       **Notes:** Queued; depends on infra and release ownership review.

## Cleanup milestone (2026-05-28)

- [x] **21. Remove deprecated verification artifacts.**
       **Notes:** Deleted the legacy `frontend/verification/verify_changes.py` helper and stored screenshots after consolidating guidance around `./scripts/verify-all.sh`.

- [x] **22. Clarify frontend-only package ownership.**
       **Notes:** Removed the unused root `package-lock.json` expectation so the frontend lockfile is the single npm dependency lock.

- [x] **23. Route client debug output through the structured logger.**
       **Notes:** Replaced ad-hoc client-side `console.log` calls in stores/components/routes with the shared logger.

- [x] **24. Align Java compatibility messaging.**
       **Notes:** Standardized CI, scripts, and docs around Java 17 compatibility while allowing newer local runtimes.

- [x] **25. Add cleanup-safe verification guidance.**
       **Notes:** Added scope-based verification commands to the verification guide for docs, frontend, backend, automation, and cross-cutting cleanup.

## Legacy completion record (v1.8.3 hardening)

The prior `plan.md` tracked a completed v1.8.3 hardening pass. Its unique outcomes are preserved here:

- Implemented shared frontend unknown-error normalization utility and tests.
- Refactored process/document routes to remove `any`-typed catches and improve loading/empty/error UX.
- Hardened Sentry Vite upload behavior to only run when required credentials are present.
- Added optional browser-console smoke validation hooks in frontend verification scripts.
- Completed release alignment tasks (version bump and changelog update) for 1.8.3.
