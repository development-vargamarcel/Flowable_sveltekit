import { describe, expect, it } from 'vitest';
import { ApiError } from '$lib/api/core';
import { getErrorMessage } from './error-message';

describe('getErrorMessage', () => {
  it('returns ApiError full message', () => {
    const error = new ApiError('Validation error', 400, 'Bad Request', 'name is required');
    expect(getErrorMessage(error)).toBe('Validation error: name is required');
  });

  it('returns standard Error message', () => {
    expect(getErrorMessage(new Error('boom'))).toBe('boom');
  });

  it('returns string errors as-is', () => {
    expect(getErrorMessage('bad request')).toBe('bad request');
  });

  it('returns fallback for empty/unknown values', () => {
    expect(getErrorMessage({ code: 42 }, 'fallback')).toBe('fallback');
    expect(getErrorMessage('   ', 'fallback')).toBe('fallback');
  });
});
