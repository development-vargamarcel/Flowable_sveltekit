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

const RETRYABLE_METHODS = new Set(['GET', 'HEAD', 'OPTIONS', 'PUT', 'DELETE']);
const RETRYABLE_STATUS_CODES = new Set([429, 502, 503, 504]);
const SENSITIVE_LOG_KEYS = new Set([
  'password',
  'token',
  'accesstoken',
  'refreshtoken',
  'secret',
  'authorization',
  'apikey',
  'api_key',
  'clientsecret',
  'client_secret'
]);
const DEFAULT_JSON_ACCEPT = 'application/json';
const VALID_RESPONSE_TYPES = new Set(['json', 'blob', 'text', 'arrayBuffer']);
const VALID_RETRY_MODES = new Set(['auto', 'never', 'always']);
const VALID_CREDENTIAL_MODES = new Set(['omit', 'same-origin', 'include']);

type QueryValue = string | number | boolean | Date | null | undefined;
type QueryInput = Record<string, QueryValue | QueryValue[]>;

const HTTP_METHOD_PATTERN = /^[A-Za-z]+$/;

function createRequestId(): string {
  if (typeof crypto !== 'undefined' && typeof crypto.randomUUID === 'function') {
    return crypto.randomUUID();
  }

  return `req-${Date.now()}-${Math.random().toString(16).slice(2, 10)}`;
}

function isRetryableRequest(
  method: string,
  status: number | undefined,
  retryMode: 'auto' | 'never' | 'always' = 'auto',
  retryableMethods: Set<string>,
  retryableStatusCodes: Set<number>
): boolean {
  if (retryMode === 'never') return false;
  if (retryMode === 'always') return status === undefined || retryableStatusCodes.has(status);

  const normalizedMethod = method.toUpperCase();
  if (!retryableMethods.has(normalizedMethod)) {
    return false;
  }

  if (status === undefined) {
    return true;
  }

  return retryableStatusCodes.has(status);
}

function parseRetryAfterMs(retryAfterValue: string | null): number | null {
  if (!retryAfterValue) return null;

  const asSeconds = Number(retryAfterValue);
  if (Number.isFinite(asSeconds) && asSeconds >= 0) {
    return asSeconds * 1000;
  }

  const asDate = new Date(retryAfterValue);
  if (Number.isNaN(asDate.getTime())) {
    return null;
  }

  const diff = asDate.getTime() - Date.now();
  return diff > 0 ? diff : 0;
}

/**
 * Custom API error with detailed information
 */
export class ApiError extends Error {
  public readonly status: number;
  public readonly statusText: string;
  public readonly details?: string;
  public readonly fieldErrors?: Record<string, string>;
  public readonly timestamp?: string;
  public readonly requestId?: string;

  constructor(
    message: string,
    status: number,
    statusText: string,
    details?: string,
    fieldErrors?: Record<string, string>,
    timestamp?: string,
    requestId?: string
  ) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
    this.statusText = statusText;
    this.details = details;
    this.fieldErrors = fieldErrors;
    this.timestamp = timestamp;
    this.requestId = requestId;
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
  rawResponse?: string,
  requestId?: string
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

    return new ApiError(
      defaultInfo.error,
      status,
      statusText,
      detailsWithRaw,
      undefined,
      undefined,
      requestId
    );
  }

  const extractString = (value: unknown): string | undefined =>
    typeof value === 'string' && value.trim().length > 0 ? value : undefined;

  const extractFieldErrors = (value: unknown): Record<string, string> | undefined => {
    if (!value || typeof value !== 'object' || Array.isArray(value)) {
      return undefined;
    }

    const normalized = Object.entries(value).reduce<Record<string, string>>((acc, [k, v]) => {
      if (typeof v === 'string' && v.trim().length > 0) {
        acc[k] = v;
      }
      return acc;
    }, {});

    return Object.keys(normalized).length > 0 ? normalized : undefined;
  };

  // Extract error information from various response formats
  // Handle both Spring Boot and custom error formats
  const error = extractString(errorBody.error);
  const message = extractString(errorBody.message);
  const details = extractString(errorBody.details);
  const fieldErrors = extractFieldErrors(errorBody.fieldErrors);
  const timestamp = extractString(errorBody.timestamp);
  // Spring Boot specific fields
  const path = extractString(errorBody.path);
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

  return new ApiError(
    errorMessage,
    status,
    statusText,
    errorDetails,
    fieldErrors,
    timestamp,
    requestId
  );
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

  const error = typeof errorBody.error === 'string' ? errorBody.error : '';
  const message = typeof errorBody.message === 'string' ? errorBody.message : '';
  const combinedText = `${error} ${message}`.toLowerCase();

  // Check for the specific startup message from Railway
  if (error === 'Service starting') return true;
  if (combinedText.includes('backend is initializing')) return true;
  if (combinedText.includes('service starting')) return true;

  // Check for proxy error messages from hooks.server.ts
  if (combinedText.includes('backend unavailable')) return true;
  if (combinedText.includes('connection refused')) return true;
  if (combinedText.includes('connection timed out')) return true;
  if (combinedText.includes('could not connect')) return true;

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
function getRetryDelay(
  attempt: number,
  retryAfterMs?: number | null,
  initialDelayMs = STARTUP_RETRY_CONFIG.initialDelayMs,
  backoffMultiplier = STARTUP_RETRY_CONFIG.backoffMultiplier
): number {
  if (typeof retryAfterMs === 'number' && retryAfterMs >= 0) {
    return Math.min(retryAfterMs, STARTUP_RETRY_CONFIG.maxDelayMs);
  }

  const delay = initialDelayMs * Math.pow(backoffMultiplier, attempt);
  const jitter = Math.floor(Math.random() * 250);
  return Math.min(delay + jitter, STARTUP_RETRY_CONFIG.maxDelayMs);
}

function normalizeEndpoint(endpoint: string): string {
  const trimmed = endpoint.trim();
  if (!trimmed) {
    throw new ApiError('Invalid endpoint', 0, 'Client Error', 'Endpoint cannot be empty.');
  }

  return trimmed.startsWith('/') ? trimmed : `/${trimmed}`;
}

function appendQueryParams(
  url: string,
  query?: QueryInput,
  includeEmptyStringQueryParams = true,
  trimStringQueryParams = false,
  dedupeArrayQueryParams = false,
  includeNullQueryParams = false,
  includeUndefinedQueryParams = false,
  sortQueryParams = true,
  trimQueryKeys = true
): string {
  if (!query) return url;

  const params = new URLSearchParams();
  const rawEntries = Object.entries(query);
  const sortedEntries = sortQueryParams
    ? rawEntries.sort(([a], [b]) => a.localeCompare(b))
    : rawEntries;
  for (const [rawKey, rawValue] of sortedEntries) {
    const key = trimQueryKeys ? rawKey.trim() : rawKey;
    if (!key) continue;

    const values = Array.isArray(rawValue) ? rawValue : [rawValue];
    const normalizedValues = dedupeArrayQueryParams ? Array.from(new Set(values)) : values;
    for (const value of normalizedValues) {
      if (value === null && !includeNullQueryParams) {
        continue;
      }

      if (value === undefined && !includeUndefinedQueryParams) {
        continue;
      }

      if (typeof value === 'number' && !Number.isFinite(value)) {
        continue;
      }

      const normalizedValue =
        trimStringQueryParams && typeof value === 'string' ? value.trim() : value;

      if (normalizedValue === '' && !includeEmptyStringQueryParams) {
        continue;
      }

      // Dates are normalized to ISO strings so client-side filtering remains deterministic.
      params.append(
        key,
        normalizedValue instanceof Date ? normalizedValue.toISOString() : String(normalizedValue)
      );
    }
  }

  const suffix = params.toString();
  if (!suffix) return url;
  return `${url}${url.includes('?') ? '&' : '?'}${suffix}`;
}

function getPayloadSizeBytes(payload: unknown): number {
  if (typeof payload === 'string') {
    return new TextEncoder().encode(payload).length;
  }

  try {
    return new TextEncoder().encode(JSON.stringify(payload)).length;
  } catch {
    // Non-serializable objects should not crash response validation.
    return Number.POSITIVE_INFINITY;
  }
}

export interface FetchOptions extends RequestInit {
  responseType?: 'json' | 'blob' | 'text' | 'arrayBuffer';
  /** Control retry behavior for this request. */
  retryMode?: 'auto' | 'never' | 'always';
  /**
   * Optional request timeout in milliseconds.
   *
   * When set, the request will be aborted if the backend does not respond before
   * this duration. This helps avoid hanging UI states on very slow networks while
   * preserving default behavior for existing callers (no timeout by default).
   */
  timeoutMs?: number;
  query?: QueryInput;
  querySerializer?: (query: QueryInput) => string;
  /** Include query params with empty-string values (default true for backward compatibility). */
  includeEmptyStringQueryParams?: boolean;
  trimStringQueryParams?: boolean;
  dedupeArrayQueryParams?: boolean;
  maxRetries?: number;
  retryableStatusCodes?: number[];
  retryableMethods?: string[];
  retryOnNetworkError?: boolean;
  initialRetryDelayMs?: number;
  maxRetryDelayMs?: number;
  retryBackoffMultiplier?: number;
  retryJitterMs?: number;
  expectedStatus?: number | number[];
  responseParser?: (response: Response) => Promise<unknown>;
  onRequest?: (context: {
    url: string;
    method: string;
    options: RequestInit;
  }) => RequestInit | void;
  onResponse?: (response: Response) => void | Promise<void>;
  suppressErrorToast?: boolean;
  baseUrl?: string;
  credentialsMode?: RequestCredentials;
  timeoutMessage?: string;
  networkErrorMessage?: string;
  skipDefaultAcceptHeader?: boolean;
  skipDefaultContentTypeHeader?: boolean;
  onRetry?: (details: {
    attempt: number;
    maxAttempts: number;
    delayMs: number;
    status?: number;
    reason: 'http' | 'network';
  }) => void;
  onError?: (error: ApiError) => void | Promise<void>;
  maxResponseBytes?: number;
  /**
   * Additional object keys that should be redacted in request/response logs.
   * Keys are normalized with trim + lowercase to make matching deterministic.
   */
  additionalSensitiveLogKeys?: string[];
  includeNullQueryParams?: boolean;
  includeUndefinedQueryParams?: boolean;
  sortQueryParams?: boolean;
  trimQueryKeys?: boolean;
  stripHashFromQuerySerializer?: boolean;
  acceptedContentTypes?: string[];
  requiredResponseHeaders?: string[];
  requestIdHeaderName?: string;
  maxLoggedResponsePreviewChars?: number;
  maxLoggedErrorPreviewChars?: number;
}

// Preserve the existing default JSON content type while avoiding overrides
// for bodies that rely on the browser to set the boundary/encoding.
function shouldSetJsonContentType(body: BodyInit | null | undefined): boolean {
  if (!body) return true;

  if (typeof body === 'string') return true;
  if (body instanceof URLSearchParams) return false;
  if (typeof FormData !== 'undefined' && body instanceof FormData) return false;
  if (typeof Blob !== 'undefined' && body instanceof Blob) return false;
  if (body instanceof ArrayBuffer) return false;
  if (ArrayBuffer.isView(body)) return false;
  if (typeof ReadableStream !== 'undefined' && body instanceof ReadableStream) return false;

  return true;
}

function buildRequestHeaders(
  options: RequestInit,
  skipDefaultAcceptHeader = false,
  skipDefaultContentTypeHeader = false
): Headers {
  const headers = new Headers(options.headers || undefined);

  if (!skipDefaultAcceptHeader && !headers.has('accept')) {
    headers.set('Accept', DEFAULT_JSON_ACCEPT);
  }

  if (
    !skipDefaultContentTypeHeader &&
    !headers.has('content-type') &&
    shouldSetJsonContentType(options.body ?? null)
  ) {
    headers.set('Content-Type', 'application/json');
  }

  return headers;
}

function normalizeMethod(method?: string): string {
  return (method ?? 'GET').trim().toUpperCase();
}

function validateNumericOption(name: string, value: number | undefined, min = 0): void {
  if (value === undefined) return;
  if (!Number.isFinite(value) || value < min) {
    throw new ApiError(
      'Invalid request configuration',
      0,
      'Client Error',
      `${name} must be >= ${min}.`
    );
  }
}

function validateHttpStatusCode(name: string, status: number): void {
  if (!Number.isInteger(status) || status < 100 || status > 599) {
    throw new ApiError(
      'Invalid request configuration',
      0,
      'Client Error',
      `${name} must contain valid HTTP status codes.`
    );
  }
}

function validateRetryableMethods(methods?: string[]): void {
  if (!methods) return;

  for (const method of methods) {
    if (!method || !HTTP_METHOD_PATTERN.test(method.trim())) {
      throw new ApiError(
        'Invalid request configuration',
        0,
        'Client Error',
        'retryableMethods must only include valid HTTP method names.'
      );
    }
  }
}

function validateStringArrayOption(name: string, values?: string[]): void {
  if (!values) return;

  for (const value of values) {
    if (typeof value !== 'string' || !value.trim()) {
      throw new ApiError(
        'Invalid request configuration',
        0,
        'Client Error',
        `${name} must only include non-empty strings.`
      );
    }
  }
}

function validateFunctionOption(name: string, value: unknown): void {
  if (value === undefined || typeof value === 'function') return;

  throw new ApiError(
    'Invalid request configuration',
    0,
    'Client Error',
    `${name} must be a function.`
  );
}

function validateBooleanOption(name: string, value: unknown): void {
  if (value === undefined || typeof value === 'boolean') return;

  throw new ApiError(
    'Invalid request configuration',
    0,
    'Client Error',
    `${name} must be a boolean.`
  );
}

function validateResponseHeaderNameOption(name: string, value: string | undefined): void {
  if (value === undefined) return;
  if (typeof value !== 'string' || !value.trim()) {
    throw new ApiError(
      'Invalid request configuration',
      0,
      'Client Error',
      `${name} must be a non-empty string.`
    );
  }
}

function validateQueryInput(query: QueryInput | undefined): void {
  if (query === undefined) return;
  if (query === null || typeof query !== 'object' || Array.isArray(query)) {
    throw new ApiError(
      'Invalid request configuration',
      0,
      'Client Error',
      'query must be an object map of key/value pairs.'
    );
  }
}

function createSensitiveLogKeySet(extraKeys?: string[]): Set<string> {
  const merged = new Set(SENSITIVE_LOG_KEYS);
  for (const extraKey of extraKeys ?? []) {
    merged.add(extraKey.trim().toLowerCase());
  }

  return merged;
}

function sanitizeBaseUrl(baseUrl: string): string {
  const sanitized = baseUrl.trim().replace(/\/+$/, '');
  if (!sanitized) {
    throw new ApiError(
      'Invalid request configuration',
      0,
      'Client Error',
      'baseUrl cannot be empty.'
    );
  }

  if (/^https?:\/\//i.test(sanitized)) {
    try {
      // Validate absolute URLs early so consumers fail fast before dispatch.
      new URL(sanitized);
    } catch {
      throw new ApiError(
        'Invalid request configuration',
        0,
        'Client Error',
        'baseUrl must be a valid absolute URL.'
      );
    }
  }

  return sanitized;
}

function normalizeRequestBody(
  body: BodyInit | Record<string, unknown> | null | undefined
): BodyInit | null | undefined {
  if (!body || typeof body === 'string') {
    return body as BodyInit | null | undefined;
  }

  if (
    body instanceof URLSearchParams ||
    (typeof FormData !== 'undefined' && body instanceof FormData) ||
    (typeof Blob !== 'undefined' && body instanceof Blob) ||
    body instanceof ArrayBuffer ||
    ArrayBuffer.isView(body) ||
    (typeof ReadableStream !== 'undefined' && body instanceof ReadableStream)
  ) {
    return body as BodyInit;
  }

  // Serialize plain objects automatically so callers can pass typed payloads directly.
  return JSON.stringify(body);
}

function sanitizeUrlForLogsWithKeys(url: string, sensitiveLogKeys: Set<string>): string {
  const [base, queryString] = url.split('?', 2);
  if (!queryString) return base;

  const params = new URLSearchParams(queryString);
  const sanitized = new URLSearchParams();
  params.forEach((value, key) => {
    const normalizedKey = key.trim().toLowerCase();
    sanitized.append(key, sensitiveLogKeys.has(normalizedKey) ? '[REDACTED]' : value);
  });

  const output = sanitized.toString();
  return output ? `${base}?${output}` : base;
}

function redactSensitiveValuesWithKeys(value: unknown, sensitiveLogKeys: Set<string>): unknown {
  if (!value || typeof value !== 'object') {
    return value;
  }

  if (Array.isArray(value)) {
    return value.map((item) => redactSensitiveValuesWithKeys(item, sensitiveLogKeys));
  }

  return Object.entries(value as Record<string, unknown>).reduce<Record<string, unknown>>(
    (acc, [key, nestedValue]) => {
      const normalizedKey = key.toLowerCase();
      if (sensitiveLogKeys.has(normalizedKey)) {
        acc[key] = '[REDACTED]';
      } else {
        acc[key] = redactSensitiveValuesWithKeys(nestedValue, sensitiveLogKeys);
      }
      return acc;
    },
    {}
  );
}

/**
 * Perform an API request with automatic error handling, logging, and retry logic.
 * @param endpoint - The API endpoint path (e.g., "/api/tasks").
 * @param options - Fetch options including method, headers, and body.
 * @returns A promise that resolves to the response data.
 * @throws {ApiError} If the request fails or returns an error status.
 */
export async function fetchApi<T>(endpoint: string, options: FetchOptions = {}): Promise<T> {
  const {
    timeoutMs,
    signal: callerSignal,
    retryMode = 'auto',
    query,
    querySerializer,
    includeEmptyStringQueryParams = true,
    trimStringQueryParams = false,
    dedupeArrayQueryParams = false,
    maxRetries,
    retryableStatusCodes,
    retryableMethods,
    retryOnNetworkError = true,
    initialRetryDelayMs,
    maxRetryDelayMs,
    retryBackoffMultiplier,
    retryJitterMs,
    expectedStatus,
    responseParser,
    onRequest,
    onResponse,
    suppressErrorToast = false,
    baseUrl,
    credentialsMode,
    timeoutMessage,
    networkErrorMessage,
    skipDefaultAcceptHeader = false,
    skipDefaultContentTypeHeader = false,
    onRetry,
    onError,
    maxResponseBytes,
    additionalSensitiveLogKeys,
    includeNullQueryParams = false,
    includeUndefinedQueryParams = false,
    sortQueryParams = true,
    trimQueryKeys = true,
    stripHashFromQuerySerializer = true,
    acceptedContentTypes,
    requiredResponseHeaders,
    requestIdHeaderName = 'X-Request-ID',
    maxLoggedResponsePreviewChars = 200,
    maxLoggedErrorPreviewChars = 500,
    ...requestOptions
  } = options;
  validateNumericOption('maxRetries', maxRetries, 0);
  validateNumericOption('timeoutMs', timeoutMs, 1);
  validateNumericOption('initialRetryDelayMs', initialRetryDelayMs, 0);
  validateNumericOption('maxRetryDelayMs', maxRetryDelayMs, 0);
  validateNumericOption('retryBackoffMultiplier', retryBackoffMultiplier, 1);
  validateNumericOption('retryJitterMs', retryJitterMs, 0);
  validateNumericOption('maxResponseBytes', maxResponseBytes, 1);
  validateRetryableMethods(retryableMethods);
  validateStringArrayOption('additionalSensitiveLogKeys', additionalSensitiveLogKeys);
  validateStringArrayOption('acceptedContentTypes', acceptedContentTypes);
  validateStringArrayOption('requiredResponseHeaders', requiredResponseHeaders);
  validateFunctionOption('querySerializer', querySerializer);
  validateFunctionOption('responseParser', responseParser);
  validateFunctionOption('onRequest', onRequest);
  validateFunctionOption('onResponse', onResponse);
  validateFunctionOption('onRetry', onRetry);
  validateFunctionOption('onError', onError);
  validateBooleanOption('includeNullQueryParams', includeNullQueryParams);
  validateBooleanOption('includeUndefinedQueryParams', includeUndefinedQueryParams);
  validateBooleanOption('sortQueryParams', sortQueryParams);
  validateBooleanOption('trimQueryKeys', trimQueryKeys);
  validateBooleanOption('stripHashFromQuerySerializer', stripHashFromQuerySerializer);
  validateResponseHeaderNameOption('requestIdHeaderName', requestIdHeaderName);
  validateNumericOption('maxLoggedResponsePreviewChars', maxLoggedResponsePreviewChars, 1);
  validateNumericOption('maxLoggedErrorPreviewChars', maxLoggedErrorPreviewChars, 1);
  validateQueryInput(query);

  if (!VALID_RETRY_MODES.has(retryMode)) {
    throw new ApiError(
      'Invalid request configuration',
      0,
      'Client Error',
      'retryMode must be one of: auto, never, always.'
    );
  }

  if (options.responseType && !VALID_RESPONSE_TYPES.has(options.responseType)) {
    throw new ApiError(
      'Invalid request configuration',
      0,
      'Client Error',
      'responseType must be one of: json, blob, text, arrayBuffer.'
    );
  }

  if (credentialsMode && !VALID_CREDENTIAL_MODES.has(credentialsMode)) {
    throw new ApiError(
      'Invalid request configuration',
      0,
      'Client Error',
      'credentialsMode must be one of: omit, same-origin, include.'
    );
  }

  if (expectedStatus !== undefined) {
    const expectedStatuses = Array.isArray(expectedStatus) ? expectedStatus : [expectedStatus];
    for (const status of expectedStatuses) {
      validateHttpStatusCode('expectedStatus', status);
    }
  }

  if (retryableStatusCodes) {
    for (const status of retryableStatusCodes) {
      validateHttpStatusCode('retryableStatusCodes', status);
    }
  }

  const normalizedBody = normalizeRequestBody(
    requestOptions.body as BodyInit | Record<string, unknown> | null | undefined
  );
  const normalizedOptions: RequestInit = { ...requestOptions, body: normalizedBody };
  const retryLimit = maxRetries ?? STARTUP_RETRY_CONFIG.maxRetries;
  const normalizedEndpoint = normalizeEndpoint(endpoint);
  const baseApiUrl = baseUrl ? sanitizeBaseUrl(baseUrl) : API_BASE;
  const baseRequestUrl = `${baseApiUrl}${normalizedEndpoint}`;
  const url =
    query && querySerializer
      ? (() => {
          const serializedResult = querySerializer(query);
          if (typeof serializedResult !== 'string') {
            throw new ApiError(
              'Invalid request configuration',
              0,
              'Client Error',
              'querySerializer must return a string.'
            );
          }

          const serializedQuery = serializedResult.trim().replace(/^\?+/, '');
          const withoutHash = stripHashFromQuerySerializer
            ? serializedQuery.replace(/#.*/, '')
            : serializedQuery;

          return withoutHash
            ? `${baseRequestUrl}${baseRequestUrl.includes('?') ? '&' : '?'}${withoutHash}`
            : baseRequestUrl;
        })()
      : appendQueryParams(
          baseRequestUrl,
          query,
          includeEmptyStringQueryParams,
          trimStringQueryParams,
          dedupeArrayQueryParams,
          includeNullQueryParams,
          includeUndefinedQueryParams,
          sortQueryParams,
          trimQueryKeys
        );
  const retryableMethodSet = new Set(
    (retryableMethods ?? Array.from(RETRYABLE_METHODS)).map((value) => value.trim().toUpperCase())
  );
  const retryableStatusSet = new Set(retryableStatusCodes ?? Array.from(RETRYABLE_STATUS_CODES));
  const method = normalizeMethod(normalizedOptions.method);
  const sensitiveLogKeys = createSensitiveLogKeySet(additionalSensitiveLogKeys);
  const sanitizedUrl = sanitizeUrlForLogsWithKeys(url, sensitiveLogKeys);
  const requestBody = redactSensitiveValuesWithKeys(
    formatRequestBodyForLogs(normalizedBody),
    sensitiveLogKeys
  );
  const requestHeaders = buildRequestHeaders(
    normalizedOptions,
    skipDefaultAcceptHeader,
    skipDefaultContentTypeHeader
  );
  const requestId = createRequestId();
  if (!requestHeaders.has(requestIdHeaderName)) {
    requestHeaders.set(requestIdHeaderName, requestId);
  }

  const logContext: Record<string, unknown> = { body: requestBody, requestId };
  if (timeoutMs && timeoutMs > 0) {
    logContext.timeoutMs = timeoutMs;
  }
  log.debug(`${method} ${sanitizedUrl}`, logContext);

  for (let attempt = 0; attempt <= retryLimit; attempt++) {
    const signalController = createRequestSignal(timeoutMs, callerSignal);

    try {
      const nextRequestOptions = onRequest?.({ url, method, options: normalizedOptions }) ?? {};
      const mergedRequestOptions = { ...normalizedOptions, ...nextRequestOptions };
      const mergedHeaders = new Headers(requestHeaders);
      if (nextRequestOptions.headers) {
        const hookHeaders = new Headers(nextRequestOptions.headers);
        hookHeaders.forEach((value, key) => mergedHeaders.set(key, value));
      }

      const response = await fetch(url, {
        ...mergedRequestOptions,
        credentials: credentialsMode ?? 'include',
        headers: mergedHeaders,
        signal: signalController.signal
      });

      await onResponse?.(typeof response.clone === 'function' ? response.clone() : response);

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
            rawText: rawText.substring(0, maxLoggedResponsePreviewChars)
          });
        }

        // Check if backend is still starting up or unavailable - retry with exponential backoff
        const retryAfterMs = parseRetryAfterMs(response.headers.get('retry-after'));

        if (
          isBackendStartingError(errorBody, response.status) ||
          isRetryableRequest(
            method,
            response.status,
            retryMode,
            retryableMethodSet,
            retryableStatusSet
          )
        ) {
          const delay = Math.min(
            getRetryDelay(
              attempt,
              retryAfterMs,
              initialRetryDelayMs ?? STARTUP_RETRY_CONFIG.initialDelayMs,
              retryBackoffMultiplier ?? STARTUP_RETRY_CONFIG.backoffMultiplier
            ),
            maxRetryDelayMs ?? STARTUP_RETRY_CONFIG.maxDelayMs
          );
          log.info(
            `Backend is starting, retrying in ${delay}ms (attempt ${attempt + 1}/${retryLimit + 1})`,
            {
              url: sanitizedUrl,
              method,
              attempt,
              requestId
            }
          );

          // Update backend status store
          backendStatus.setStarting(attempt + 1, retryLimit + 1);
          onRetry?.({
            attempt: attempt + 1,
            maxAttempts: retryLimit + 1,
            delayMs: delay,
            status: response.status,
            reason: 'http'
          });

          if (attempt < retryLimit) {
            await sleep(delay);
            continue; // Retry the request
          }
          log.warn('Backend startup retries exhausted', {
            url: sanitizedUrl,
            method,
            attempts: retryLimit + 1,
            requestId
          });
          // Max retries reached, fall through to throw error
        }

        log.error(`${method} ${sanitizedUrl} failed`, undefined, {
          status: response.status,
          statusText: response.statusText,
          errorBody: redactSensitiveValuesWithKeys(errorBody, sensitiveLogKeys),
          rawText: rawText.substring(0, maxLoggedErrorPreviewChars),
          requestId
        });

        // Mark backend as ready if we got a response (even an error)
        backendStatus.setReady();

        if (browser && response.status >= 500 && !suppressErrorToast) {
          const msg =
            (errorBody?.message as string) ||
            (errorBody?.error as string) ||
            'An unexpected error occurred on the server.';
          toast.error(`Server Error (${response.status})`, {
            description: msg
          });
        }

        const parsedError = parseErrorResponse(
          response.status,
          response.statusText,
          errorBody,
          rawText,
          response.headers.get('x-request-id') ?? requestId
        );
        throw parsedError;
      }

      if (typeof maxResponseBytes === 'number') {
        const contentLength = Number(response.headers.get('content-length'));
        if (Number.isFinite(contentLength) && contentLength > maxResponseBytes) {
          throw new ApiError(
            'Response payload too large',
            0,
            'Client Error',
            `Response exceeded configured maxResponseBytes (${maxResponseBytes}).`
          );
        }
      }

      if (expectedStatus !== undefined) {
        const expectedList = Array.isArray(expectedStatus) ? expectedStatus : [expectedStatus];
        if (!expectedList.includes(response.status)) {
          throw new ApiError(
            'Unexpected response status',
            response.status,
            response.statusText,
            `Expected status ${expectedList.join(', ')} but received ${response.status}.`
          );
        }
      }

      if (acceptedContentTypes && acceptedContentTypes.length > 0) {
        const responseContentType = (response.headers.get('content-type') || '').toLowerCase();
        const isAccepted = acceptedContentTypes.some((contentType) =>
          responseContentType.includes(contentType.toLowerCase())
        );

        if (!isAccepted) {
          throw new ApiError(
            'Unexpected response content type',
            response.status,
            response.statusText,
            `Expected one of ${acceptedContentTypes.join(', ')} but received ${responseContentType || 'none'}.`
          );
        }
      }

      if (requiredResponseHeaders && requiredResponseHeaders.length > 0) {
        const missingHeaders = requiredResponseHeaders.filter(
          (headerName) => !response.headers.get(headerName)
        );

        if (missingHeaders.length > 0) {
          throw new ApiError(
            'Missing required response headers',
            response.status,
            response.statusText,
            `Missing headers: ${missingHeaders.join(', ')}.`
          );
        }
      }

      // Success - mark backend as ready
      backendStatus.setReady();

      // Handle empty responses (204 No Content)
      const contentLength = response.headers.get('content-length');
      if (response.status === 204 || contentLength === '0') {
        return {} as T;
      }

      const data = (
        responseParser
          ? await responseParser(response)
          : await parseResponsePayload<T>(response, method, sanitizedUrl, options.responseType)
      ) as T;

      if (typeof maxResponseBytes === 'number') {
        const serializedLength = getPayloadSizeBytes(data);
        if (serializedLength > maxResponseBytes) {
          const payloadTooLargeError = new ApiError(
            'Response payload too large',
            0,
            'Client Error',
            `Response exceeded configured maxResponseBytes (${maxResponseBytes}).`
          );
          throw payloadTooLargeError;
        }
      }

      log.debug(`${method} ${sanitizedUrl} success`, {
        data: redactSensitiveValuesWithKeys(data, sensitiveLogKeys)
      });
      return data;
    } catch (error) {
      // Re-throw ApiErrors as-is (they're already processed)
      if (error instanceof ApiError) {
        await onError?.(error);
        throw error;
      }

      if (error instanceof DOMException && error.name === 'AbortError') {
        if (signalController.wasTimedOut()) {
          const message =
            timeoutMessage ?? `The request to ${endpoint} timed out after ${timeoutMs}ms.`;
          log.warn('Request timed out', {
            url: sanitizedUrl,
            method,
            timeoutMs,
            attempt,
            requestId
          });
          backendStatus.setError('Request timeout');

          if (browser && !suppressErrorToast) {
            toast.error('Request Timed Out', { description: message });
          }

          const timeoutError = new ApiError('Request timeout', 408, 'Request Timeout', message);
          await onError?.(timeoutError);
          throw timeoutError;
        }

        log.info('Request aborted by caller', { url: sanitizedUrl, method, attempt, requestId });
        const cancelledError = new ApiError(
          'Request cancelled',
          0,
          'Aborted',
          'The request was cancelled.'
        );
        await onError?.(cancelledError);
        throw cancelledError;
      }

      // For network errors during startup, retry
      if (
        error instanceof TypeError &&
        attempt < retryLimit &&
        retryOnNetworkError &&
        isRetryableRequest(method, undefined, retryMode, retryableMethodSet, retryableStatusSet)
      ) {
        const delay = Math.min(
          getRetryDelay(
            attempt,
            undefined,
            initialRetryDelayMs ?? STARTUP_RETRY_CONFIG.initialDelayMs,
            retryBackoffMultiplier ?? STARTUP_RETRY_CONFIG.backoffMultiplier
          ) + Math.floor(Math.random() * (retryJitterMs ?? 250)),
          maxRetryDelayMs ?? STARTUP_RETRY_CONFIG.maxDelayMs
        );
        log.info(
          `Network error, backend may be starting. Retrying in ${delay}ms (attempt ${attempt + 1}/${retryLimit + 1})`,
          {
            url: sanitizedUrl,
            method,
            error: error.message,
            attempt,
            requestId
          }
        );
        backendStatus.setStarting(attempt + 1, retryLimit + 1);
        onRetry?.({
          attempt: attempt + 1,
          maxAttempts: retryLimit + 1,
          delayMs: delay,
          reason: 'network'
        });
        await sleep(delay);
        continue;
      }

      // Handle network errors with more specific messages
      if (error instanceof TypeError) {
        const isConnectionRefused =
          error.message.includes('fetch') || error.message.includes('network');
        log.error('Network error occurred', error, { url: sanitizedUrl, method, requestId });
        backendStatus.setError('Connection failed');

        const message =
          networkErrorMessage ??
          (isConnectionRefused
            ? `Unable to connect to ${url}. The server may be down or there may be a network issue.`
            : `Network error: ${error.message}`);

        if (browser && !suppressErrorToast) {
          toast.error('Connection Failed', { description: message });
        }

        const connectionError = new ApiError('Connection failed', 0, 'Network Error', message);
        await onError?.(connectionError);
        throw connectionError;
      }

      // Handle other errors
      const errorMessage = error instanceof Error ? error.message : 'Unknown error';
      log.error('Unexpected error', error, { url: sanitizedUrl, method, requestId });

      if (browser && !suppressErrorToast) {
        toast.error('Unexpected Error', { description: errorMessage });
      }

      const unknownError = new ApiError(
        'Request failed',
        0,
        'Unknown Error',
        `An unexpected error occurred: ${errorMessage}`
      );
      await onError?.(unknownError);
      throw unknownError;
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
    `Backend is still starting after ${retryLimit + 1} attempts. Please try again in a moment.`,
    undefined,
    undefined,
    requestId
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

  if (typeof FormData !== 'undefined' && body instanceof FormData) {
    return Object.fromEntries(body.entries());
  }

  return `[${body.constructor.name}]`;
}

async function parseResponsePayload<T>(
  response: Response,
  method: string,
  url: string,
  responseType?: 'json' | 'blob' | 'text' | 'arrayBuffer'
): Promise<T> {
  if (responseType === 'blob') {
    return (await response.blob()) as unknown as T;
  }

  if (responseType === 'arrayBuffer') {
    return (await response.arrayBuffer()) as unknown as T;
  }

  if (responseType === 'text') {
    return (await response.text()) as unknown as T;
  }

  const contentType = response.headers.get('content-type')?.toLowerCase() ?? '';
  const raw = await response.text();

  if (!raw.trim()) {
    log.warn(`${method} ${url} returned an empty response body for JSON payload`);
    return {} as T;
  }

  const looksJson = raw.trimStart().startsWith('{') || raw.trimStart().startsWith('[');

  // Auto-fallback to text for non-JSON payloads to better support existing endpoints.
  if (!contentType.includes('application/json') && !contentType.includes('+json') && !looksJson) {
    return raw as unknown as T;
  }

  return parseJsonPayload<T>(raw, response, method, url);
}

function parseJsonPayload<T>(raw: string, response: Response, method: string, url: string): T {
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
