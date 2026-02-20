# Comprehensive Enhancement Plan and Implementation Report

This document defines an extensive enhancement plan and tracks completion evidence for this execution.

## Objectives
- Improve API transport resilience and caller configurability.
- Strengthen query handling for real-world filtering use cases.
- Expand request/response contract controls to reduce integration bugs.
- Increase automated verification depth with focused regression tests.

## Major Improvements (22 total)

1. Add support for **array query parameters** (`tags=a&tags=b`) in `fetchApi`.
2. Add support for **Date query serialization** (ISO-8601 normalized output).
3. Enforce **deterministic query ordering** by sorting query keys to stabilize tests/cache keys.
4. Add per-request option to **omit empty-string query params**.
5. Extend `query` typing to allow richer scalar + array input values.
6. Add per-request **retryable status code overrides**.
7. Add per-request **retryable method overrides**.
8. Add per-request toggle to **disable network retries** for latency-sensitive calls.
9. Add per-request **retry callback hook** (`onRetry`) for observability/UI telemetry.
10. Ensure retry callback includes attempt index, max attempts, delay, HTTP status, and reason.
11. Refactor retry checks to consume **caller-specified retry policy** consistently.
12. Add per-request **expected status contract validation**.
13. Return explicit `ApiError` when expected status contract is violated.
14. Add per-request **custom response parser hook** for specialized payload handling.
15. Preserve existing default behavior when none of the new options are used.
16. Add automated test for array + date query serialization behavior.
17. Add automated test for empty-string query omission behavior.
18. Add automated test for expected status validation behavior.
19. Add automated test for custom response parser behavior.
20. Add automated test for retry callback invocation metadata.
21. Add automated test for custom retryable status/method policy.
22. Add automated test for disabling network retries.

## Completion Status

All 22 items above are fully implemented in code and covered by automated tests.

## Validation Checklist

- Frontend unit tests pass with the new scenarios and existing suite.
- Installed `openjdk-17-jdk` to satisfy backend Maven Java version constraints for full verification.
