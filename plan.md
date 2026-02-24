# One-Day Implementation Plan: API Client Reliability & Contract Hardening

## Objective
Deliver a full-day, production-grade hardening pass focused on the shared frontend API client (`fetchApi`) so request construction, transport behavior, and response contract validation are safer, more observable, and easier to debug.

## Major Improvements (20)

1. **Add strict validation for `query` shape** to reject non-object query payloads early.
2. **Add boolean-option runtime validation** for new query toggles to protect JS runtime callers.
3. **Add `includeNullQueryParams`** to optionally include `null` query values.
4. **Add `includeUndefinedQueryParams`** to optionally include `undefined` query values.
5. **Add `sortQueryParams` toggle** so callers can preserve insertion order when required.
6. **Add `trimQueryKeys` toggle** to support APIs that require literal key spacing.
7. **Extend query builder logic** to support all new query toggles consistently.
8. **Add `stripHashFromQuerySerializer` option** to sanitize accidental URL fragment output.
9. **Harden `baseUrl` validation** to reject empty and malformed absolute URLs.
10. **Add custom request correlation header name** via `requestIdHeaderName`.
11. **Preserve caller-supplied correlation header value** instead of always overwriting.
12. **Add `acceptedContentTypes` response contract guard** with explicit errors.
13. **Add `requiredResponseHeaders` response contract guard** with explicit missing-header diagnostics.
14. **Add configurable parse log preview cap** via `maxLoggedResponsePreviewChars`.
15. **Add configurable error log preview cap** via `maxLoggedErrorPreviewChars`.
16. **Validate `acceptedContentTypes` and `requiredResponseHeaders` arrays** as non-empty strings.
17. **Validate `requestIdHeaderName`** as a non-empty string.
18. **Add tests for new query toggle behavior** (null/undefined inclusion, sorting, key trimming).
19. **Add tests for new contract enforcement behavior** (content type + required headers).
20. **Add tests for new runtime validations/options** (query type, booleans, request ID header name, hash stripping).

## Verification Approach
- Run frontend dependency install.
- Run frontend lint, type-check, and unit tests.
- Run backend unit/integration test suite as available in environment.
- Review changed files to confirm every planned item is implemented and tested.
