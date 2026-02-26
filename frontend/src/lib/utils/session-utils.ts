import { api } from '$lib/api/client';
import { logger } from '$lib/utils/logger';

const SESSION_LOGGER = logger.child({ module: 'session-utils' });
const DEFAULT_HEALTH_TIMEOUT_MS = 4000;
const DEFAULT_HEALTH_DELAYS_MS = [1000, 2000, 3000];
const LARGE_COOKIE_WARNING_BYTES = 4096;
const HEADER_TOO_LARGE_PATTERNS = [
  /header.*too large/i,
  /cookie.*too large/i,
  /request header or cookie too large/i,
  /request entity too large/i,
  /request-uri too long/i,
  /413 request entity/i,
  /431 request header/i,
  /header fields too large/i,
  /bad request.*header/i,
  /header.*overflow/i,
  /status\s*:?\s*431/i,
  /status\s*:?\s*413/i
];

interface FetchLike {
  (input: RequestInfo | URL, init?: RequestInit): Promise<Response>;
}

interface HealthCheckOptions {
  fetchFn?: FetchLike;
  timeoutMs?: number;
  delaysMs?: number[];
  healthEndpoint?: string;
  readyEndpoint?: string;
}

interface ClearCookiesOptions {
  fetchFn?: FetchLike;
  skipRedirect?: boolean;
}

interface ClearSessionAttempt {
  strategy: 'fallback-endpoint' | 'backend-endpoint' | 'redirect';
  success: boolean;
  details: string;
}

/**
 * Get diagnostic information about current cookies.
 */
export function getCookieDiagnostics(maxNames = 10): string {
  if (typeof document === 'undefined') return 'Cookie diagnostics not available during SSR';

  const safeMaxNames = Number.isFinite(maxNames) ? Math.max(0, Math.floor(maxNames)) : 10;

  const cookies = document.cookie
    .split(';')
    .map((cookie) => cookie.trim())
    .filter(Boolean);

  const totalSize = document.cookie.length;
  const names = Array.from(
    new Set(cookies.map((cookie) => cookie.split('=')[0]?.trim()).filter(Boolean) as string[])
  );
  const limitedNames = names.slice(0, safeMaxNames);
  const suffix =
    names.length > limitedNames.length ? ` (+${names.length - limitedNames.length} more)` : '';
  const possibleHeaderWarning =
    totalSize > LARGE_COOKIE_WARNING_BYTES
      ? ` Potential risk: browser cookie payload is above ${LARGE_COOKIE_WARNING_BYTES} bytes.`
      : '';

  return `Found ${cookies.length} accessible cookies (${totalSize} bytes): ${limitedNames.join(', ') || 'none'}${suffix}. Note: HttpOnly cookies (like JSESSIONID) are not visible to JavaScript.${possibleHeaderWarning}`;
}

async function fetchWithTimeout(
  fetchFn: FetchLike,
  input: RequestInfo | URL,
  init: RequestInit,
  timeoutMs: number
): Promise<Response> {
  const controller = new AbortController();
  const timeout = setTimeout(() => controller.abort(), timeoutMs);

  try {
    return await fetchFn(input, { ...init, signal: controller.signal });
  } finally {
    clearTimeout(timeout);
  }
}

async function safeReadJson(response: Response): Promise<Record<string, unknown>> {
  try {
    return (await response.json()) as Record<string, unknown>;
  } catch {
    return {};
  }
}

function resolveDelays(retries: number, providedDelays?: number[]): number[] {
  if (providedDelays && providedDelays.length > 0) {
    const sanitizedDelays = providedDelays
      .map((delay) => (Number.isFinite(delay) ? Math.max(0, Math.floor(delay)) : 0))
      .slice(0, retries);

    if (sanitizedDelays.length > 0) {
      return sanitizedDelays;
    }
  }

  if (retries <= DEFAULT_HEALTH_DELAYS_MS.length) {
    return DEFAULT_HEALTH_DELAYS_MS.slice(0, retries);
  }

  const lastDefaultDelay = DEFAULT_HEALTH_DELAYS_MS[DEFAULT_HEALTH_DELAYS_MS.length - 1];
  return Array.from(
    { length: retries },
    (_, idx) => DEFAULT_HEALTH_DELAYS_MS[idx] ?? lastDefaultDelay
  );
}

/**
 * Check if the backend is healthy and ready to accept requests.
 * Retries with exponential-style delay defaults.
 */
export async function checkBackendHealth(
  retries = 3,
  options: HealthCheckOptions = {}
): Promise<boolean> {
  const fetchFn = options.fetchFn ?? fetch;
  const timeoutMs = options.timeoutMs ?? DEFAULT_HEALTH_TIMEOUT_MS;
  const healthEndpoint = options.healthEndpoint ?? '/health';
  const readyEndpoint = options.readyEndpoint ?? '/ready';
  const retryCount = Math.max(1, retries);
  const delays = resolveDelays(retryCount, options.delaysMs);

  for (let i = 0; i < retryCount; i++) {
    try {
      const response = await fetchWithTimeout(
        fetchFn,
        healthEndpoint,
        { method: 'GET', headers: { Accept: 'application/json' } },
        timeoutMs
      );

      if (response.ok) {
        const readyResponse = await fetchWithTimeout(
          fetchFn,
          readyEndpoint,
          { method: 'GET', headers: { Accept: 'application/json' } },
          timeoutMs
        );

        if (readyResponse.ok) {
          return true;
        }

        if (readyResponse.status === 503) {
          SESSION_LOGGER.info('Backend not ready yet, retrying health check', {
            attempt: i + 1,
            retryCount,
            readyStatus: readyResponse.status
          });
        }
      } else {
        SESSION_LOGGER.warn('Backend health endpoint returned non-success status', {
          attempt: i + 1,
          retryCount,
          healthStatus: response.status
        });
      }
    } catch (error) {
      const isAbort = error instanceof DOMException && error.name === 'AbortError';
      SESSION_LOGGER.warn('Backend health check failed', {
        attempt: i + 1,
        retryCount,
        timeoutMs,
        reason: isAbort ? 'timeout' : 'request-failure',
        error
      });
    }

    if (i < retryCount - 1) {
      await new Promise((resolve) => setTimeout(resolve, delays[i] ?? 1000));
    }
  }

  return false;
}

function clearCookieByName(name: string, hostname: string): void {
  const paths = ['/', '/api', '/api/'];
  const domains = ['', hostname, `.${hostname}`];
  const encodedName = encodeURIComponent(name);

  for (const path of paths) {
    for (const domain of domains) {
      const domainPart = domain ? `;domain=${domain}` : '';
      document.cookie = `${encodedName}=;expires=Thu, 01 Jan 1970 00:00:00 GMT;path=${path}${domainPart}`;
      document.cookie = `${encodedName}=;max-age=0;path=${path}${domainPart}`;
    }
  }
}

function performRedirect(skipRedirect: boolean): void {
  if (skipRedirect) return;

  // Guard against accidental redirect loops while still allowing manual recovery.
  if (sessionStorage.getItem('cookie-clear-redirected') === 'true') {
    SESSION_LOGGER.warn('Skipping duplicate clear-cookie redirect to avoid loop');
    return;
  }

  sessionStorage.setItem('cookie-clear-redirected', 'true');
  window.location.href = '/clear-cookies';
}

async function tryClearServerSession(fetchFn: FetchLike): Promise<ClearSessionAttempt> {
  try {
    const fallbackResponse = await fetchFn('/api/auth/clear-session-fallback', {
      method: 'POST',
      credentials: 'include',
      headers: { Accept: 'application/json' }
    });

    if (fallbackResponse.ok) {
      const payload = await safeReadJson(fallbackResponse);
      return {
        strategy: 'fallback-endpoint',
        success: true,
        details: (payload.details as string) || 'Session cleared via nginx fallback'
      };
    }

    SESSION_LOGGER.warn('Fallback endpoint returned non-success status', {
      status: fallbackResponse.status
    });
  } catch (fallbackError) {
    SESSION_LOGGER.warn('Fallback clear-session endpoint failed', { error: fallbackError });
  }

  try {
    const result = await api.clearSession();
    return {
      strategy: 'backend-endpoint',
      success: true,
      details: result.details || 'Session cleared via backend endpoint'
    };
  } catch (backendError) {
    SESSION_LOGGER.warn('Backend clear-session endpoint failed', { error: backendError });
    return {
      strategy: 'redirect',
      success: false,
      details:
        'Unable to clear server session through available endpoints; falling back to redirect'
    };
  }
}

/**
 * Result of a clear cookies operation.
 */
export interface ClearCookiesResult {
  cookiesCleared: boolean;
  diagnostics: string;
  backendReady: boolean;
}

/**
 * Clears all cookies for the current domain to resolve large-header/cookie failures.
 * This clears JavaScript-accessible cookies and attempts to clear HttpOnly server session cookies.
 */
export async function clearAllCookies(
  options: ClearCookiesOptions = {}
): Promise<ClearCookiesResult> {
  if (typeof document === 'undefined' || typeof window === 'undefined') {
    return { cookiesCleared: false, diagnostics: '', backendReady: false };
  }

  const fetchFn = options.fetchFn ?? fetch;

  try {
    const cookies = document.cookie
      .split(';')
      .map((cookie) => cookie.trim())
      .filter(Boolean);
    const uniqueCookieNames = Array.from(
      new Set(
        cookies
          .map((cookie) => {
            const eqPos = cookie.indexOf('=');
            return eqPos > -1 ? cookie.substring(0, eqPos).trim() : cookie;
          })
          .filter(Boolean)
      )
    );
    let clearedCount = 0;

    for (const name of uniqueCookieNames) {
      if (!name) continue;
      clearCookieByName(name, window.location.hostname);
      clearedCount++;
    }

    const sessionResult = await tryClearServerSession(fetchFn);
    if (!sessionResult.success && sessionResult.strategy === 'redirect') {
      performRedirect(options.skipRedirect === true);
      return { cookiesCleared: false, diagnostics: 'Redirecting...', backendReady: false };
    }

    const backendReady = await checkBackendHealth(3, { fetchFn });
    const readinessMessage = backendReady
      ? 'Backend health check succeeded.'
      : 'Warning: Backend may still be starting up. Please wait before retrying.';

    return {
      cookiesCleared: true,
      diagnostics: `Cleared ${clearedCount} browser cookies. ${sessionResult.details} ${readinessMessage}`,
      backendReady
    };
  } catch (error) {
    SESSION_LOGGER.error('Unexpected error while clearing cookies', error);
    performRedirect(options.skipRedirect === true);
    return { cookiesCleared: false, diagnostics: 'Redirecting...', backendReady: false };
  }
}

/**
 * Checks if the error is related to request headers or cookies being too large.
 */
export function isHeaderTooLargeError(
  errorMessage: string,
  details: string,
  rawText?: string
): boolean {
  const combined = `${errorMessage} ${details} ${rawText || ''}`;
  return HEADER_TOO_LARGE_PATTERNS.some((pattern) => pattern.test(combined));
}
