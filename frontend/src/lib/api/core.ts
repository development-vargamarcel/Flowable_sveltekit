import { backendStatus } from '$lib/stores/backendStatus';
import { toast } from 'svelte-sonner';
import { browser } from '$app/environment';
import { createLogger } from '$lib/utils/logger';

// In production, use relative URLs (empty string) so nginx can proxy /api/* to backend
// In development, use localhost:8080 for direct backend access
export const API_BASE =
  import.meta.env.VITE_API_URL ?? (import.meta.env.DEV ? 'http://localhost:8080' : '');

const log = createLogger('api');

// Retry configuration for backend startup
const STARTUP_RETRY_CONFIG = {
  maxRetries: 10,
  initialDelayMs: 1000,
  maxDelayMs: 5000,
  backoffMultiplier: 1.5
};

/**
 * Custom API error with detailed information
 */
export class ApiError extends Error {
  public readonly status: number;
  public readonly statusText: string;
  public readonly details?: string;
  public readonly fieldErrors?: Record<string, string>;
  public readonly timestamp?: string;

  constructor(
    message: string,
    status: number,
    statusText: string,
    details?: string,
    fieldErrors?: Record<string, string>,
    timestamp?: string
  ) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
    this.statusText = statusText;
    this.details = details;
    this.fieldErrors = fieldErrors;
    this.timestamp = timestamp;
  }

  /**
   * Get a user-friendly error message with details
   */
  getFullMessage(): string {
    if (this.details && this.details !== this.message) {
      return `${this.message}: ${this.details}`;
    }
    return this.message;
  }

  /**
   * Check if this is a validation error
   */
  isValidationError(): boolean {
    return this.status === 400 && !!this.fieldErrors && Object.keys(this.fieldErrors).length > 0;
  }
}

/**
 * Parse error response body and extract meaningful error information
 */
function parseErrorResponse(
  status: number,
  statusText: string,
  errorBody: Record<string, unknown> | null,
  rawResponse?: string
): ApiError {
  // Default error messages by status code with more helpful descriptions
  const defaultMessages: Record<number, { error: string; details: string }> = {
    400: { error: 'Bad request', details: 'The request was malformed or contained invalid data' },
    401: { error: 'Authentication failed', details: 'Invalid credentials or session expired' },
    403: {
      error: 'Access forbidden',
      details: 'You do not have permission to access this resource'
    },
    404: { error: 'Resource not found', details: 'The requested resource does not exist' },
    405: {
      error: 'Method not allowed',
      details: 'This HTTP method is not supported for this endpoint'
    },
    415: {
      error: 'Unsupported media type',
      details: 'The Content-Type header is missing or unsupported'
    },
    422: { error: 'Validation error', details: 'The submitted data failed validation' },
    500: { error: 'Internal server error', details: 'An unexpected error occurred on the server' },
    502: { error: 'Backend unavailable', details: 'The backend server is not responding' },
    503: { error: 'Service unavailable', details: 'The service is temporarily unavailable' },
    504: { error: 'Gateway timeout', details: 'The backend server took too long to respond' }
  };

  const defaultInfo = defaultMessages[status] || {
    error: `Request failed (${status})`,
    details: statusText
  };

  if (!errorBody) {
    // If we have a raw response that's not JSON, include it in details
    const detailsWithRaw = rawResponse
      ? `${defaultInfo.details}. Server response: ${rawResponse.substring(0, 200)}${rawResponse.length > 200 ? '...' : ''}`
      : defaultInfo.details;

    return new ApiError(defaultInfo.error, status, statusText, detailsWithRaw);
  }

  // Extract error information from various response formats
  // Handle both Spring Boot and custom error formats
  const error = (errorBody.error as string) ?? undefined;
  const message = (errorBody.message as string) ?? undefined;
  const details = (errorBody.details as string) ?? undefined;
  const fieldErrors = (errorBody.fieldErrors as Record<string, string>) ?? undefined;
  const timestamp = (errorBody.timestamp as string) ?? undefined;
  // Spring Boot specific fields
  const path = (errorBody.path as string) ?? undefined;
  // Note: trace is available but not used to avoid exposing stack traces to users
  const _trace = (errorBody.trace as string) ?? undefined;
  void _trace; // Suppress unused variable warning

  // Build the error message - prefer specific message over generic error
  let errorMessage: string;
  let errorDetails: string | undefined;

  if (message && message.length > 0 && message !== error) {
    // Use message as primary (usually more descriptive)
    if (error && error.length > 0) {
      errorMessage = error;
      errorDetails = message;
    } else {
      errorMessage = message;
      errorDetails = details;
    }
  } else if (error && error.length > 0) {
    errorMessage = error;
    errorDetails = details || message;
  } else if (details && details.length > 0) {
    errorMessage = defaultInfo.error;
    errorDetails = details;
  } else {
    errorMessage = defaultInfo.error;
    errorDetails = defaultInfo.details;
  }

  // Format field errors into details if present
  if (fieldErrors && Object.keys(fieldErrors).length > 0) {
    const fieldErrorMessages = Object.entries(fieldErrors)
      .map(([field, msg]) => `${field}: ${msg}`)
      .join('; ');
    errorDetails = errorDetails
      ? `${errorDetails}. Field errors: ${fieldErrorMessages}`
      : `Field errors: ${fieldErrorMessages}`;
  }

  // Add path info for debugging if available
  if (path && !errorDetails?.includes(path)) {
    errorDetails = errorDetails ? `${errorDetails} (path: ${path})` : `Path: ${path}`;
  }

  return new ApiError(errorMessage, status, statusText, errorDetails, fieldErrors, timestamp);
}

/**
 * Check if an error response indicates the backend is still starting up or unavailable
 */
function isBackendStartingError(
  errorBody: Record<string, unknown> | null,
  status?: number
): boolean {
  // 502 errors from the proxy indicate backend is unavailable - should retry
  if (status === 502) return true;

  if (!errorBody) return false;

  const error = errorBody.error as string | undefined;
  const message = errorBody.message as string | undefined;

  // Check for the specific startup message from Railway
  if (error === 'Service starting') return true;
  if (message?.toLowerCase().includes('backend is initializing')) return true;
  if (message?.toLowerCase().includes('service starting')) return true;

  // Check for proxy error messages from hooks.server.ts
  if (error?.toLowerCase().includes('backend unavailable')) return true;
  if (error?.toLowerCase().includes('connection refused')) return true;
  if (error?.toLowerCase().includes('connection timed out')) return true;
  if (message?.toLowerCase().includes('could not connect')) return true;

  return false;
}

/**
 * Sleep for a specified number of milliseconds
 */
function sleep(ms: number): Promise<void> {
  return new Promise((resolve) => setTimeout(resolve, ms));
}

/**
 * Calculate delay for next retry using exponential backoff
 */
function getRetryDelay(attempt: number): number {
  const delay =
    STARTUP_RETRY_CONFIG.initialDelayMs * Math.pow(STARTUP_RETRY_CONFIG.backoffMultiplier, attempt);
  return Math.min(delay, STARTUP_RETRY_CONFIG.maxDelayMs);
}

export interface FetchOptions extends RequestInit {
  responseType?: 'json' | 'blob' | 'text';
  /**
   * Optional request timeout in milliseconds.
   *
   * When set, the request will be aborted if the backend does not respond before
   * this duration. This helps avoid hanging UI states on very slow networks while
   * preserving default behavior for existing callers (no timeout by default).
   */
  timeoutMs?: number;
}

/**
 * Perform an API request with automatic error handling, logging, and retry logic.
 * @param endpoint - The API endpoint path (e.g., "/api/tasks").
 * @param options - Fetch options including method, headers, and body.
 * @returns A promise that resolves to the response data.
 * @throws {ApiError} If the request fails or returns an error status.
 */
export async function fetchApi<T>(endpoint: string, options: FetchOptions = {}): Promise<T> {
  const { timeoutMs, signal: callerSignal, ...requestOptions } = options;
  const url = `${API_BASE}${endpoint}`;
  const method = requestOptions.method || 'GET';
  const requestBody = formatRequestBodyForLogs(requestOptions.body);

  log.debug(`${method} ${url}`, { body: requestBody, timeoutMs });

  for (let attempt = 0; attempt <= STARTUP_RETRY_CONFIG.maxRetries; attempt++) {
    const signalController = createRequestSignal(timeoutMs, callerSignal);

    try {
      const response = await fetch(url, {
        ...requestOptions,
        signal: signalController.signal,
        credentials: 'include',
        headers: {
          'Content-Type': 'application/json',
          Accept: 'application/json',
          ...requestOptions.headers
        }
      });

      if (!response.ok) {
        // Read the raw response text first
        let rawText = '';
        let errorBody: Record<string, unknown> | null = null;

        try {
          rawText = await response.text();
          if (rawText && rawText.trim()) {
            // Check if it looks like JSON before parsing
            const trimmed = rawText.trim();
            if (trimmed.startsWith('{') || trimmed.startsWith('[')) {
              errorBody = JSON.parse(rawText);
            }
          }
        } catch {
          // Response body is not valid JSON
          log.warn('Failed to parse error response as JSON', {
            rawText: rawText.substring(0, 200)
          });
        }

        // Check if backend is still starting up or unavailable - retry with exponential backoff
        if (isBackendStartingError(errorBody, response.status)) {
          const delay = getRetryDelay(attempt);
          log.info(
            `Backend is starting, retrying in ${delay}ms (attempt ${attempt + 1}/${STARTUP_RETRY_CONFIG.maxRetries + 1})`,
            {
              url,
              method,
              attempt
            }
          );

          // Update backend status store
          backendStatus.setStarting(attempt + 1, STARTUP_RETRY_CONFIG.maxRetries + 1);

          if (attempt < STARTUP_RETRY_CONFIG.maxRetries) {
            await sleep(delay);
            continue; // Retry the request
          }
          // Max retries reached, fall through to throw error
        }

        log.error(`${method} ${url} failed`, undefined, {
          status: response.status,
          statusText: response.statusText,
          errorBody,
          rawText: rawText.substring(0, 500)
        });

        // Mark backend as ready if we got a response (even an error)
        backendStatus.setReady();

        if (browser && response.status >= 500) {
          const msg =
            (errorBody?.message as string) ||
            (errorBody?.error as string) ||
            'An unexpected error occurred on the server.';
          toast.error(`Server Error (${response.status})`, {
            description: msg
          });
        }

        throw parseErrorResponse(response.status, response.statusText, errorBody, rawText);
      }

      // Success - mark backend as ready
      backendStatus.setReady();

      // Handle empty responses (204 No Content)
      const contentLength = response.headers.get('content-length');
      if (response.status === 204 || contentLength === '0') {
        return {} as T;
      }

      if (options.responseType === 'blob') {
        const data = await response.blob();
        log.debug(`${method} ${url} success (blob)`);
        return data as unknown as T;
      }

      if (options.responseType === 'text') {
        const data = await response.text();
        log.debug(`${method} ${url} success (text)`);
        return data as unknown as T;
      }

      const data = await parseJsonResponse<T>(response, method, url);
      log.debug(`${method} ${url} success`, { data });
      return data;
    } catch (error) {
      // Re-throw ApiErrors as-is (they're already processed)
      if (error instanceof ApiError) {
        throw error;
      }

      if (error instanceof DOMException && error.name === 'AbortError') {
        if (signalController.wasTimedOut()) {
          const message = `The request to ${endpoint} timed out after ${timeoutMs}ms.`;
          log.warn('Request timed out', { url, method, timeoutMs, attempt });
          backendStatus.setError('Request timeout');

          if (browser) {
            toast.error('Request Timed Out', { description: message });
          }

          throw new ApiError('Request timeout', 408, 'Request Timeout', message);
        }

        log.info('Request aborted by caller', { url, method, attempt });
        throw new ApiError('Request cancelled', 0, 'Aborted', 'The request was cancelled.');
      }

      // For network errors during startup, retry
      if (error instanceof TypeError && attempt < STARTUP_RETRY_CONFIG.maxRetries) {
        const delay = getRetryDelay(attempt);
        log.info(
          `Network error, backend may be starting. Retrying in ${delay}ms (attempt ${attempt + 1}/${STARTUP_RETRY_CONFIG.maxRetries + 1})`,
          {
            url,
            method,
            error: error.message,
            attempt
          }
        );
        backendStatus.setStarting(attempt + 1, STARTUP_RETRY_CONFIG.maxRetries + 1);
        await sleep(delay);
        continue;
      }

      // Handle network errors with more specific messages
      if (error instanceof TypeError) {
        const isConnectionRefused =
          error.message.includes('fetch') || error.message.includes('network');
        log.error('Network error occurred', error, { url, method });
        backendStatus.setError('Connection failed');

        const message = isConnectionRefused
          ? `Unable to connect to ${url}. The server may be down or there may be a network issue.`
          : `Network error: ${error.message}`;

        if (browser) {
          toast.error('Connection Failed', { description: message });
        }

        throw new ApiError('Connection failed', 0, 'Network Error', message);
      }

      // Handle other errors
      const errorMessage = error instanceof Error ? error.message : 'Unknown error';
      log.error('Unexpected error', error, { url, method });

      if (browser) {
        toast.error('Unexpected Error', { description: errorMessage });
      }

      throw new ApiError(
        'Request failed',
        0,
        'Unknown Error',
        `An unexpected error occurred: ${errorMessage}`
      );
    } finally {
      signalController.cleanup();
    }
  }

  // If we've exhausted all retries
  backendStatus.setError('Backend failed to start');
  throw new ApiError(
    'Backend unavailable',
    503,
    'Service Unavailable',
    `Backend is still starting after ${STARTUP_RETRY_CONFIG.maxRetries + 1} attempts. Please try again in a moment.`
  );
}

interface RequestSignalController {
  signal?: AbortSignal;
  cleanup: () => void;
  wasTimedOut: () => boolean;
}

/**
 * Creates a request signal that combines caller cancellation with an optional timeout.
 *
 * We keep this logic centralized to make cancellation behavior explicit and testable.
 */
function createRequestSignal(
  timeoutMs?: number,
  callerSignal?: AbortSignal | null
): RequestSignalController {
  if (!timeoutMs || timeoutMs <= 0) {
    return {
      signal: callerSignal ?? undefined,
      cleanup: () => {},
      wasTimedOut: () => false
    };
  }

  const abortController = new AbortController();
  let timeoutTriggered = false;

  const timeoutId = setTimeout(() => {
    timeoutTriggered = true;
    abortController.abort();
  }, timeoutMs);

  const onCallerAbort = () => abortController.abort();
  if (callerSignal) {
    if (callerSignal.aborted) {
      onCallerAbort();
    } else {
      callerSignal.addEventListener('abort', onCallerAbort, { once: true });
    }
  }

  return {
    signal: abortController.signal,
    cleanup: () => {
      clearTimeout(timeoutId);
      if (callerSignal) {
        callerSignal.removeEventListener('abort', onCallerAbort);
      }
    },
    wasTimedOut: () => timeoutTriggered
  };
}

function formatRequestBodyForLogs(body: BodyInit | null | undefined): unknown {
  if (!body) return undefined;

  if (typeof body === 'string') {
    try {
      return JSON.parse(body);
    } catch {
      return body;
    }
  }

  if (body instanceof URLSearchParams) {
    return Object.fromEntries(body.entries());
  }

  if (body instanceof FormData) {
    return Object.fromEntries(body.entries());
  }

  return `[${body.constructor.name}]`;
}

async function parseJsonResponse<T>(response: Response, method: string, url: string): Promise<T> {
  const raw = await response.text();
  if (!raw.trim()) {
    log.warn(`${method} ${url} returned an empty response body for JSON payload`);
    return {} as T;
  }

  try {
    return JSON.parse(raw) as T;
  } catch (error) {
    const preview = raw.substring(0, 200);
    log.error(`${method} ${url} returned invalid JSON`, error, {
      responsePreview: preview
    });
    throw new ApiError(
      'Invalid server response',
      response.status,
      response.statusText,
      'The server returned malformed JSON.'
    );
  }
}
