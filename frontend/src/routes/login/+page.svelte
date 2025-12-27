<script lang="ts">
	import { goto } from '$app/navigation';
	import { api, ApiError } from '$lib/api/client';
	import { authStore } from '$lib/stores/auth.svelte';

	let username = $state('');
	let password = $state('');
	let error = $state('');
	let errorDetails = $state('');
	let errorStatus = $state(0);
	let fieldErrors = $state<Record<string, string>>({});
	let loading = $state(false);
	let cookiesCleared = $state(false);
	let clearingCookies = $state(false);
	let cookieDiagnostics = $state('');
	let backendReady = $state(false);
	let checkingBackend = $state(false);

	/**
	 * Get diagnostic information about current cookies
	 */
	function getCookieDiagnostics(): string {
		const cookies = document.cookie.split(';').filter(c => c.trim());
		const totalSize = document.cookie.length;
		const cookieNames = cookies.map(c => c.split('=')[0].trim()).join(', ');
		return `Found ${cookies.length} accessible cookies (${totalSize} bytes): ${cookieNames || 'none'}. Note: HttpOnly cookies (like JSESSIONID) are not visible to JavaScript.`;
	}

	/**
	 * Check if the backend is healthy and ready to accept requests.
	 * Retries up to 3 times with increasing delays.
	 */
	async function checkBackendHealth(retries = 3): Promise<boolean> {
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
	 * Clears all cookies for the current domain to resolve "Request Header Or Cookie Too Large" errors.
	 * This clears both JavaScript-accessible cookies and calls the backend to clear HttpOnly cookies.
	 * Uses multiple fallback strategies when headers are too large.
	 */
	async function clearAllCookies(): Promise<void> {
		clearingCookies = true;
		error = '';
		errorDetails = '';
		errorStatus = 0;

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
					return;
				}
			}

			cookiesCleared = true;
			if (serverCleared) {
				cookieDiagnostics = `Cleared ${clearedCount} browser cookies and server session. ${clearDetails}`;
			} else {
				cookieDiagnostics = `Cleared ${clearedCount} browser cookies. Server session cookies will expire automatically. Please refresh the page and try again.`;
			}

			// Check if backend is healthy before telling user to proceed
			checkingBackend = true;
			try {
				backendReady = await checkBackendHealth(3);
				if (!backendReady) {
					cookieDiagnostics += ' Warning: Backend may still be starting up. Please wait a moment before trying again.';
				}
			} finally {
				checkingBackend = false;
			}
		} catch (e) {
			console.error('Error clearing cookies:', e);
			// As a last resort, redirect to clear-cookies page
			// eslint-disable-next-line no-console
			console.log('Redirecting to /clear-cookies page after error');
			window.location.href = '/clear-cookies';
		} finally {
			clearingCookies = false;
		}
	}

	/**
	 * Checks if the error is related to request headers or cookies being too large.
	 * Detects various error message formats from nginx, proxies, and browsers.
	 */
	function isHeaderTooLargeError(errorMessage: string, details: string, rawText?: string): boolean {
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

		for (const pattern of patterns) {
			if (new RegExp(pattern).test(combined)) {
				return true;
			}
		}

		// Also check for 400 errors with HTML responses (nginx default error page)
		if (combined.includes('400') && (combined.includes('<html') || combined.includes('<!doctype'))) {
			return true;
		}

		return false;
	}

	async function handleSubmit(event: Event) {
		event.preventDefault();
		error = '';
		errorDetails = '';
		errorStatus = 0;
		fieldErrors = {};
		cookiesCleared = false;
		cookieDiagnostics = '';
		loading = true;

		try {
			const response = await api.login({ username, password });
			authStore.setUser(response.user);
			goto('/');
		} catch (err) {
			if (err instanceof ApiError) {
				error = err.message;
				errorDetails = err.details || '';
				errorStatus = err.status;
				fieldErrors = err.fieldErrors || {};

				// Check for 502/503 errors (backend unavailable or starting)
				if (err.status === 502 || err.status === 503) {
					error = 'Backend is unavailable';
					errorDetails = 'The backend server is not responding. This could mean:\n' +
						'• The backend is still starting up (please wait 30-60 seconds)\n' +
						'• The backend has crashed or is overloaded\n' +
						'• There is a network issue\n\n' +
						'Please wait a moment and try again, or check the server logs.';

					// Don't show cookie clearing button for 502/503 since it won't help
					cookieDiagnostics = '';
				}

				// Check for cookie/header too large error (400 from nginx or similar)
				// This can happen with status 400, 413, or 431
				const isHeaderError = [400, 413, 431].includes(err.status) &&
					isHeaderTooLargeError(err.message, err.details || '');

				if (isHeaderError) {
					// Check if nginx auto-cleared the cookies (look for "cookiesCleared" or "automatically cleared" in details)
					const autoClearedByNginx = err.details?.includes('automatically cleared') ||
						err.details?.includes('cookiesCleared');

					if (autoClearedByNginx) {
						error = 'Session cookies cleared';
						errorDetails = 'Your session cookies were too large and have been automatically cleared. ' +
							'Please refresh the page and try logging in again.';
						cookiesCleared = true;
						cookieDiagnostics = 'Cookies were automatically cleared by the server. Refresh and try again.';
					} else {
						error = 'Request headers too large';
						errorDetails = 'Your browser has accumulated too many cookies or the session data is too large. ' +
							'This is a common issue that can be resolved by clearing cookies.';
						cookieDiagnostics = getCookieDiagnostics();
					}
				}

				// Log detailed error info for debugging
				console.error('Login failed:', {
					status: err.status,
					statusText: err.statusText,
					message: err.message,
					details: err.details,
					fieldErrors: err.fieldErrors,
					timestamp: err.timestamp,
					fullMessage: err.getFullMessage(),
					isHeaderError,
					cookieDiagnostics: getCookieDiagnostics()
				});
			} else if (err instanceof Error) {
				error = err.message;
				// Check for header too large error in generic Error
				if (isHeaderTooLargeError(err.message, '')) {
					error = 'Request headers too large';
					errorDetails = 'Your browser has accumulated too many cookies or the session data is too large. ' +
						'This is a common issue that can be resolved by clearing cookies.';
					errorStatus = 400;
					cookieDiagnostics = getCookieDiagnostics();
				}
				console.error('Login error:', err);
			} else {
				error = 'An unexpected error occurred';
				console.error('Unknown login error:', err);
			}
		} finally {
			loading = false;
		}
	}

	function selectUser(user: string) {
		username = user;
		password = 'password';
		// Clear any previous errors when selecting a quick login
		error = '';
		errorDetails = '';
		errorStatus = 0;
		fieldErrors = {};
	}

	// Check if a specific field has an error
	function hasFieldError(field: string): boolean {
		return field in fieldErrors;
	}
</script>

<svelte:head>
	<title>Login - BPM Demo</title>
</svelte:head>

<div class="min-h-screen flex items-center justify-center bg-gradient-to-br from-blue-50 to-indigo-100 px-4">
	<div class="max-w-md w-full">
		<div class="text-center mb-8">
			<div class="w-16 h-16 bg-blue-600 rounded-2xl flex items-center justify-center mx-auto mb-4">
				<span class="text-white font-bold text-2xl">BPM</span>
			</div>
			<h1 class="text-2xl font-bold text-gray-900">Flowable + SvelteKit Demo</h1>
			<p class="text-gray-600 mt-2">Business Process Management</p>
		</div>

		<div class="card">
			<form onsubmit={handleSubmit} class="space-y-4">
				{#if cookiesCleared}
					<div class="bg-green-50 border border-green-200 text-green-700 px-4 py-3 rounded-lg text-sm">
						<div class="font-medium">Cookies and session cleared successfully</div>
						<div class="mt-1 text-green-600 text-xs">Please try logging in again.</div>
						{#if cookieDiagnostics}
							<div class="mt-2 text-green-600 text-xs bg-green-100 p-2 rounded font-mono break-words">
								{cookieDiagnostics}
							</div>
						{/if}
						{#if checkingBackend}
							<div class="mt-3 flex items-center justify-center gap-2 text-green-600">
								<div class="w-4 h-4 border-2 border-green-600 border-t-transparent rounded-full animate-spin"></div>
								<span class="text-sm">Checking backend availability...</span>
							</div>
						{:else if backendReady}
							<button
								type="button"
								onclick={() => window.location.reload()}
								class="mt-3 w-full py-2 px-3 text-sm font-medium text-white bg-green-600 hover:bg-green-700 rounded-lg transition-colors"
							>
								Refresh Page & Try Again
							</button>
						{:else}
							<div class="mt-3 space-y-2">
								<div class="bg-yellow-50 border border-yellow-200 text-yellow-700 px-3 py-2 rounded text-xs">
									<span class="font-medium">Backend may be starting up.</span> Please wait a moment or try the login form directly below.
								</div>
								<div class="flex gap-2">
									<button
										type="button"
										onclick={() => window.location.reload()}
										class="flex-1 py-2 px-3 text-sm font-medium text-green-700 bg-green-100 hover:bg-green-200 rounded-lg transition-colors"
									>
										Refresh Anyway
									</button>
									<button
										type="button"
										onclick={async () => {
											checkingBackend = true;
											backendReady = await checkBackendHealth(3);
											checkingBackend = false;
										}}
										class="flex-1 py-2 px-3 text-sm font-medium text-green-700 bg-green-100 hover:bg-green-200 rounded-lg transition-colors"
									>
										Re-check Backend
									</button>
								</div>
							</div>
						{/if}
					</div>
				{/if}

				{#if error}
					<div class="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded-lg text-sm">
						<div class="flex items-start justify-between">
							<div class="font-medium">{error}</div>
							{#if errorStatus > 0}
								<span class="text-xs bg-red-100 text-red-800 px-2 py-0.5 rounded-full ml-2">
									HTTP {errorStatus}
								</span>
							{/if}
						</div>
						{#if errorDetails}
							<div class="mt-2 text-red-600 text-xs leading-relaxed whitespace-pre-line">{errorDetails}</div>
						{/if}
						{#if [502, 503].includes(errorStatus)}
							<div class="mt-3 flex gap-2">
								<button
									type="button"
									onclick={async () => {
										error = '';
										errorDetails = '';
										errorStatus = 0;
										loading = true;
										// Wait a moment then check backend
										await new Promise(resolve => setTimeout(resolve, 2000));
										const ready = await checkBackendHealth(2);
										loading = false;
										if (ready) {
											// Backend is ready, user can try login now
											error = '';
										} else {
											error = 'Backend still unavailable';
											errorDetails = 'The backend is still not responding. Please wait longer or check the server logs.';
											errorStatus = 503;
										}
									}}
									disabled={loading}
									class="flex-1 py-2 px-3 text-sm font-medium text-white bg-blue-600 hover:bg-blue-700 disabled:bg-blue-400 disabled:cursor-not-allowed rounded-lg transition-colors"
								>
									{loading ? 'Checking...' : 'Check Backend & Retry'}
								</button>
								<button
									type="button"
									onclick={() => window.location.reload()}
									class="flex-1 py-2 px-3 text-sm font-medium text-red-700 bg-red-100 hover:bg-red-200 rounded-lg transition-colors"
								>
									Refresh Page
								</button>
							</div>
						{/if}
						{#if cookieDiagnostics && error.includes('headers too large')}
							<div class="mt-2 text-red-600 text-xs bg-red-100 p-2 rounded font-mono break-words">
								<span class="font-medium">Diagnostics:</span> {cookieDiagnostics}
							</div>
						{/if}
						{#if Object.keys(fieldErrors).length > 0}
							<div class="mt-2 border-t border-red-200 pt-2">
								<div class="text-xs font-medium text-red-800 mb-1">Field errors:</div>
								<ul class="text-xs text-red-600 list-disc list-inside space-y-0.5">
									{#each Object.entries(fieldErrors) as [field, message]}
										<li><span class="font-medium">{field}:</span> {message}</li>
									{/each}
								</ul>
							</div>
						{/if}
						{#if [400, 413, 431].includes(errorStatus) && error.includes('headers too large')}
							<div class="mt-3 space-y-2">
								<button
									type="button"
									onclick={clearAllCookies}
									disabled={clearingCookies}
									class="w-full py-2 px-3 text-sm font-medium text-white bg-red-600 hover:bg-red-700 disabled:bg-red-400 disabled:cursor-not-allowed rounded-lg transition-colors"
								>
									{#if clearingCookies}
										Clearing cookies...
									{:else}
										Clear Cookies & Session
									{/if}
								</button>
								<p class="text-xs text-red-500">
									This will clear both browser cookies and server session data to resolve the issue.
								</p>
								<p class="text-xs text-red-400">
									If the button above doesn't work, <a href="/clear-cookies" class="underline font-medium hover:text-red-600">click here to clear cookies directly</a>.
								</p>
							</div>
						{/if}
					</div>
				{/if}

				<div>
					<label for="username" class="label">Username</label>
					<input
						id="username"
						type="text"
						bind:value={username}
						class="input"
						class:border-red-500={hasFieldError('username')}
						placeholder="Enter username"
						required
					/>
					{#if hasFieldError('username')}
						<p class="mt-1 text-xs text-red-600">{fieldErrors.username}</p>
					{/if}
				</div>

				<div>
					<label for="password" class="label">Password</label>
					<input
						id="password"
						type="password"
						bind:value={password}
						class="input"
						class:border-red-500={hasFieldError('password')}
						placeholder="Enter password"
						required
					/>
					{#if hasFieldError('password')}
						<p class="mt-1 text-xs text-red-600">{fieldErrors.password}</p>
					{/if}
				</div>

				<button
					type="submit"
					class="w-full btn btn-primary"
					disabled={loading}
				>
					{loading ? 'Signing in...' : 'Sign In'}
				</button>
			</form>

			<div class="mt-6 pt-6 border-t border-gray-200">
				<p class="text-sm text-gray-600 mb-4 text-center">Quick login <span class="text-xs text-gray-400">(password: "password")</span></p>

				<!-- Tabs for department selection -->
				{#snippet userButton(username: string, displayName: string, role: string, colorClass: string)}
					<button
						type="button"
						onclick={() => selectUser(username)}
						class="px-2 py-1.5 text-xs {colorClass} rounded-lg transition-colors text-left truncate"
						title="{displayName} ({username})"
					>
						<div class="font-medium truncate">{displayName}</div>
						<div class="text-[10px] opacity-75 truncate">{role}</div>
					</button>
				{/snippet}

				<!-- Executive Leadership -->
				<div class="mb-3">
					<div class="text-xs font-medium text-purple-600 uppercase tracking-wider mb-2 flex items-center gap-1">
						<span class="w-2 h-2 bg-purple-500 rounded-full"></span>
						Executive Leadership
					</div>
					<div class="grid grid-cols-3 gap-1.5">
						{@render userButton('exec.ceo', 'Elizabeth R.', 'CEO', 'bg-purple-100 hover:bg-purple-200 border border-purple-200 text-purple-900')}
						{@render userButton('executive1', 'Executive', 'Legacy', 'bg-purple-50 hover:bg-purple-100 border border-purple-100 text-purple-800')}
						{@render userButton('director1', 'Director', 'Legacy', 'bg-indigo-50 hover:bg-indigo-100 border border-indigo-100 text-indigo-800')}
					</div>
				</div>

				<!-- Engineering Department -->
				<div class="mb-3">
					<div class="text-xs font-medium text-blue-600 uppercase tracking-wider mb-2 flex items-center gap-1">
						<span class="w-2 h-2 bg-blue-500 rounded-full"></span>
						Engineering
					</div>
					<div class="grid grid-cols-4 gap-1.5">
						{@render userButton('eng.lisa', 'Lisa W.', 'Manager', 'bg-blue-100 hover:bg-blue-200 border border-blue-200 text-blue-900')}
						{@render userButton('eng.mike', 'Mike J.', 'Tech Lead', 'bg-blue-50 hover:bg-blue-100 border border-blue-100 text-blue-800')}
						{@render userButton('eng.john', 'John C.', 'Engineer', 'bg-slate-100 hover:bg-slate-200 border border-slate-200 text-slate-800')}
						{@render userButton('eng.sarah', 'Sarah M.', 'Engineer', 'bg-slate-100 hover:bg-slate-200 border border-slate-200 text-slate-800')}
					</div>
				</div>

				<!-- Finance Department -->
				<div class="mb-3">
					<div class="text-xs font-medium text-emerald-600 uppercase tracking-wider mb-2 flex items-center gap-1">
						<span class="w-2 h-2 bg-emerald-500 rounded-full"></span>
						Finance
					</div>
					<div class="grid grid-cols-5 gap-1.5">
						{@render userButton('fin.cfo', 'Michael T.', 'CFO', 'bg-emerald-100 hover:bg-emerald-200 border border-emerald-200 text-emerald-900')}
						{@render userButton('fin.david', 'David W.', 'Manager', 'bg-emerald-50 hover:bg-emerald-100 border border-emerald-100 text-emerald-800')}
						{@render userButton('fin.carol', 'Carol D.', 'Sr Acct', 'bg-teal-50 hover:bg-teal-100 border border-teal-100 text-teal-800')}
						{@render userButton('fin.bob', 'Bob S.', 'Acct', 'bg-slate-100 hover:bg-slate-200 border border-slate-200 text-slate-800')}
						{@render userButton('fin.alice', 'Alice B.', 'Acct', 'bg-slate-100 hover:bg-slate-200 border border-slate-200 text-slate-800')}
					</div>
				</div>

				<!-- HR Department -->
				<div class="mb-3">
					<div class="text-xs font-medium text-pink-600 uppercase tracking-wider mb-2 flex items-center gap-1">
						<span class="w-2 h-2 bg-pink-500 rounded-full"></span>
						Human Resources
					</div>
					<div class="grid grid-cols-5 gap-1.5">
						{@render userButton('hr.chro', 'Patricia J.', 'CHRO', 'bg-pink-100 hover:bg-pink-200 border border-pink-200 text-pink-900')}
						{@render userButton('hr.tom', 'Tom T.', 'Manager', 'bg-pink-50 hover:bg-pink-100 border border-pink-100 text-pink-800')}
						{@render userButton('hr.nina', 'Nina A.', 'HRBP', 'bg-rose-50 hover:bg-rose-100 border border-rose-100 text-rose-800')}
						{@render userButton('hr.emma', 'Emma G.', 'HR Spec', 'bg-slate-100 hover:bg-slate-200 border border-slate-200 text-slate-800')}
						{@render userButton('hr.james', 'James M.', 'HR Spec', 'bg-slate-100 hover:bg-slate-200 border border-slate-200 text-slate-800')}
					</div>
				</div>

				<!-- Sales Department -->
				<div class="mb-3">
					<div class="text-xs font-medium text-orange-600 uppercase tracking-wider mb-2 flex items-center gap-1">
						<span class="w-2 h-2 bg-orange-500 rounded-full"></span>
						Sales
					</div>
					<div class="grid grid-cols-4 gap-1.5">
						{@render userButton('sales.rachel', 'Rachel T.', 'Manager', 'bg-orange-100 hover:bg-orange-200 border border-orange-200 text-orange-900')}
						{@render userButton('sales.peter', 'Peter M.', 'Team Lead', 'bg-orange-50 hover:bg-orange-100 border border-orange-100 text-orange-800')}
						{@render userButton('sales.kevin', 'Kevin W.', 'Sales Rep', 'bg-slate-100 hover:bg-slate-200 border border-slate-200 text-slate-800')}
						{@render userButton('sales.maria', 'Maria H.', 'Sales Rep', 'bg-slate-100 hover:bg-slate-200 border border-slate-200 text-slate-800')}
					</div>
				</div>

				<!-- Operations Department -->
				<div class="mb-3">
					<div class="text-xs font-medium text-amber-600 uppercase tracking-wider mb-2 flex items-center gap-1">
						<span class="w-2 h-2 bg-amber-500 rounded-full"></span>
						Operations
					</div>
					<div class="grid grid-cols-5 gap-1.5">
						{@render userButton('ops.coo', 'Robert W.', 'COO', 'bg-amber-100 hover:bg-amber-200 border border-amber-200 text-amber-900')}
						{@render userButton('ops.grace', 'Grace L.', 'Manager', 'bg-amber-50 hover:bg-amber-100 border border-amber-100 text-amber-800')}
						{@render userButton('ops.frank', 'Frank L.', 'Supervisor', 'bg-yellow-50 hover:bg-yellow-100 border border-yellow-100 text-yellow-800')}
						{@render userButton('ops.steve', 'Steve R.', 'Analyst', 'bg-slate-100 hover:bg-slate-200 border border-slate-200 text-slate-800')}
						{@render userButton('ops.linda', 'Linda C.', 'Analyst', 'bg-slate-100 hover:bg-slate-200 border border-slate-200 text-slate-800')}
					</div>
				</div>

				<!-- IT Department -->
				<div class="mb-3">
					<div class="text-xs font-medium text-cyan-600 uppercase tracking-wider mb-2 flex items-center gap-1">
						<span class="w-2 h-2 bg-cyan-500 rounded-full"></span>
						IT / Technology
					</div>
					<div class="grid grid-cols-5 gap-1.5">
						{@render userButton('it.cto', 'William S.', 'CTO', 'bg-cyan-100 hover:bg-cyan-200 border border-cyan-200 text-cyan-900')}
						{@render userButton('it.olivia', 'Olivia K.', 'Manager', 'bg-cyan-50 hover:bg-cyan-100 border border-cyan-100 text-cyan-800')}
						{@render userButton('it.henry', 'Henry Y.', 'Team Lead', 'bg-sky-50 hover:bg-sky-100 border border-sky-100 text-sky-800')}
						{@render userButton('it.alex', 'Alex H.', 'IT Spec', 'bg-slate-100 hover:bg-slate-200 border border-slate-200 text-slate-800')}
						{@render userButton('it.diana', 'Diana A.', 'IT Spec', 'bg-slate-100 hover:bg-slate-200 border border-slate-200 text-slate-800')}
					</div>
				</div>

				<!-- Legal Department -->
				<div class="mb-3">
					<div class="text-xs font-medium text-violet-600 uppercase tracking-wider mb-2 flex items-center gap-1">
						<span class="w-2 h-2 bg-violet-500 rounded-full"></span>
						Legal / Compliance
					</div>
					<div class="grid grid-cols-3 gap-1.5">
						{@render userButton('legal.claire', 'Claire N.', 'Manager', 'bg-violet-100 hover:bg-violet-200 border border-violet-200 text-violet-900')}
						{@render userButton('legal.ben', 'Ben A.', 'Sr Counsel', 'bg-violet-50 hover:bg-violet-100 border border-violet-100 text-violet-800')}
						{@render userButton('legal.amy', 'Amy G.', 'Analyst', 'bg-slate-100 hover:bg-slate-200 border border-slate-200 text-slate-800')}
					</div>
				</div>

				<!-- Legacy Users (Collapsible) -->
				<details class="mt-4">
					<summary class="text-xs font-medium text-gray-400 uppercase tracking-wider mb-2 cursor-pointer hover:text-gray-600">
						Legacy Test Users
					</summary>
					<div class="grid grid-cols-4 gap-1.5 mt-2">
						{@render userButton('manager1', 'Manager 1', 'Manager', 'bg-gray-100 hover:bg-gray-200 border border-gray-200 text-gray-800')}
						{@render userButton('manager2', 'Manager 2', 'Manager', 'bg-gray-100 hover:bg-gray-200 border border-gray-200 text-gray-800')}
						{@render userButton('supervisor1', 'Supervisor 1', 'Supervisor', 'bg-gray-100 hover:bg-gray-200 border border-gray-200 text-gray-800')}
						{@render userButton('supervisor2', 'Supervisor 2', 'Supervisor', 'bg-gray-100 hover:bg-gray-200 border border-gray-200 text-gray-800')}
						{@render userButton('user1', 'User 1', 'Employee', 'bg-gray-50 hover:bg-gray-100 border border-gray-100 text-gray-700')}
						{@render userButton('user2', 'User 2', 'Employee', 'bg-gray-50 hover:bg-gray-100 border border-gray-100 text-gray-700')}
					</div>
				</details>
			</div>
		</div>

		<p class="text-center text-sm text-gray-500 mt-6">
			Powered by Flowable 7.0 + SvelteKit + Svelte 5
		</p>
	</div>
</div>
