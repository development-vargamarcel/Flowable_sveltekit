import { api } from '$lib/api/client';

/**
 * Get diagnostic information about current cookies
 */
export function getCookieDiagnostics(): string {
    if (typeof document === 'undefined') return 'Cookie diagnostics not available during SSR';
    const cookies = document.cookie.split(';').filter(c => c.trim());
    const totalSize = document.cookie.length;
    const cookieNames = cookies.map(c => c.split('=')[0].trim()).join(', ');
    return `Found ${cookies.length} accessible cookies (${totalSize} bytes): ${cookieNames || 'none'}. Note: HttpOnly cookies (like JSESSIONID) are not visible to JavaScript.`;
}

/**
 * Check if the backend is healthy and ready to accept requests.
 * Retries up to 3 times with increasing delays.
 */
export async function checkBackendHealth(retries = 3): Promise<boolean> {
    for (let i = 0; i < retries; i++) {
        try {
            // Try the health endpoint first
            const response = await fetch('/health', {
                method: 'GET',
                headers: { 'Accept': 'application/json' }
            });

            if (response.ok) {
                // Also try the ready endpoint to check backend specifically
                const readyResponse = await fetch('/ready', {
                    method: 'GET',
                    headers: { 'Accept': 'application/json' }
                });

                if (readyResponse.ok) {
                    return true;
                }

                // If ready returns 503 (backend starting), wait and retry
                if (readyResponse.status === 503) {
                    // eslint-disable-next-line no-console
                    console.log(`Backend not ready yet, attempt ${i + 1}/${retries}...`);
                    await new Promise(resolve => setTimeout(resolve, 1000 * (i + 1)));
                    continue;
                }
            }
        } catch (e) {
            // eslint-disable-next-line no-console
            console.log(`Health check failed, attempt ${i + 1}/${retries}:`, e);
            await new Promise(resolve => setTimeout(resolve, 1000 * (i + 1)));
        }
    }
    return false;
}

/**
 * Result of a clear cookies operation
 */
export interface ClearCookiesResult {
    cookiesCleared: boolean;
    diagnostics: string;
    backendReady: boolean;
}

/**
 * Clears all cookies for the current domain to resolve "Request Header Or Cookie Too Large" errors.
 * This clears both JavaScript-accessible cookies and calls the backend to clear HttpOnly cookies.
 * Uses multiple fallback strategies when headers are too large.
 */
export async function clearAllCookies(): Promise<ClearCookiesResult> {
    if (typeof document === 'undefined' || typeof window === 'undefined') {
        return { cookiesCleared: false, diagnostics: '', backendReady: false };
    }

    let cookieDiagnostics = '';
    let backendReady = false;

    try {
        // First, clear JavaScript-accessible cookies locally
        const cookies = document.cookie.split(';');
        let clearedCount = 0;
        for (const cookie of cookies) {
            const eqPos = cookie.indexOf('=');
            const name = eqPos > -1 ? cookie.substring(0, eqPos).trim() : cookie.trim();
            if (name) {
                // Clear the cookie with various path and domain combinations
                const paths = ['/', '/api', '/api/'];
                const domains = ['', window.location.hostname];
                for (const path of paths) {
                    for (const domain of domains) {
                        const domainPart = domain ? `;domain=${domain}` : '';
                        document.cookie = `${name}=;expires=Thu, 01 Jan 1970 00:00:00 GMT;path=${path}${domainPart}`;
                        document.cookie = `${name}=;max-age=0;path=${path}${domainPart}`;
                    }
                }
                clearedCount++;
            }
        }
        // eslint-disable-next-line no-console
        console.log(`Cleared ${clearedCount} JavaScript-accessible cookies`);

        // Try to clear HttpOnly cookies (like JSESSIONID) using multiple strategies
        let serverCleared = false;
        let clearDetails = '';

        // Strategy 1: Try the nginx fallback endpoint FIRST (more reliable when headers are large)
        // This endpoint uses credentials: 'include' but nginx will set Set-Cookie headers to clear
        try {
            const fallbackResponse = await fetch('/api/auth/clear-session-fallback', {
                method: 'POST',
                credentials: 'include', // Send cookies so browser associates the Set-Cookie response
                headers: {
                    'Accept': 'application/json'
                }
            });

            if (fallbackResponse.ok) {
                const result = await fallbackResponse.json();
                // eslint-disable-next-line no-console
                console.log('Fallback session clear succeeded:', result);
                serverCleared = true;
                clearDetails = result.details || 'Session cleared via nginx fallback';
            } else {
                console.warn('Fallback endpoint returned error:', fallbackResponse.status);
            }
        } catch (fallbackError) {
            console.warn('Fallback clear failed, trying backend:', fallbackError);

            // Strategy 2: Try the normal backend endpoint
            try {
                const result = await api.clearSession();
                // eslint-disable-next-line no-console
                console.log('Backend session cleared:', result);
                serverCleared = true;
                clearDetails = result.details;
            } catch (backendError) {
                console.warn('Backend clear-session also failed:', backendError);

                // Strategy 3: Redirect to the static clear-cookies page
                // eslint-disable-next-line no-console
                console.log('Redirecting to /clear-cookies page as last resort');
                window.location.href = '/clear-cookies';
                return { cookiesCleared: false, diagnostics: 'Redirecting...', backendReady: false };
            }
        }

        if (serverCleared) {
            cookieDiagnostics = `Cleared ${clearedCount} browser cookies and server session. ${clearDetails}`;
        } else {
            cookieDiagnostics = `Cleared ${clearedCount} browser cookies. Server session cookies will expire automatically. Please refresh the page and try again.`;
        }

        // Check if backend is healthy before telling user to proceed
        try {
            backendReady = await checkBackendHealth(3);
            if (!backendReady) {
                cookieDiagnostics += ' Warning: Backend may still be starting up. Please wait a moment before trying again.';
            }
        } catch (e) {
            // ignore
        }

        return { cookiesCleared: true, diagnostics: cookieDiagnostics, backendReady };

    } catch (e) {
        console.error('Error clearing cookies:', e);
        // As a last resort, redirect to clear-cookies page
        // eslint-disable-next-line no-console
        console.log('Redirecting to /clear-cookies page after error');
        window.location.href = '/clear-cookies';
        return { cookiesCleared: false, diagnostics: 'Redirecting...', backendReady: false };
    }
}

/**
 * Checks if the error is related to request headers or cookies being too large.
 * Detects various error message formats from nginx, proxies, and browsers.
 */
export function isHeaderTooLargeError(errorMessage: string, details: string, rawText?: string): boolean {
    const combined = `${errorMessage} ${details} ${rawText || ''}`.toLowerCase();

    // Check for common nginx/proxy error patterns
    const patterns = [
        'header.*too large',
        'cookie.*too large',
        'request header or cookie too large',
        'request entity too large',
        'request-uri too long',
        '413 request entity',
        '431 request header',
        'header fields too large',
        'bad request.*header',
        'header.*overflow'
    ];

    return patterns.some(pattern => new RegExp(pattern).test(combined));
}
