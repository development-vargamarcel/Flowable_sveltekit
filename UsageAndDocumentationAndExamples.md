# Usage, Documentation, and Examples

## Scope of this implementation pass

This implementation pass extends `fetchApi` in `frontend/src/lib/api/core.ts` to harden request configuration, query composition behavior, and response contract validation.

## Implemented behavior updates

### 1) Query configuration hardening

New query controls were added:

- `includeNullQueryParams?: boolean` (default `false`)
- `includeUndefinedQueryParams?: boolean` (default `false`)
- `sortQueryParams?: boolean` (default `true`)
- `trimQueryKeys?: boolean` (default `true`)
- `stripHashFromQuerySerializer?: boolean` (default `true`)

Runtime validation now rejects invalid non-object `query` values and non-boolean values for these toggles.

### 2) Response contract enforcement

New response guard options were added:

- `acceptedContentTypes?: string[]` to enforce an allow-list for response `content-type`.
- `requiredResponseHeaders?: string[]` to enforce mandatory response headers.

If either contract is violated, `fetchApi` throws a structured `ApiError` with explicit details.

### 3) Correlation header customization

New option:

- `requestIdHeaderName?: string` (default: `X-Request-ID`)

Behavior:

- The request ID header name is now configurable per request.
- If the configured header already exists in request headers, `fetchApi` preserves it.

### 4) Log preview safety controls

New options:

- `maxLoggedResponsePreviewChars?: number` (default `200`)
- `maxLoggedErrorPreviewChars?: number` (default `500`)

These configure truncation for response/error previews in logs to prevent oversized log payloads.

### 5) Base URL validation improvements

`baseUrl` now fails fast when:

- Empty or whitespace-only
- Absolute URL format is malformed

This avoids hidden runtime fetch failures and surfaces clear configuration errors earlier.

## Usage examples

### Include nullable query values for a backend filter API

```ts
await fetchApi('/api/search', {
  query: {
    status: null,
    includeArchived: undefined
  },
  includeNullQueryParams: true,
  includeUndefinedQueryParams: true
});
```

### Preserve insertion order of query parameters

```ts
await fetchApi('/api/reports', {
  query: { page: 2, pageSize: 20, sort: 'createdAt' },
  sortQueryParams: false
});
```

### Enforce JSON response contract with required tracing header

```ts
await fetchApi('/api/tasks', {
  acceptedContentTypes: ['application/json'],
  requiredResponseHeaders: ['x-trace-id']
});
```

### Customize request correlation header

```ts
await fetchApi('/api/workflow', {
  requestIdHeaderName: 'X-Correlation-ID'
});
```

### Use a serializer while stripping accidental hash fragments

```ts
await fetchApi('/api/advanced', {
  query: { term: 'invoice' },
  querySerializer: (query) => `term=${query.term}#debug`,
  // final URL omits #debug by default
});
```

## Testing and verification steps

From `frontend/`:

1. `npm install`
2. `npm run test -- src/tests/api.test.ts`
3. `npm run test`
4. `npm run check`
5. `npx eslint src/lib/api/core.ts src/tests/api.test.ts`

From `backend/`:

6. `./mvnw test` *(expected to fail with default Java 25 because backend enforces Java 17)*
7. `JAVA_HOME=$(mise where java@17.0.2) PATH="$JAVA_HOME/bin:$PATH" ./mvnw test`

## Notes for maintainers

- Prefer `acceptedContentTypes` and `requiredResponseHeaders` for endpoints with strict downstream contract requirements.
- Keep query toggle defaults conservative to preserve existing behavior unless consumers opt in explicitly.
- Use `requestIdHeaderName` only when integrating with systems that require a specific correlation header key.
