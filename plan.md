# API Reliability Hardening Plan (Implemented)

This document defines and records a full implementation pass focused on strengthening the shared frontend API client (`frontend/src/lib/api/core.ts`) and its automated verification (`frontend/src/tests/api.test.ts`).

All items below were implemented and validated with tests.

## Major Improvements (20)

1. Standardized sensitive-key matching to lowercase forms so redaction logic is deterministic.
2. Expanded sensitive key coverage with additional common credential keys (`api_key`, `client_secret`, etc.).
3. Added URL query-value redaction for logs so sensitive query parameters are never logged in plaintext.
4. Preserved real request URLs while only sanitizing log output (no runtime behavior regressions).
5. Added strict `retryableMethods` validation so invalid method tokens fail fast.
6. Added strict `retryableStatusCodes` validation for valid HTTP status ranges.
7. Refactored expected status validation to reusable HTTP status helper validation.
8. Added `querySerializer` return-type validation to prevent runtime crashes from invalid serializer output.
9. Normalized retryable method inputs with trim+uppercase to tolerate accidental whitespace.
10. Introduced byte-accurate payload size measurement for `maxResponseBytes` checks.
11. Added pre-parse `content-length` enforcement to fail early on oversized successful responses.
12. Preserved post-parse payload size guard as a second line of defense.
13. Sanitized failure log messages to prevent sensitive query leakage in retry and error logging paths.
14. Sanitized success log messages to align with new secure logging behavior.
15. Sanitized timeout/cancellation/network/unexpected error log contexts for consistency.
16. Added regression tests proving sensitive query parameters are redacted in logs.
17. Added regression tests for invalid `querySerializer` return values.
18. Added regression tests for invalid retryable status/method configuration.
19. Added regression tests validating byte-oriented `maxResponseBytes` enforcement.
20. Added regression tests for nested `accessToken` redaction and early `content-length` rejection behavior.

## Verification Strategy

- Run targeted frontend API test suite (`src/tests/api.test.ts`) to verify new functionality.
- Run full frontend test suite to ensure no regressions outside the API module.
- Run frontend static checks (`check`) for type safety.
- Re-run backend Maven tests to ensure no backend regressions in the monorepo.
