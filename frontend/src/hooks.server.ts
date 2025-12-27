import type { Handle } from '@sveltejs/kit';

// Backend URL for server-side API proxying
// In docker-compose: http://backend:8080
// In production with nginx: requests don't reach here (nginx handles /api/)
// Fallback: http://localhost:8080 for local development
const BACKEND_URL = process.env.BACKEND_URL || process.env.VITE_API_URL || 'http://localhost:8080';

// Headers that should NOT be forwarded to the backend
// These are hop-by-hop headers or headers that should be recalculated
const HEADERS_TO_SKIP = new Set([
	'host',                  // Must be set for the backend, not the frontend
	'content-length',        // Will be recalculated by fetch
	'transfer-encoding',     // Hop-by-hop header
	'connection',            // Hop-by-hop header
	'keep-alive',            // Hop-by-hop header
	'upgrade',               // Hop-by-hop header
	'proxy-authenticate',    // Hop-by-hop header
	'proxy-authorization',   // Hop-by-hop header
	'te',                    // Hop-by-hop header
	'trailer',               // Hop-by-hop header
]);

export const handle: Handle = async ({ event, resolve }) => {
	// Proxy /api/* requests to the backend
	if (event.url.pathname.startsWith('/api/')) {
		const backendUrl = `${BACKEND_URL}${event.url.pathname}${event.url.search}`;

		try {
			// Read the request body for non-GET/HEAD requests
			let requestBody: string | undefined;
			if (event.request.method !== 'GET' && event.request.method !== 'HEAD') {
				try {
					requestBody = await event.request.text();
				} catch (bodyError) {
					console.error('Failed to read request body:', bodyError);
					return new Response(JSON.stringify({
						error: 'Failed to read request body',
						details: bodyError instanceof Error ? bodyError.message : 'Unknown error'
					}), {
						status: 400,
						headers: { 'Content-Type': 'application/json' }
					});
				}
			}

			// Build headers, filtering out problematic ones
			const forwardHeaders = new Headers();
			event.request.headers.forEach((value, key) => {
				if (!HEADERS_TO_SKIP.has(key.toLowerCase())) {
					forwardHeaders.set(key, value);
				}
			});

			// Ensure Content-Type is set for JSON requests
			if (requestBody && !forwardHeaders.has('content-type')) {
				forwardHeaders.set('Content-Type', 'application/json');
			}

			const response = await fetch(backendUrl, {
				method: event.request.method,
				headers: forwardHeaders,
				body: requestBody,
				// Don't follow redirects - let client handle them
				redirect: 'manual'
			});

			// Forward the response back to the client
			const responseHeaders = new Headers();
			response.headers.forEach((value, key) => {
				// Forward all headers except hop-by-hop headers
				if (!['transfer-encoding', 'content-encoding', 'connection'].includes(key.toLowerCase())) {
					responseHeaders.set(key, value);
				}
			});

			return new Response(response.body, {
				status: response.status,
				statusText: response.statusText,
				headers: responseHeaders
			});
		} catch (error) {
			console.error('API proxy error:', error);

			// Provide more detailed error messages
			let errorMessage = 'Backend unavailable';
			let errorDetails = '';

			if (error instanceof Error) {
				errorDetails = error.message;
				if (error.message.includes('ECONNREFUSED')) {
					errorMessage = 'Cannot connect to backend server';
					errorDetails = `Backend at ${BACKEND_URL} is not responding. Please ensure the backend is running.`;
				} else if (error.message.includes('ETIMEDOUT')) {
					errorMessage = 'Backend connection timed out';
					errorDetails = 'The backend server took too long to respond.';
				} else if (error.message.includes('ENOTFOUND')) {
					errorMessage = 'Backend server not found';
					errorDetails = `Cannot resolve backend host. Check BACKEND_URL configuration: ${BACKEND_URL}`;
				}
			}

			return new Response(JSON.stringify({
				error: errorMessage,
				details: errorDetails,
				timestamp: new Date().toISOString()
			}), {
				status: 502,
				headers: { 'Content-Type': 'application/json' }
			});
		}
	}

	return resolve(event);
};
