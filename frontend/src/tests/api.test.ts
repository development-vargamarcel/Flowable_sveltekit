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

    expect(logger.debug).toHaveBeenCalledWith(expect.stringContaining('POST'), {
      body: 'plain-text-body'
    });
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
      expect.objectContaining({ url: expect.stringContaining('/api/cancelled') })
    );
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
});
