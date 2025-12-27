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

	/**
	 * Clears all cookies for the current domain to resolve "Request Header Or Cookie Too Large" errors.
	 * This typically happens when session cookies accumulate over time.
	 */
	function clearAllCookies(): void {
		const cookies = document.cookie.split(';');
		for (const cookie of cookies) {
			const eqPos = cookie.indexOf('=');
			const name = eqPos > -1 ? cookie.substring(0, eqPos).trim() : cookie.trim();
			if (name) {
				// Clear the cookie with various path combinations
				document.cookie = `${name}=;expires=Thu, 01 Jan 1970 00:00:00 GMT;path=/`;
				document.cookie = `${name}=;expires=Thu, 01 Jan 1970 00:00:00 GMT;path=/api`;
			}
		}
		cookiesCleared = true;
		console.log('Cleared all cookies to resolve header size issue');
	}

	/**
	 * Checks if the error is related to request headers or cookies being too large
	 */
	function isHeaderTooLargeError(errorMessage: string, details: string): boolean {
		const combined = `${errorMessage} ${details}`.toLowerCase();
		return combined.includes('header') && (combined.includes('too large') || combined.includes('400'));
	}

	async function handleSubmit(event: Event) {
		event.preventDefault();
		error = '';
		errorDetails = '';
		errorStatus = 0;
		fieldErrors = {};
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

				// Check for cookie/header too large error (400 from nginx)
				if (err.status === 400 && isHeaderTooLargeError(err.message, err.details || '')) {
					error = 'Request headers too large';
					errorDetails = 'Your browser has accumulated too many cookies. Click "Clear Cookies & Retry" below to fix this.';
				}

				// Log detailed error info for debugging
				console.error('Login failed:', {
					status: err.status,
					statusText: err.statusText,
					message: err.message,
					details: err.details,
					fieldErrors: err.fieldErrors,
					timestamp: err.timestamp,
					fullMessage: err.getFullMessage()
				});
			} else if (err instanceof Error) {
				error = err.message;
				// Check for header too large error in generic Error
				if (isHeaderTooLargeError(err.message, '')) {
					error = 'Request headers too large';
					errorDetails = 'Your browser has accumulated too many cookies. Click "Clear Cookies & Retry" below to fix this.';
					errorStatus = 400;
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
						<div class="font-medium">Cookies cleared successfully</div>
						<div class="mt-1 text-green-600 text-xs">Please try logging in again.</div>
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
							<div class="mt-2 text-red-600 text-xs leading-relaxed">{errorDetails}</div>
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
						{#if errorStatus === 400 && error.includes('headers too large')}
							<button
								type="button"
								onclick={() => { clearAllCookies(); error = ''; errorDetails = ''; errorStatus = 0; }}
								class="mt-3 w-full py-2 px-3 text-sm font-medium text-white bg-red-600 hover:bg-red-700 rounded-lg transition-colors"
							>
								Clear Cookies & Retry
							</button>
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
				<p class="text-sm text-gray-600 mb-3 text-center">Quick login (password: "password")</p>
				<div class="grid grid-cols-3 gap-2">
					<button
						type="button"
						onclick={() => selectUser('user1')}
						class="px-3 py-2 text-sm bg-gray-100 hover:bg-gray-200 rounded-lg transition-colors"
					>
						<div class="font-medium">User</div>
						<div class="text-xs text-gray-500">user1</div>
					</button>
					<button
						type="button"
						onclick={() => selectUser('supervisor1')}
						class="px-3 py-2 text-sm bg-blue-100 hover:bg-blue-200 rounded-lg transition-colors"
					>
						<div class="font-medium">Supervisor</div>
						<div class="text-xs text-gray-500">supervisor1</div>
					</button>
					<button
						type="button"
						onclick={() => selectUser('executive1')}
						class="px-3 py-2 text-sm bg-purple-100 hover:bg-purple-200 rounded-lg transition-colors"
					>
						<div class="font-medium">Executive</div>
						<div class="text-xs text-gray-500">executive1</div>
					</button>
				</div>
			</div>
		</div>

		<p class="text-center text-sm text-gray-500 mt-6">
			Powered by Flowable 7.0 + SvelteKit + Svelte 5
		</p>
	</div>
</div>
