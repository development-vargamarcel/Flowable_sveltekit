# Usage, Documentation, and Examples

## Scope of this update

This update hardens the shared API client at `frontend/src/lib/api/core.ts` with safer logging, stronger runtime configuration validation, and stricter response-size handling.

## What changed

### 1) Safer logging

- Query parameters are now sanitized in logs using the same sensitive-key redaction rules used for request-body logging.
- This prevents keys like `token`, `authorization`, `api_key`, `client_secret`, and nested credentials from appearing in plaintext log output.
- Actual request URLs sent to `fetch` are unchanged; only log output is sanitized.

### 2) Stronger configuration validation

- `retryableMethods` now validates each value and rejects invalid method names.
- `retryableStatusCodes` now validates each status code and rejects anything outside `100-599`.
- `expectedStatus` uses the same shared HTTP-status validation.
- `querySerializer` must return a string; non-string values now throw a structured `ApiError`.

### 3) More accurate response-size protection

- `maxResponseBytes` now performs:
  1. **Early rejection** via `content-length` header when available.
  2. **Byte-accurate post-parse enforcement** using UTF-8 byte length rather than JS string length.
- This improves protection for non-ASCII data where character count differs from byte count.

## Usage examples

### Basic request

```ts
import { fetchApi } from "$lib/api/core";

const tasks = await fetchApi("/api/tasks");
```

### Custom retry configuration (validated)

```ts
await fetchApi("/api/jobs", {
  retryableMethods: ["GET", "HEAD"],
  retryableStatusCodes: [429, 503],
  maxRetries: 2,
});
```

### Query serializer (must return string)

```ts
await fetchApi("/api/search", {
  query: { q: "invoice", page: 1 },
  querySerializer: (query) => `q=${query.q}&page=${query.page}`,
});
```

### Response-size protection

```ts
await fetchApi("/api/analytics/export", {
  maxResponseBytes: 100_000,
});
```

## Testing steps

From `frontend/`:

1. `npm run test -- src/tests/api.test.ts`
2. `npm run test`
3. `npm run check`

From `backend/`:

4. `./mvnw test`

## Notes for maintainers

- If you add new sensitive auth keys used in query strings or JSON payloads, update the shared redaction key set in `core.ts`.
- Keep validation fail-fast and descriptive so consuming UI code receives predictable `ApiError` messages.
