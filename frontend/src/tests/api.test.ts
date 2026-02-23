import { beforeEach, describe, expect, it, vi } from 'vitest';
const { logger, toastErrorSpy } = vi.hoisted(() => ({
  logger: {
    debug: vi.fn(),
    info: vi.fn(),
    warn: vi.fn(),
    error: vi.fn()
  },
  toastErrorSpy: vi.fn()
}));

vi.mock('$app/environment', () => ({ browser: true }));

vi.mock('$lib/utils/logger', () => ({
  createLogger: () => logger
}));

vi.mock('svelte-sonner', () => ({
  toast: {
    error: toastErrorSpy
  }
}));

import { ApiError, fetchApi } from '$lib/api/core';
import { backendStatus } from '$lib/stores/backendStatus';

describe('ApiError', () => {
  it('should create an error with correct properties', () => {
    const error = new ApiError(
      'Test error',
      400,
      'Bad Request',
      'Details here',
      undefined,
      '2024-01-01'
    );

    expect(error.message).toBe('Test error');
    expect(error.status).toBe(400);
    expect(error.statusText).toBe('Bad Request');
    expect(error.details).toBe('Details here');
    expect(error.timestamp).toBe('2024-01-01');
    expect(error.name).toBe('ApiError');
  });

  it('should return full message with details', () => {
    const error = new ApiError('Test error', 400, 'Bad Request', 'More details');

    expect(error.getFullMessage()).toBe('Test error: More details');
  });

  it('should return only message when details match', () => {
    const error = new ApiError('Test error', 400, 'Bad Request', 'Test error');

    expect(error.getFullMessage()).toBe('Test error');
  });

  it('should identify validation errors correctly', () => {
    const validationError = new ApiError('Validation failed', 400, 'Bad Request', undefined, {
      field1: 'is required'
    });

    expect(validationError.isValidationError()).toBe(true);
  });

  it('should not identify non-400 errors as validation errors', () => {
    const serverError = new ApiError('Server error', 500, 'Internal Server Error', undefined, {
      field1: 'is required'
    });

    expect(serverError.isValidationError()).toBe(false);
  });

  it('should not identify errors without field errors as validation errors', () => {
    const badRequest = new ApiError('Bad request', 400, 'Bad Request');

    expect(badRequest.isValidationError()).toBe(false);
  });
});

describe('fetchApi', () => {
  const mockFetch = vi.fn();

  beforeEach(() => {
    global.fetch = mockFetch;
    mockFetch.mockReset();
    vi.clearAllMocks();
    backendStatus.setReady();
  });

  it('logs string request bodies without throwing when body is not JSON', async () => {
    mockFetch.mockResolvedValueOnce({
      ok: true,
      status: 200,
      text: () => Promise.resolve('{"ok":true}'),
      statusText: 'OK',
      headers: new Headers({ 'content-length': '11' })
    });

    await fetchApi('/api/test', {
      method: 'POST',
      body: 'plain-text-body'
    });

    expect(logger.debug).toHaveBeenNthCalledWith(
      1,
      expect.stringContaining('POST'),
      expect.objectContaining({
        body: 'plain-text-body',
        requestId: expect.any(String)
      })
    );
  });

  it('does not force a JSON content type for FormData requests', async () => {
    const formData = new FormData();
    formData.append('file', 'payload');

    mockFetch.mockImplementationOnce((_url: string, init?: RequestInit) => {
      const headers = new Headers(init?.headers);
      expect(headers.has('content-type')).toBe(false);
      return Promise.resolve({
        ok: true,
        status: 200,
        text: () => Promise.resolve('{"ok":true}'),
        statusText: 'OK',
        headers: new Headers({ 'content-length': '11' })
      });
    });

    await fetchApi('/api/upload', {
      method: 'POST',
      body: formData
    });
  });

  it('attaches a correlation header to outgoing requests', async () => {
    mockFetch.mockImplementationOnce((_url: string, init?: RequestInit) => {
      const headers = new Headers(init?.headers);
      expect(headers.get('X-Request-ID')).toBeTruthy();
      return Promise.resolve({
        ok: true,
        status: 200,
        text: () => Promise.resolve('{"ok":true}'),
        statusText: 'OK',
        headers: new Headers({ 'content-length': '11' })
      });
    });

    await fetchApi('/api/request-id-test');
  });

  it('normalizes request methods before logging and retry evaluation', async () => {
    mockFetch.mockResolvedValueOnce({
      ok: true,
      status: 200,
      text: () => Promise.resolve('{"ok":true}'),
      statusText: 'OK',
      headers: new Headers({ 'content-length': '11' })
    });

    await fetchApi('/api/normalize-method', { method: ' get ' });

    expect(logger.debug).toHaveBeenCalledWith(
      expect.stringContaining('GET'),
      expect.objectContaining({ requestId: expect.any(String) })
    );
  });

  it('returns text payload when responseType is text', async () => {
    mockFetch.mockResolvedValueOnce({
      ok: true,
      status: 200,
      text: () => Promise.resolve('plain response'),
      statusText: 'OK',
      headers: new Headers({ 'content-length': '14' })
    });

    const data = await fetchApi<string>('/api/test', {
      responseType: 'text'
    });

    expect(data).toBe('plain response');
  });

  it('returns empty object for successful empty JSON responses', async () => {
    mockFetch.mockResolvedValueOnce({
      ok: true,
      status: 200,
      text: () => Promise.resolve('   '),
      statusText: 'OK',
      headers: new Headers({})
    });

    const data = await fetchApi<Record<string, never>>('/api/test');

    expect(data).toEqual({});
    expect(logger.warn).toHaveBeenCalledWith(
      expect.stringContaining('returned an empty response body for JSON payload')
    );
  });

  it('throws a structured ApiError when success response has malformed JSON', async () => {
    mockFetch.mockResolvedValueOnce({
      ok: true,
      status: 200,
      text: () => Promise.resolve('{bad json'),
      statusText: 'OK',
      headers: new Headers({ 'content-length': '9' })
    });

    await expect(fetchApi('/api/test')).rejects.toMatchObject({
      name: 'ApiError',
      message: 'Invalid server response',
      status: 200,
      statusText: 'OK',
      details: 'The server returned malformed JSON.'
    });

    expect(logger.error).toHaveBeenCalledWith(
      expect.stringContaining('returned invalid JSON'),
      expect.any(SyntaxError),
      expect.objectContaining({ responsePreview: '{bad json' })
    );
  });

  it('aborts slow requests when timeoutMs is reached', async () => {
    vi.useFakeTimers();

    mockFetch.mockImplementationOnce((_url: string, init?: RequestInit) => {
      return new Promise((_resolve, reject) => {
        init?.signal?.addEventListener('abort', () => {
          reject(new DOMException('The operation was aborted.', 'AbortError'));
        });
      });
    });

    const requestPromise = fetchApi('/api/slow', { timeoutMs: 50 });
    const timeoutExpectation = expect(requestPromise).rejects.toMatchObject({
      name: 'ApiError',
      message: 'Request timeout',
      status: 408,
      statusText: 'Request Timeout'
    });

    await vi.advanceTimersByTimeAsync(60);
    await timeoutExpectation;

    expect(toastErrorSpy).toHaveBeenCalledWith('Request Timed Out', {
      description: 'The request to /api/slow timed out after 50ms.'
    });
    expect(backendStatus.state).toBe('error');

    vi.useRealTimers();
  });

  it('returns a cancellation error when aborted by caller signal', async () => {
    mockFetch.mockImplementationOnce((_url: string, init?: RequestInit) => {
      return new Promise((_resolve, reject) => {
        init?.signal?.addEventListener('abort', () => {
          reject(new DOMException('The operation was aborted.', 'AbortError'));
        });
      });
    });

    const callerController = new AbortController();
    const requestPromise = fetchApi('/api/cancelled', { signal: callerController.signal });
    callerController.abort();

    await expect(requestPromise).rejects.toMatchObject({
      name: 'ApiError',
      message: 'Request cancelled',
      statusText: 'Aborted',
      details: 'The request was cancelled.'
    });
    expect(logger.info).toHaveBeenCalledWith(
      'Request aborted by caller',
      expect.objectContaining({
        url: expect.stringContaining('/api/cancelled'),
        requestId: expect.any(String)
      })
    );
  });

  it('does not retry non-idempotent network failures', async () => {
    mockFetch.mockRejectedValue(new TypeError('network error'));

    await expect(
      fetchApi('/api/mutations', {
        method: 'POST',
        body: JSON.stringify({ name: 'test' })
      })
    ).rejects.toMatchObject({
      message: 'Connection failed',
      status: 0
    });

    expect(mockFetch).toHaveBeenCalledTimes(1);
  });

  it('retries GET requests on 429 using retry-after header', async () => {
    vi.useFakeTimers();

    mockFetch
      .mockResolvedValueOnce({
        ok: false,
        status: 429,
        statusText: 'Too Many Requests',
        text: () => Promise.resolve('{"message":"slow down"}'),
        headers: new Headers({ 'retry-after': '0', 'content-length': '23' })
      })
      .mockResolvedValueOnce({
        ok: true,
        status: 200,
        text: () => Promise.resolve('{"ok":true}'),
        statusText: 'OK',
        headers: new Headers({ 'content-length': '11' })
      });

    const requestPromise = fetchApi<{ ok: boolean }>('/api/rate-limited');
    await vi.runAllTimersAsync();
    const data = await requestPromise;

    expect(data).toEqual({ ok: true });
    expect(mockFetch).toHaveBeenCalledTimes(2);
    vi.useRealTimers();
  });

  it('shows server toast for HTTP 500 responses and sets backend ready when server responds', async () => {
    mockFetch.mockResolvedValueOnce({
      ok: false,
      status: 500,
      statusText: 'Server Error',
      text: () => Promise.resolve('{"message":"boom"}'),
      headers: new Headers({ 'content-length': '18' })
    });

    await expect(fetchApi('/api/test')).rejects.toBeInstanceOf(ApiError);

    expect(toastErrorSpy).toHaveBeenCalledWith('Server Error (500)', {
      description: 'boom'
    });
    expect(backendStatus.state).toBe('ready');
  });

  it('ignores non-string field errors without crashing', async () => {
    mockFetch.mockResolvedValueOnce({
      ok: false,
      status: 400,
      statusText: 'Bad Request',
      text: () => Promise.resolve('{"error":"Validation failed","fieldErrors":{"name":123}}'),
      headers: new Headers({ 'content-length': '57' })
    });

    await expect(fetchApi('/api/test')).rejects.toMatchObject({
      name: 'ApiError',
      message: 'Validation failed'
    });
  });

  it('auto-serializes plain object request bodies', async () => {
    mockFetch.mockImplementationOnce((_url: string, init?: RequestInit) => {
      expect(init?.body).toBe(JSON.stringify({ hello: 'world' }));
      const headers = new Headers(init?.headers);
      expect(headers.get('content-type')).toBe('application/json');
      return Promise.resolve({
        ok: true,
        status: 200,
        text: () => Promise.resolve('{"ok":true}'),
        statusText: 'OK',
        headers: new Headers({ 'content-length': '11', 'content-type': 'application/json' })
      });
    });

    await fetchApi('/api/object-body', {
      method: 'POST',
      body: { hello: 'world' } as unknown as BodyInit
    });
  });

  it('returns text automatically for non-json content types', async () => {
    mockFetch.mockResolvedValueOnce({
      ok: true,
      status: 200,
      text: () => Promise.resolve('plain server text'),
      statusText: 'OK',
      headers: new Headers({ 'content-length': '17', 'content-type': 'text/plain' })
    });

    const data = await fetchApi<string>('/api/plain-content');
    expect(data).toBe('plain server text');
  });

  it('parses json payloads even when content-type is missing', async () => {
    mockFetch.mockResolvedValueOnce({
      ok: true,
      status: 200,
      text: () => Promise.resolve('{"ok":true}'),
      statusText: 'OK',
      headers: new Headers({ 'content-length': '11' })
    });

    const data = await fetchApi<{ ok: boolean }>('/api/missing-content-type');
    expect(data).toEqual({ ok: true });
  });

  it('respects retryMode never even for retryable GET statuses', async () => {
    mockFetch.mockResolvedValueOnce({
      ok: false,
      status: 503,
      statusText: 'Service Unavailable',
      text: () => Promise.resolve('{"message":"try later"}'),
      headers: new Headers({ 'content-length': '23' })
    });

    await expect(fetchApi('/api/no-retry', { retryMode: 'never' })).rejects.toBeInstanceOf(
      ApiError
    );
    expect(mockFetch).toHaveBeenCalledTimes(1);
  });

  it('retries PUT when retryMode always is enabled', async () => {
    vi.useFakeTimers();
    mockFetch
      .mockResolvedValueOnce({
        ok: false,
        status: 503,
        statusText: 'Service Unavailable',
        text: () => Promise.resolve('{"message":"starting"}'),
        headers: new Headers({ 'retry-after': '0', 'content-length': '22' })
      })
      .mockResolvedValueOnce({
        ok: true,
        status: 200,
        text: () => Promise.resolve('{"ok":true}'),
        statusText: 'OK',
        headers: new Headers({ 'content-length': '11', 'content-type': 'application/json' })
      });

    const requestPromise = fetchApi<{ ok: boolean }>('/api/put-retry', {
      method: 'PUT',
      retryMode: 'always'
    });
    await vi.runAllTimersAsync();
    await expect(requestPromise).resolves.toEqual({ ok: true });
    expect(mockFetch).toHaveBeenCalledTimes(2);
    vi.useRealTimers();
  });

  it('retries DELETE in auto mode for retryable status codes', async () => {
    vi.useFakeTimers();
    mockFetch
      .mockResolvedValueOnce({
        ok: false,
        status: 504,
        statusText: 'Gateway Timeout',
        text: () => Promise.resolve('{"message":"timeout"}'),
        headers: new Headers({ 'retry-after': '0', 'content-length': '21' })
      })
      .mockResolvedValueOnce({
        ok: true,
        status: 200,
        text: () => Promise.resolve('{"deleted":true}'),
        statusText: 'OK',
        headers: new Headers({ 'content-length': '16', 'content-type': 'application/json' })
      });

    const requestPromise = fetchApi<{ deleted: boolean }>('/api/resource/1', {
      method: 'DELETE'
    });

    await vi.runAllTimersAsync();
    await expect(requestPromise).resolves.toEqual({ deleted: true });
    expect(mockFetch).toHaveBeenCalledTimes(2);
    vi.useRealTimers();
  });

  it('uses backend request-id from response headers in errors', async () => {
    mockFetch.mockResolvedValueOnce({
      ok: false,
      status: 500,
      statusText: 'Server Error',
      text: () => Promise.resolve('{"message":"boom"}'),
      headers: new Headers({ 'content-length': '18', 'x-request-id': 'server-request-id-123' })
    });

    await expect(fetchApi('/api/server-request-id')).rejects.toMatchObject({
      name: 'ApiError',
      requestId: 'server-request-id-123'
    });
  });

  it('normalizes endpoints that are passed without a leading slash', async () => {
    mockFetch.mockResolvedValueOnce({
      ok: true,
      status: 200,
      text: () => Promise.resolve('{"ok":true}'),
      statusText: 'OK',
      headers: new Headers({ 'content-length': '11' })
    });

    await fetchApi('api/no-leading-slash');

    expect(mockFetch).toHaveBeenCalledWith(
      expect.stringContaining('/api/no-leading-slash'),
      expect.any(Object)
    );
  });

  it('appends query parameters while skipping null or undefined values', async () => {
    mockFetch.mockResolvedValueOnce({
      ok: true,
      status: 200,
      text: () => Promise.resolve('{"ok":true}'),
      statusText: 'OK',
      headers: new Headers({ 'content-length': '11' })
    });

    await fetchApi('/api/query-check', {
      query: { page: 2, includeArchived: false, ignoreMe: undefined, alsoIgnore: null }
    });

    expect(mockFetch).toHaveBeenCalledWith(
      expect.stringContaining('/api/query-check?includeArchived=false&page=2'),
      expect.any(Object)
    );
  });

  it('redacts sensitive keys when logging request bodies', async () => {
    mockFetch.mockResolvedValueOnce({
      ok: true,
      status: 200,
      text: () => Promise.resolve('{"ok":true}'),
      statusText: 'OK',
      headers: new Headers({ 'content-length': '11' })
    });

    await fetchApi('/api/redaction', {
      method: 'POST',
      body: { username: 'alex', password: 'super-secret', token: 'abc' } as unknown as BodyInit
    });

    expect(logger.debug).toHaveBeenNthCalledWith(
      1,
      expect.stringContaining('POST'),
      expect.objectContaining({
        body: {
          username: 'alex',
          password: '[REDACTED]',
          token: '[REDACTED]'
        }
      })
    );
  });

  it('supports arrayBuffer responses', async () => {
    const binary = new Uint8Array([1, 2, 3]).buffer;
    mockFetch.mockResolvedValueOnce({
      ok: true,
      status: 200,
      arrayBuffer: () => Promise.resolve(binary),
      text: () => Promise.resolve('unused'),
      statusText: 'OK',
      headers: new Headers({ 'content-length': '3' })
    });

    const data = await fetchApi<ArrayBuffer>('/api/binary', { responseType: 'arrayBuffer' });
    expect(new Uint8Array(data)).toEqual(new Uint8Array([1, 2, 3]));
  });

  it('uses per-request maxRetries override for retry loops', async () => {
    vi.useFakeTimers();
    mockFetch
      .mockResolvedValueOnce({
        ok: false,
        status: 503,
        statusText: 'Service Unavailable',
        text: () => Promise.resolve('{"message":"still starting"}'),
        headers: new Headers({ 'retry-after': '0', 'content-length': '28' })
      })
      .mockResolvedValueOnce({
        ok: false,
        status: 503,
        statusText: 'Service Unavailable',
        text: () => Promise.resolve('{"message":"still starting"}'),
        headers: new Headers({ 'retry-after': '0', 'content-length': '28' })
      });

    const requestExpectation = expect(
      fetchApi('/api/max-retries', { maxRetries: 1 })
    ).rejects.toBeInstanceOf(ApiError);

    await vi.runAllTimersAsync();
    await requestExpectation;
    expect(mockFetch).toHaveBeenCalledTimes(2);
    vi.useRealTimers();
  });

  it('serializes array and date query params deterministically', async () => {
    mockFetch.mockResolvedValueOnce({
      ok: true,
      status: 200,
      text: () => Promise.resolve('{"ok":true}'),
      statusText: 'OK',
      headers: new Headers({ 'content-length': '11' })
    });

    await fetchApi('/api/query-advanced', {
      query: {
        tags: ['alpha', 'beta'],
        createdAfter: new Date('2024-01-01T00:00:00.000Z'),
        a: 1
      }
    });

    expect(mockFetch).toHaveBeenCalledWith(
      expect.stringContaining(
        '/api/query-advanced?a=1&createdAfter=2024-01-01T00%3A00%3A00.000Z&tags=alpha&tags=beta'
      ),
      expect.any(Object)
    );
  });

  it('can omit empty-string query params when configured', async () => {
    mockFetch.mockResolvedValueOnce({
      ok: true,
      status: 200,
      text: () => Promise.resolve('{"ok":true}'),
      statusText: 'OK',
      headers: new Headers({ 'content-length': '11' })
    });

    await fetchApi('/api/query-empty', {
      query: { q: '', page: 2 },
      includeEmptyStringQueryParams: false
    });

    expect(mockFetch).toHaveBeenCalledWith(
      expect.stringContaining('/api/query-empty?page=2'),
      expect.any(Object)
    );
  });

  it('supports custom expected status enforcement', async () => {
    mockFetch.mockResolvedValueOnce({
      ok: true,
      status: 200,
      text: () => Promise.resolve('{"ok":true}'),
      statusText: 'OK',
      headers: new Headers({ 'content-length': '11' })
    });

    await expect(fetchApi('/api/expected-status', { expectedStatus: 201 })).rejects.toMatchObject({
      name: 'ApiError',
      message: 'Unexpected response status',
      status: 200
    });
  });

  it('allows custom response parsers for specialized payloads', async () => {
    mockFetch.mockResolvedValueOnce({
      ok: true,
      status: 200,
      text: () => Promise.resolve('raw payload'),
      statusText: 'OK',
      headers: new Headers({ 'content-length': '11' })
    });

    const parsed = await fetchApi('/api/custom-parser', {
      responseParser: async (response) => ({
        upper: (await response.text()).toUpperCase()
      })
    });

    expect(parsed).toEqual({ upper: 'RAW PAYLOAD' });
  });

  it('invokes onRetry callback with retry details', async () => {
    vi.useFakeTimers();
    const onRetry = vi.fn();

    mockFetch
      .mockResolvedValueOnce({
        ok: false,
        status: 503,
        statusText: 'Service Unavailable',
        text: () => Promise.resolve('{"message":"try again"}'),
        headers: new Headers({ 'retry-after': '0', 'content-length': '23' })
      })
      .mockResolvedValueOnce({
        ok: true,
        status: 200,
        text: () => Promise.resolve('{"ok":true}'),
        statusText: 'OK',
        headers: new Headers({ 'content-length': '11' })
      });

    const request = fetchApi('/api/retry-callback', { onRetry, maxRetries: 1 });
    await vi.runAllTimersAsync();
    await request;

    expect(onRetry).toHaveBeenCalledWith(
      expect.objectContaining({
        reason: 'http',
        attempt: 1,
        maxAttempts: 2,
        status: 503
      })
    );

    vi.useRealTimers();
  });

  it('supports request-specific retryable status codes', async () => {
    vi.useFakeTimers();

    mockFetch
      .mockResolvedValueOnce({
        ok: false,
        status: 418,
        statusText: "I'm a teapot",
        text: () => Promise.resolve('{"message":"short outage"}'),
        headers: new Headers({ 'retry-after': '0', 'content-length': '26' })
      })
      .mockResolvedValueOnce({
        ok: true,
        status: 200,
        text: () => Promise.resolve('{"ok":true}'),
        statusText: 'OK',
        headers: new Headers({ 'content-length': '11' })
      });

    const request = fetchApi('/api/custom-status-retry', {
      retryableStatusCodes: [418],
      retryableMethods: ['GET'],
      maxRetries: 1
    });

    await vi.runAllTimersAsync();
    await expect(request).resolves.toEqual({ ok: true });
    vi.useRealTimers();
  });

  it('can disable network retries per request', async () => {
    mockFetch.mockRejectedValue(new TypeError('network error'));

    await expect(
      fetchApi('/api/no-network-retry', {
        retryOnNetworkError: false,
        maxRetries: 3
      })
    ).rejects.toMatchObject({
      message: 'Connection failed',
      status: 0
    });

    expect(mockFetch).toHaveBeenCalledTimes(1);
  });

  it('supports baseUrl overrides for edge and integration environments', async () => {
    mockFetch.mockResolvedValueOnce({
      ok: true,
      status: 200,
      text: () => Promise.resolve('{"ok":true}'),
      statusText: 'OK',
      headers: new Headers({ 'content-length': '11' })
    });

    await fetchApi('/api/base-url', { baseUrl: 'https://example.test' });

    expect(mockFetch).toHaveBeenCalledWith(
      expect.stringContaining('https://example.test/api/base-url'),
      expect.any(Object)
    );
  });

  it('allows custom query serialization for backend-specific conventions', async () => {
    mockFetch.mockResolvedValueOnce({
      ok: true,
      status: 200,
      text: () => Promise.resolve('{"ok":true}'),
      statusText: 'OK',
      headers: new Headers({ 'content-length': '11' })
    });

    await fetchApi('/api/custom-query', {
      query: { tags: ['x', 'y'] },
      querySerializer: () => 'tags=x|y'
    });

    expect(mockFetch).toHaveBeenCalledWith(
      expect.stringContaining('/api/custom-query?tags=x|y'),
      expect.any(Object)
    );
  });

  it('trims string query values when trimStringQueryParams is enabled', async () => {
    mockFetch.mockResolvedValueOnce({
      ok: true,
      status: 200,
      text: () => Promise.resolve('{"ok":true}'),
      statusText: 'OK',
      headers: new Headers({ 'content-length': '11' })
    });

    await fetchApi('/api/query-trim', {
      query: { q: '  spaced  ' },
      trimStringQueryParams: true
    });

    expect(mockFetch).toHaveBeenCalledWith(
      expect.stringContaining('/api/query-trim?q=spaced'),
      expect.any(Object)
    );
  });

  it('deduplicates array query values when configured', async () => {
    mockFetch.mockResolvedValueOnce({
      ok: true,
      status: 200,
      text: () => Promise.resolve('{"ok":true}'),
      statusText: 'OK',
      headers: new Headers({ 'content-length': '11' })
    });

    await fetchApi('/api/query-dedupe', {
      query: { tag: ['a', 'a', 'b'] },
      dedupeArrayQueryParams: true
    });

    expect(mockFetch).toHaveBeenCalledWith(
      expect.stringContaining('/api/query-dedupe?tag=a&tag=b'),
      expect.any(Object)
    );
  });

  it('runs onRequest hook to allow runtime request mutation', async () => {
    mockFetch.mockImplementationOnce((_url: string, init?: RequestInit) => {
      const headers = new Headers(init?.headers);
      expect(headers.get('X-Env')).toBe('test');
      return Promise.resolve({
        ok: true,
        status: 200,
        text: () => Promise.resolve('{"ok":true}'),
        statusText: 'OK',
        headers: new Headers({ 'content-length': '11' })
      });
    });

    await fetchApi('/api/request-hook', {
      onRequest: () => ({ headers: { 'X-Env': 'test' } })
    });
  });

  it('runs onResponse hook for side-effect integrations', async () => {
    const onResponse = vi.fn();
    mockFetch.mockResolvedValueOnce({
      ok: true,
      status: 200,
      text: () => Promise.resolve('{"ok":true}'),
      clone: function () {
        return this;
      },
      statusText: 'OK',
      headers: new Headers({ 'content-length': '11' })
    });

    await fetchApi('/api/response-hook', { onResponse });

    expect(onResponse).toHaveBeenCalledTimes(1);
  });

  it('can suppress browser toasts for server and network failures', async () => {
    mockFetch.mockResolvedValueOnce({
      ok: false,
      status: 500,
      statusText: 'Server Error',
      text: () => Promise.resolve('{"message":"boom"}'),
      headers: new Headers({ 'content-length': '18' })
    });

    await expect(fetchApi('/api/no-toast', { suppressErrorToast: true })).rejects.toBeInstanceOf(
      ApiError
    );

    expect(toastErrorSpy).not.toHaveBeenCalled();
  });

  it('allows custom timeout and network error messaging', async () => {
    vi.useFakeTimers();

    mockFetch.mockImplementationOnce((_url: string, init?: RequestInit) => {
      return new Promise((_resolve, reject) => {
        init?.signal?.addEventListener('abort', () => {
          reject(new DOMException('The operation was aborted.', 'AbortError'));
        });
      });
    });

    const requestPromise = fetchApi('/api/custom-timeout', {
      timeoutMs: 10,
      timeoutMessage: 'Custom timeout message'
    });
    const timeoutExpectation = expect(requestPromise).rejects.toMatchObject({
      details: 'Custom timeout message'
    });

    await vi.advanceTimersByTimeAsync(15);
    await timeoutExpectation;

    vi.useRealTimers();
  });

  it('validates retry and timeout numeric configuration', async () => {
    await expect(fetchApi('/api/invalid-config', { maxRetries: -1 })).rejects.toMatchObject({
      message: 'Invalid request configuration'
    });
    await expect(fetchApi('/api/invalid-config', { timeoutMs: 0 })).rejects.toMatchObject({
      message: 'Invalid request configuration'
    });
  });

  it('detects backend startup messages in either error or message field', async () => {
    mockFetch
      .mockResolvedValueOnce({
        ok: false,
        status: 503,
        statusText: 'Service Unavailable',
        text: () => Promise.resolve('{"message":"could not connect to backend"}'),
        headers: new Headers({ 'content-length': '41' })
      })
      .mockResolvedValueOnce({
        ok: true,
        status: 200,
        text: () => Promise.resolve('{"ok":true}'),
        statusText: 'OK',
        headers: new Headers({ 'content-length': '11' })
      });

    const data = await fetchApi<{ ok: boolean }>('/api/retry');

    expect(data).toEqual({ ok: true });
    expect(mockFetch).toHaveBeenCalledTimes(2);
    expect(logger.info).toHaveBeenCalledWith(
      expect.stringContaining('Backend is starting, retrying'),
      expect.objectContaining({ method: 'GET' })
    );
  });

  it('trims blank query keys and skips non-finite numeric query values', async () => {
    mockFetch.mockResolvedValueOnce({
      ok: true,
      status: 200,
      text: () => Promise.resolve('{"ok":true}'),
      statusText: 'OK',
      headers: new Headers({ 'content-length': '11' })
    });

    await fetchApi('/api/query-sanitize', {
      query: { '  ': 'skip-me', valid: 1, bad: Number.NaN }
    });

    expect(mockFetch).toHaveBeenCalledWith(
      expect.stringContaining('/api/query-sanitize?valid=1'),
      expect.any(Object)
    );
  });

  it('handles baseUrl values with trailing slash without double-slash URLs', async () => {
    mockFetch.mockResolvedValueOnce({
      ok: true,
      status: 200,
      text: () => Promise.resolve('{"ok":true}'),
      statusText: 'OK',
      headers: new Headers({ 'content-length': '11' })
    });

    await fetchApi('/api/base-url-trim', { baseUrl: 'https://example.test///' });

    expect(mockFetch).toHaveBeenCalledWith(
      expect.stringContaining('https://example.test/api/base-url-trim'),
      expect.any(Object)
    );
  });

  it('normalizes custom query serializer output that starts with a question mark', async () => {
    mockFetch.mockResolvedValueOnce({
      ok: true,
      status: 200,
      text: () => Promise.resolve('{"ok":true}'),
      statusText: 'OK',
      headers: new Headers({ 'content-length': '11' })
    });

    await fetchApi('/api/custom-query-prefix', {
      query: { page: 1 },
      querySerializer: () => '?page=1'
    });

    expect(mockFetch).toHaveBeenCalledWith(
      expect.stringContaining('/api/custom-query-prefix?page=1'),
      expect.any(Object)
    );
  });

  it('redacts authorization-like keys regardless of case in logged request body', async () => {
    mockFetch.mockResolvedValueOnce({
      ok: true,
      status: 200,
      text: () => Promise.resolve('{"ok":true}'),
      statusText: 'OK',
      headers: new Headers({ 'content-length': '11' })
    });

    await fetchApi('/api/sensitive', {
      method: 'POST',
      body: {
        Authorization: 'secret-token',
        nested: { apiKey: 'abc123' }
      } as unknown as BodyInit
    });

    expect(logger.debug).toHaveBeenCalledWith(
      expect.stringContaining('POST'),
      expect.objectContaining({
        body: {
          Authorization: '[REDACTED]',
          nested: { apiKey: '[REDACTED]' }
        }
      })
    );
  });

  it('runs onError hook when server response fails', async () => {
    const onError = vi.fn();
    mockFetch.mockResolvedValueOnce({
      ok: false,
      status: 500,
      statusText: 'Server Error',
      text: () => Promise.resolve('{"message":"boom"}'),
      headers: new Headers({ 'content-length': '18' })
    });

    await expect(fetchApi('/api/on-error', { onError })).rejects.toBeInstanceOf(ApiError);
    expect(onError).toHaveBeenCalledTimes(1);
    expect(onError.mock.calls[0][0]).toBeInstanceOf(ApiError);
  });

  it('rejects invalid expectedStatus configuration', async () => {
    await expect(fetchApi('/api/invalid-status', { expectedStatus: 99 })).rejects.toMatchObject({
      message: 'Invalid request configuration'
    });
  });

  it('redacts sensitive query values from log messages while preserving actual request URL', async () => {
    mockFetch.mockResolvedValueOnce({
      ok: true,
      status: 200,
      text: () => Promise.resolve('{"ok":true}'),
      statusText: 'OK',
      headers: new Headers({ 'content-length': '11' })
    });

    await fetchApi('/api/log-query', {
      query: { token: 'abc123', page: 1 }
    });

    const [loggedMessage] = logger.debug.mock.calls[0];
    expect(loggedMessage).toContain('token=%5BREDACTED%5D');
    expect(mockFetch).toHaveBeenCalledWith(
      expect.stringContaining('/api/log-query?page=1&token=abc123'),
      expect.any(Object)
    );
  });

  it('rejects non-string values returned by querySerializer', async () => {
    await expect(
      fetchApi('/api/query-serializer-invalid', {
        query: { page: 1 },
        querySerializer: () => 123 as unknown as string
      })
    ).rejects.toMatchObject({
      message: 'Invalid request configuration',
      details: 'querySerializer must return a string.'
    });
  });

  it('rejects invalid retryable status code configuration', async () => {
    await expect(
      fetchApi('/api/invalid-retry-status', {
        retryableStatusCodes: [700],
        maxRetries: 1
      })
    ).rejects.toMatchObject({
      message: 'Invalid request configuration',
      details: 'retryableStatusCodes must contain valid HTTP status codes.'
    });
  });

  it('rejects invalid retryable method names', async () => {
    await expect(
      fetchApi('/api/invalid-retry-method', {
        retryableMethods: ['G3T']
      })
    ).rejects.toMatchObject({
      message: 'Invalid request configuration',
      details: 'retryableMethods must only include valid HTTP method names.'
    });
  });

  it('evaluates maxResponseBytes using byte size instead of string length', async () => {
    mockFetch.mockResolvedValueOnce({
      ok: true,
      status: 200,
      text: () => Promise.resolve('{"value":"🙂"}'),
      statusText: 'OK',
      headers: new Headers({ 'content-length': '16', 'content-type': 'application/json' })
    });

    await expect(fetchApi('/api/max-bytes', { maxResponseBytes: 12 })).rejects.toMatchObject({
      message: 'Response payload too large'
    });
  });

  it('fails early when content-length exceeds maxResponseBytes', async () => {
    const responseParser = vi.fn();
    mockFetch.mockResolvedValueOnce({
      ok: true,
      status: 200,
      text: () => Promise.resolve('{"ok":true}'),
      statusText: 'OK',
      headers: new Headers({ 'content-length': '999', 'content-type': 'application/json' })
    });

    await expect(
      fetchApi('/api/max-bytes-header', { maxResponseBytes: 10, responseParser })
    ).rejects.toMatchObject({
      message: 'Response payload too large'
    });
    expect(responseParser).not.toHaveBeenCalled();
  });

  it('redacts accessToken values in nested request bodies', async () => {
    mockFetch.mockResolvedValueOnce({
      ok: true,
      status: 200,
      text: () => Promise.resolve('{"ok":true}'),
      statusText: 'OK',
      headers: new Headers({ 'content-length': '11' })
    });

    await fetchApi('/api/nested-sensitive', {
      method: 'POST',
      body: {
        profile: {
          accessToken: 'raw-token'
        }
      } as unknown as BodyInit
    });

    expect(logger.debug).toHaveBeenNthCalledWith(
      1,
      expect.stringContaining('POST'),
      expect.objectContaining({
        body: {
          profile: {
            accessToken: '[REDACTED]'
          }
        }
      })
    );
  });
  it('rejects oversized responses using maxResponseBytes', async () => {
    mockFetch.mockResolvedValueOnce({
      ok: true,
      status: 200,
      text: () => Promise.resolve('{"value":"1234567890"}'),
      statusText: 'OK',
      headers: new Headers({ 'content-length': '20', 'content-type': 'application/json' })
    });

    await expect(fetchApi('/api/oversized', { maxResponseBytes: 5 })).rejects.toMatchObject({
      message: 'Response payload too large'
    });
  });
});
