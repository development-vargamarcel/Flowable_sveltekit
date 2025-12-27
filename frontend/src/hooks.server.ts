import type { Handle } from '@sveltejs/kit';

// Backend URL for server-side API proxying
// In docker-compose: http://backend:8080
// In production with nginx: requests don't reach here (nginx handles /api/)
// Fallback: http://localhost:8080 for local development
const BACKEND_URL = process.env.BACKEND_URL || process.env.VITE_API_URL || 'http://localhost:8080';

export const handle: Handle = async ({ event, resolve }) => {
	// Proxy /api/* requests to the backend
	if (event.url.pathname.startsWith('/api/')) {
		const backendUrl = `${BACKEND_URL}${event.url.pathname}${event.url.search}`;

		try {
			const response = await fetch(backendUrl, {
				method: event.request.method,
				headers: event.request.headers,
				body: event.request.method !== 'GET' && event.request.method !== 'HEAD'
					? await event.request.text()
					: undefined,
				// Don't follow redirects - let client handle them
				redirect: 'manual'
			});

			// Forward the response back to the client
			const responseHeaders = new Headers();
			response.headers.forEach((value, key) => {
				// Forward all headers except some that shouldn't be proxied
				if (!['transfer-encoding', 'content-encoding'].includes(key.toLowerCase())) {
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
			return new Response(JSON.stringify({ error: 'Backend unavailable' }), {
				status: 502,
				headers: { 'Content-Type': 'application/json' }
			});
		}
	}

	return resolve(event);
};
