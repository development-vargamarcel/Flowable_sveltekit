import * as Sentry from '@sentry/sveltekit';
import type { Handle } from '@sveltejs/kit';
import { sequence } from '@sveltejs/kit/hooks';

// Initialize Sentry for server-side error tracking
// Set PUBLIC_SENTRY_DSN environment variable to enable Sentry
const sentryDsn = process.env.PUBLIC_SENTRY_DSN;

if (sentryDsn) {
  Sentry.init({
    dsn: sentryDsn,

    // Performance Monitoring
    tracesSampleRate: 1.0, // Capture 100% of transactions

    // Environment configuration
    environment: process.env.NODE_ENV || 'development'
  });
}

// Backend URL for server-side API proxying
// For Netlify deployment: Set BACKEND_URL in Netlify dashboard (e.g., https://your-app.railway.app)
// For local development: http://localhost:8080
// Remove trailing slashes to prevent double-slash issues (e.g., //api/auth/login)
const BACKEND_URL = (process.env.BACKEND_URL || process.env.VITE_API_URL || 'http://localhost:8080').replace(/\/+$/, '');
const isDev = process.env.NODE_ENV === 'development';

// Check if we're running on Netlify (serverless functions)
const isNetlify = !!process.env.NETLIFY;

// Log backend URL at startup (helpful for debugging)
console.log(`[Hooks] Backend URL configured: ${BACKEND_URL}, isNetlify: ${isNetlify}`);

// Maximum cookie header size before we consider it too large (16KB is a safe limit)
const MAX_COOKIE_SIZE = 16 * 1024;

// Headers that should NOT be forwarded to the backend
// These are hop-by-hop headers or headers that should be recalculated
const HEADERS_TO_SKIP = new Set([
  'host', // Must be set for the backend, not the frontend
  'content-length', // Will be recalculated by fetch
  'transfer-encoding', // Hop-by-hop header
  'connection', // Hop-by-hop header
  'keep-alive', // Hop-by-hop header
  'upgrade', // Hop-by-hop header
  'proxy-authenticate', // Hop-by-hop header
  'proxy-authorization', // Hop-by-hop header
  'te', // Hop-by-hop header
  'trailer' // Hop-by-hop header
]);

/**
 * Build a redirect response to clear cookies
 */
function buildClearCookiesRedirect(): Response {
  return new Response(null, {
    status: 302,
    headers: {
      Location: '/clear-cookies',
      'Set-Cookie': [
        'JSESSIONID=; Path=/; Max-Age=0; HttpOnly; SameSite=Lax',
        'JSESSIONID=; Path=/api; Max-Age=0; HttpOnly; SameSite=Lax',
        'SESSION=; Path=/; Max-Age=0; HttpOnly; SameSite=Lax'
      ].join(', '),
      'Cache-Control': 'no-cache, no-store, must-revalidate'
    }
  });
}

/**
 * Build a consistent JSON error response
 */
function buildErrorResponse(
  error: string,
  message: string,
  details?: string,
  status?: number
): Response {
  const body = {
    error,
    message,
    details: details || message,
    timestamp: new Date().toISOString(),
    ...(status && { status })
  };

  return new Response(JSON.stringify(body), {
    status: status || 500,
    headers: { 'Content-Type': 'application/json' }
  });
}

const customHandle: Handle = async ({ event, resolve }) => {
  // Check if cookie header is too large and redirect to clear cookies page
  // This prevents the "Request headers too large" error
  const cookieHeader = event.request.headers.get('cookie') || '';
  if (cookieHeader.length > MAX_COOKIE_SIZE) {
    console.warn(
      `[Hooks] Cookie header too large (${cookieHeader.length} bytes), redirecting to clear cookies`
    );
    // Don't redirect for clear-cookies page itself to avoid infinite loop
    if (event.url.pathname !== '/clear-cookies') {
      return buildClearCookiesRedirect();
    }
  }

  // Proxy /api/* requests to the backend
  if (event.url.pathname.startsWith('/api/')) {
    const backendUrl = `${BACKEND_URL}${event.url.pathname}${event.url.search}`;
    const method = event.request.method;

    if (isDev) {
      console.log(`[Proxy] ${method} ${event.url.pathname} -> ${backendUrl}`);
    }

    try {
      // Read the request body for non-GET/HEAD requests
      let requestBody: string | undefined;
      if (method !== 'GET' && method !== 'HEAD') {
        try {
          requestBody = await event.request.text();
          if (isDev && requestBody) {
            console.log(`[Proxy] Request body:`, requestBody.substring(0, 200));
          }
        } catch (bodyError) {
          console.error('[Proxy] Failed to read request body:', bodyError);
          return buildErrorResponse(
            'Failed to read request body',
            'The request body could not be read',
            bodyError instanceof Error ? bodyError.message : 'Unknown error',
            400
          );
        }
      }

      // Build headers, filtering out problematic ones
      const forwardHeaders = new Headers();
      event.request.headers.forEach((value, key) => {
        if (!HEADERS_TO_SKIP.has(key.toLowerCase())) {
          forwardHeaders.set(key, value);
        }
      });

      // Ensure Content-Type and Accept headers are set for JSON requests
      if (requestBody) {
        if (!forwardHeaders.has('content-type')) {
          forwardHeaders.set('Content-Type', 'application/json');
        }
      }
      if (!forwardHeaders.has('accept')) {
        forwardHeaders.set('Accept', 'application/json');
      }

      const response = await fetch(backendUrl, {
        method,
        headers: forwardHeaders,
        body: requestBody,
        // Don't follow redirects - let client handle them
        redirect: 'manual'
      });

      if (isDev) {
        console.log(
          `[Proxy] ${method} ${event.url.pathname} -> ${response.status} ${response.statusText}`
        );
      }

      // Forward the response back to the client
      const responseHeaders = new Headers();
      response.headers.forEach((value, key) => {
        // Forward all headers except hop-by-hop headers
        if (!['transfer-encoding', 'content-encoding', 'connection'].includes(key.toLowerCase())) {
          responseHeaders.set(key, value);
        }
      });

      // For error responses, try to enhance them with more details
      if (!response.ok && isDev) {
        try {
          const text = await response.text();
          console.log(`[Proxy] Error response body:`, text.substring(0, 500));
          // Return the response with the read body
          return new Response(text, {
            status: response.status,
            statusText: response.statusText,
            headers: responseHeaders
          });
        } catch {
          // Couldn't read body, just forward the original response
        }
      }

      return new Response(response.body, {
        status: response.status,
        statusText: response.statusText,
        headers: responseHeaders
      });
    } catch (error) {
      console.error('[Proxy] API proxy error:', error);

      // Provide detailed error messages based on error type
      let errorTitle = 'Backend unavailable';
      let errorMessage = 'Could not connect to the backend server';
      let errorDetails = '';

      if (error instanceof Error) {
        errorDetails = error.message;

        if (error.message.includes('ECONNREFUSED')) {
          errorTitle = 'Connection refused';
          errorMessage = 'Cannot connect to backend server';
          errorDetails = `Backend at ${BACKEND_URL} is not responding. Please ensure the backend is running.`;
        } else if (error.message.includes('ETIMEDOUT') || error.message.includes('timeout')) {
          errorTitle = 'Connection timed out';
          errorMessage = 'Backend connection timed out';
          errorDetails = 'The backend server took too long to respond. Please try again.';
        } else if (error.message.includes('ENOTFOUND')) {
          errorTitle = 'Host not found';
          errorMessage = 'Backend server not found';
          errorDetails = `Cannot resolve backend host. Check BACKEND_URL configuration: ${BACKEND_URL}`;
        } else if (error.message.includes('ECONNRESET')) {
          errorTitle = 'Connection reset';
          errorMessage = 'Connection to backend was reset';
          errorDetails = 'The backend server closed the connection unexpectedly.';
        } else if (error.message.includes('fetch')) {
          errorTitle = 'Network error';
          errorMessage = 'Failed to fetch from backend';
          errorDetails = `Network error when connecting to ${BACKEND_URL}: ${error.message}`;
        }
      }

      return buildErrorResponse(errorTitle, errorMessage, errorDetails, 502);
    }
  }

  return resolve(event);
};

// Combine Sentry's handle with our custom handle
export const handle = sequence(Sentry.sentryHandle(), customHandle);

// Export Sentry's handleError hook
export const handleError = Sentry.handleErrorWithSentry();
