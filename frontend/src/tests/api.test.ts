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
      expect.stringContaining('/api/query-check?page=2&includeArchived=false'),
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
});
