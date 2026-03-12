import { ApiError } from '$lib/api/core';

/**
 * Normalize unknown thrown values into a user-facing message.
 * Keeping this in one place prevents route-level `any` casts.
 */
export function getErrorMessage(error: unknown, fallback = 'Unexpected error'): string {
  if (error instanceof ApiError) {
    return error.getFullMessage();
  }

  if (error instanceof Error && error.message.trim().length > 0) {
    return error.message;
  }

  if (typeof error === 'string' && error.trim().length > 0) {
    return error;
  }

  return fallback;
}
