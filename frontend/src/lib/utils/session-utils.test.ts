import { beforeEach, describe, expect, it, vi } from 'vitest';
import { api } from '$lib/api/client';
import {
  checkBackendHealth,
  clearAllCookies,
  getCookieDiagnostics,
  isHeaderTooLargeError
} from './session-utils';

vi.mock('$lib/api/client', () => ({
  api: {
    clearSession: vi.fn()
  }
}));

describe('session-utils', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  describe('getCookieDiagnostics', () => {
    it('limits cookie names when maxNames is provided', () => {
      const cookieDescriptor = Object.getOwnPropertyDescriptor(Document.prototype, 'cookie');
      Object.defineProperty(document, 'cookie', {
        configurable: true,
        get: () => 'a=1; b=2; c=3; d=4',
        set: () => undefined
      });

      const diagnostics = getCookieDiagnostics(2);
      expect(diagnostics).toContain('a, b (+2 more)');

      if (cookieDescriptor) {
        Object.defineProperty(document, 'cookie', cookieDescriptor);
      }
    });

    it('guards against invalid maxNames and includes payload warning when cookies are large', () => {
      const cookieDescriptor = Object.getOwnPropertyDescriptor(Document.prototype, 'cookie');
      Object.defineProperty(document, 'cookie', {
        configurable: true,
        get: () => `token=${'x'.repeat(4200)}; a=1`,
        set: () => undefined
      });

      const diagnostics = getCookieDiagnostics(Number.NaN);
      expect(diagnostics).toContain('Potential risk');
      expect(diagnostics).toContain('token, a');

      if (cookieDescriptor) {
        Object.defineProperty(document, 'cookie', cookieDescriptor);
      }
    });
  });

  describe('checkBackendHealth', () => {
    it('returns true when health and ready endpoints are successful', async () => {
      const fetchFn = vi
        .fn()
        .mockResolvedValueOnce({ ok: true, status: 200 })
        .mockResolvedValueOnce({ ok: true, status: 200 });

      await expect(checkBackendHealth(1, { fetchFn: fetchFn as any, delaysMs: [0] })).resolves.toBe(
        true
      );
      expect(fetchFn).toHaveBeenCalledTimes(2);
    });

    it('retries when ready endpoint returns 503', async () => {
      const fetchFn = vi
        .fn()
        .mockResolvedValueOnce({ ok: true, status: 200 })
        .mockResolvedValueOnce({ ok: false, status: 503 })
        .mockResolvedValueOnce({ ok: true, status: 200 })
        .mockResolvedValueOnce({ ok: true, status: 200 });

      await expect(
        checkBackendHealth(2, { fetchFn: fetchFn as any, delaysMs: [0, 0] })
      ).resolves.toBe(true);
      expect(fetchFn).toHaveBeenCalledTimes(4);
    });

    it('handles timeout-style failures and keeps retrying', async () => {
      const fetchFn = vi
        .fn()
        .mockRejectedValueOnce(new DOMException('timeout', 'AbortError'))
        .mockResolvedValueOnce({ ok: true, status: 200 })
        .mockResolvedValueOnce({ ok: true, status: 200 });

      await expect(
        checkBackendHealth(2, { fetchFn: fetchFn as typeof fetch, delaysMs: [0, 0] })
      ).resolves.toBe(true);
      expect(fetchFn).toHaveBeenCalledTimes(3);
    });
  });

  describe('clearAllCookies', () => {
    it('clears browser cookies and reports backend readiness when fallback succeeds', async () => {
      const cookieDescriptor = Object.getOwnPropertyDescriptor(Document.prototype, 'cookie');
      Object.defineProperty(document, 'cookie', {
        configurable: true,
        get: () => 'session=1; preference=2',
        set: () => undefined
      });

      const fetchFn = vi
        .fn()
        .mockResolvedValueOnce({ ok: true, json: async () => ({ details: 'ok from fallback' }) })
        .mockResolvedValueOnce({ ok: true, status: 200 })
        .mockResolvedValueOnce({ ok: true, status: 200 });

      const result = await clearAllCookies({ fetchFn: fetchFn as any, skipRedirect: true });
      expect(result.cookiesCleared).toBe(true);
      expect(result.diagnostics).toContain('ok from fallback');
      expect(result.backendReady).toBe(true);

      if (cookieDescriptor) {
        Object.defineProperty(document, 'cookie', cookieDescriptor);
      }
    });

    it('falls back to backend endpoint when fallback endpoint fails', async () => {
      (api.clearSession as ReturnType<typeof vi.fn>).mockResolvedValue({
        details: 'backend cleared'
      });

      const fetchFn = vi
        .fn()
        .mockRejectedValueOnce(new Error('network'))
        .mockResolvedValueOnce({ ok: true, status: 200 })
        .mockResolvedValueOnce({ ok: true, status: 200 });

      const result = await clearAllCookies({ fetchFn: fetchFn as any, skipRedirect: true });
      expect(result.cookiesCleared).toBe(true);
      expect(result.diagnostics).toContain('backend cleared');
      expect(api.clearSession).toHaveBeenCalledTimes(1);
    });

    it('does not duplicate clear operations for repeated cookie names', async () => {
      const writes: string[] = [];
      const cookieDescriptor = Object.getOwnPropertyDescriptor(Document.prototype, 'cookie');
      Object.defineProperty(document, 'cookie', {
        configurable: true,
        get: () => 'session=1; session=2; preference=3',
        set: (value: string) => {
          writes.push(value);
        }
      });

      const fetchFn = vi
        .fn()
        .mockResolvedValueOnce({ ok: true, json: async () => ({ details: 'ok from fallback' }) })
        .mockResolvedValueOnce({ ok: true, status: 200 })
        .mockResolvedValueOnce({ ok: true, status: 200 });

      await clearAllCookies({ fetchFn: fetchFn as typeof fetch, skipRedirect: true });

      // There are 18 cookie invalidation writes per cookie name (3 paths × 3 domains × 2 directives).
      expect(writes.length).toBe(36);

      if (cookieDescriptor) {
        Object.defineProperty(document, 'cookie', cookieDescriptor);
      }
    });
  });

  describe('isHeaderTooLargeError', () => {
    it('detects status code and header-too-large variants', () => {
      expect(isHeaderTooLargeError('Bad Request', 'status: 431')).toBe(true);
      expect(isHeaderTooLargeError('Request failed', 'random detail')).toBe(false);
    });
  });
});
