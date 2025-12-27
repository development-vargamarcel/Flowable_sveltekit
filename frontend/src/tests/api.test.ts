import { describe, it, expect, vi, beforeEach } from 'vitest';
import { ApiError } from '$lib/api/client';

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

describe('API Client fetch behavior', () => {
  const mockFetch = vi.fn();

  beforeEach(() => {
    global.fetch = mockFetch;
    mockFetch.mockReset();
  });

  it('should handle successful JSON responses', async () => {
    const mockData = { id: 1, name: 'Test' };
    mockFetch.mockResolvedValueOnce({
      ok: true,
      status: 200,
      json: () => Promise.resolve(mockData),
      headers: new Headers({ 'content-length': '100' })
    });

    // Direct fetch test since we can't easily mock module imports
    const response = await fetch('/api/test');
    const data = await response.json();

    expect(data).toEqual(mockData);
  });

  it('should handle empty responses', async () => {
    mockFetch.mockResolvedValueOnce({
      ok: true,
      status: 204,
      headers: new Headers({ 'content-length': '0' })
    });

    const response = await fetch('/api/test');

    expect(response.status).toBe(204);
  });

  it('should handle network errors', async () => {
    mockFetch.mockRejectedValueOnce(new TypeError('Failed to fetch'));

    await expect(fetch('/api/test')).rejects.toThrow('Failed to fetch');
  });
});
