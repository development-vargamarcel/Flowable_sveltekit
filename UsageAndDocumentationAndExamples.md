# Usage, Documentation, and Examples

## Scope of this implementation pass

This pass strengthens the frontend API client (`frontend/src/lib/api/core.ts`) with stricter runtime option validation and more secure, customizable log redaction behavior.

## Implemented behavior updates

### 1) Runtime request-option validation (fail-fast)

`fetchApi` now validates dynamic runtime options before making a network call:

- `retryMode` must be one of: `auto | never | always`.
- `responseType` must be one of: `json | blob | text | arrayBuffer`.
- `credentialsMode` must be one of: `omit | same-origin | include`.
- Hook/parser options must be functions when provided:
  - `querySerializer`
  - `responseParser`
  - `onRequest`
  - `onResponse`
  - `onRetry`
  - `onError`

When invalid values are passed, `fetchApi` throws a structured `ApiError` with a clear configuration message.

### 2) Customizable sensitive log redaction

A new option was introduced:

```ts
additionalSensitiveLogKeys?: string[]
```

Behavior:

- Keys are normalized (`trim + lowercase`) to keep matching deterministic.
- Extra keys merge with built-in sensitive keys (e.g., `token`, `authorization`, `api_key`, etc.).
- Redaction now applies consistently to:
  - logged request URLs (query params)
  - logged request payloads
  - logged success payloads
  - logged error payloads

This allows endpoint-specific secret fields (for example `sessionId` or custom API credentials) to be hidden without editing global redaction defaults.

## Usage examples

### Basic request

```ts
import { fetchApi } from '$lib/api/core';

const tasks = await fetchApi('/api/tasks');
```

### Custom per-request redaction keys

```ts
await fetchApi('/api/workflow/run?sessionId=abc123', {
  method: 'POST',
  body: { sessionId: 'abc123', action: 'start' },
  additionalSensitiveLogKeys: ['sessionId']
});
```

### Validated runtime options

```ts
await fetchApi('/api/reports', {
  retryMode: 'always',
  responseType: 'json',
  credentialsMode: 'include'
});
```

### Hook option usage

```ts
await fetchApi('/api/export', {
  onRetry: ({ attempt, delayMs }) => {
    console.info(`Retry #${attempt} in ${delayMs}ms`);
  },
  onError: (error) => {
    console.error(error.getFullMessage());
  }
});
```

## Testing steps used for this implementation

From `frontend/`:

1. `npm install`
2. `npm run test -- src/tests/api.test.ts`
3. `npm run test`
4. `npm run check`
5. `npm run lint`

From `backend/` (using Java 17 for Maven enforcer compatibility):

6. `JAVA_HOME=$(mise where java@17.0.2) PATH="$JAVA_HOME/bin:$PATH" ./mvnw test`

## Maintenance notes

- Prefer `additionalSensitiveLogKeys` for endpoint-specific fields instead of expanding global defaults prematurely.
- Keep `ApiError` configuration messages explicit; these messages are relied on by tests and make integration debugging faster.
