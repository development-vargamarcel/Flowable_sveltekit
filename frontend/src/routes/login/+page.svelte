<script lang="ts">
	import { goto } from '$app/navigation';
	import { api, ApiError } from '$lib/api/client';
	import { authStore } from '$lib/stores/auth.svelte';
	import { checkBackendHealth, clearAllCookies, getCookieDiagnostics, isHeaderTooLargeError } from '$lib/utils/session-utils';

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

	async function handleClearCookies() {
		clearingCookies = true;
		try {
			const result = await clearAllCookies();
			cookiesCleared = result.cookiesCleared;
			cookieDiagnostics = result.diagnostics;
			backendReady = result.backendReady;

			// If backend is healthy, try to auto-reload page after a delay to ensure clean state
			if (backendReady) {
				setTimeout(() => {
					window.location.reload();
				}, 2000);
			}
		} finally {
			clearingCookies = false;
		}
	}

	async function handleSubmit(e: Event) {
		e.preventDefault();
		loading = true;
		error = '';
		errorDetails = '';
		errorStatus = 0;
		fieldErrors = {};
		cookiesCleared = false;
		cookieDiagnostics = '';

		try {
			await authStore.login(username, password);
			goto('/');
		} catch (e: any) {
			console.error('Login error:', e);

			if (e instanceof ApiError) {
				errorStatus = e.status;
				if (e.status === 400 && e.fieldErrors) {
					fieldErrors = e.fieldErrors;
					error = 'Please check the fields below.';
				} else if (e.status === 401) {
					error = 'Invalid credentials. Please try again.';
				} else if (e.status >= 500) {
					error = 'Server error. Please try again later.';
					errorDetails = e.message;
				} else {
					error = e.message || 'Login failed.';
					if (e.details) {
						errorDetails = e.details;
					}
				}

				// Check for header too large errors in 400/431/502/520 responses
				const isHeaderError = isHeaderTooLargeError(error, errorDetails, JSON.stringify(e));

				if (isHeaderError || e.status === 431 || (e.status === 400 && error.toLowerCase().includes('header'))) {
					error = 'Your browser sent too many cookies (Header Too Large).';
					errorDetails = 'This often happens when many session cookies accumulate. Please click the button below to clear them.';
					cookieDiagnostics = getCookieDiagnostics();
				}
			} else {
				error = 'An unexpected error occurred.';
				errorDetails = e.message || String(e);
			}
		} finally {
			loading = false;
		}
	}
</script>

<div class="flex min-h-screen items-center justify-center bg-gray-50 px-4 py-12 sm:px-6 lg:px-8">
	<div class="w-full max-w-md space-y-8">
		<div>
			<h2 class="mt-6 text-center text-3xl font-bold tracking-tight text-gray-900">
				Sign in to your account
			</h2>
			<p class="mt-2 text-center text-sm text-gray-600">
				Or
				<button type="button" class="font-medium text-blue-600 hover:text-blue-500 bg-transparent border-0 p-0 cursor-pointer">
					start your 14-day free trial
				</button>
			</p>
		</div>

		<form class="mt-8 space-y-6" onsubmit={handleSubmit}>
			<input type="hidden" name="remember" value="true" />

			{#if error}
				<div class="rounded-md bg-red-50 p-4">
					<div class="flex">
						<div class="flex-shrink-0">
							<svg class="h-5 w-5 text-red-400" viewBox="0 0 20 20" fill="currentColor" aria-hidden="true">
								<path fill-rule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zM8.28 7.22a.75.75 0 00-1.06 1.06L8.94 10l-1.72 1.72a.75.75 0 101.06 1.06L10 11.06l1.72 1.72a.75.75 0 101.06-1.06L11.06 10l1.72-1.72a.75.75 0 00-1.06-1.06L10 8.94 8.28 7.22z" clip-rule="evenodd" />
							</svg>
						</div>
						<div class="ml-3">
							<h3 class="text-sm font-medium text-red-800">Login Failed</h3>
							<div class="mt-2 text-sm text-red-700">
								<p>{error}</p>
								{#if errorDetails}
									<p class="mt-1 text-xs opacity-75">{errorDetails}</p>
								{/if}
								{#if errorStatus === 404}
									<p class="mt-1 text-xs">The authentication service could not be reached. Please check if the backend is running.</p>
								{/if}
							</div>

							{#if cookieDiagnostics || error.includes('cookie') || error.includes('header')}
								<div class="mt-4">
									<div class="rounded bg-red-100 p-2 text-xs font-mono text-red-800 break-all mb-2">
										{cookieDiagnostics || getCookieDiagnostics()}
									</div>
									<button
										type="button"
										onclick={handleClearCookies}
										disabled={clearingCookies}
										class="inline-flex items-center rounded-md bg-red-600 px-3 py-2 text-sm font-semibold text-white shadow-sm hover:bg-red-500 focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-red-600 disabled:opacity-50"
									>
										{#if clearingCookies}
											Clearing...
										{:else}
											Clear Cookies & Fix
										{/if}
									</button>
								</div>
							{/if}
						</div>
					</div>
				</div>
			{/if}

			{#if cookiesCleared}
				<div class="rounded-md bg-green-50 p-4">
					<div class="flex">
						<div class="flex-shrink-0">
							<svg class="h-5 w-5 text-green-400" viewBox="0 0 20 20" fill="currentColor">
								<path fill-rule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zm3.857-9.809a.75.75 0 00-1.214-.882l-3.483 4.79-1.88-1.88a.75.75 0 10-1.06 1.061l2.5 2.5a.75.75 0 001.137-.089l4-5.5z" clip-rule="evenodd" />
							</svg>
						</div>
						<div class="ml-3">
							<h3 class="text-sm font-medium text-green-800">Cookies Cleared Successfully</h3>
							<div class="mt-2 text-sm text-green-700">
								<p>{cookieDiagnostics}</p>
								<p class="mt-1 font-semibold">Please try logging in again.</p>
							</div>
						</div>
					</div>
				</div>
			{/if}

			<div class="-space-y-px rounded-md shadow-sm">
				<div>
					<label for="username" class="sr-only">Username</label>
					<input
						id="username"
						name="username"
						type="text"
						required
						bind:value={username}
						class="relative block w-full rounded-t-md border-0 py-1.5 text-gray-900 ring-1 ring-inset ring-gray-300 placeholder:text-gray-400 focus:z-10 focus:ring-2 focus:ring-inset focus:ring-blue-600 sm:text-sm sm:leading-6 {fieldErrors.username ? 'ring-red-300 focus:ring-red-500' : ''}"
						placeholder="Username"
					/>
					{#if fieldErrors.username}
						<p class="mt-1 text-sm text-red-600">{fieldErrors.username}</p>
					{/if}
				</div>
				<div>
					<label for="password" class="sr-only">Password</label>
					<input
						id="password"
						name="password"
						type="password"
						required
						bind:value={password}
						class="relative block w-full rounded-b-md border-0 py-1.5 text-gray-900 ring-1 ring-inset ring-gray-300 placeholder:text-gray-400 focus:z-10 focus:ring-2 focus:ring-inset focus:ring-blue-600 sm:text-sm sm:leading-6 {fieldErrors.password ? 'ring-red-300 focus:ring-red-500' : ''}"
						placeholder="Password"
					/>
					{#if fieldErrors.password}
						<p class="mt-1 text-sm text-red-600">{fieldErrors.password}</p>
					{/if}
				</div>
			</div>

			<div class="flex items-center justify-between">
				<div class="flex items-center">
					<input
						id="remember-me"
						name="remember-me"
						type="checkbox"
						class="h-4 w-4 rounded border-gray-300 text-blue-600 focus:ring-blue-600"
					/>
					<label for="remember-me" class="ml-2 block text-sm text-gray-900">Remember me</label>
				</div>

				<div class="text-sm">
					<button type="button" class="font-medium text-blue-600 hover:text-blue-500 bg-transparent border-0 p-0 cursor-pointer">Forgot password?</button>
				</div>
			</div>

			<div>
				<button
					type="submit"
					disabled={loading}
					class="group relative flex w-full justify-center rounded-md bg-blue-600 px-3 py-2 text-sm font-semibold text-white hover:bg-blue-500 focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-blue-600 disabled:opacity-50"
				>
					{#if loading}
						<span class="absolute inset-y-0 left-0 flex items-center pl-3">
							<svg class="h-5 w-5 animate-spin text-blue-300" viewBox="0 0 24 24">
								<circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
								<path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
							</svg>
						</span>
						Signing in...
					{:else}
						<span class="absolute inset-y-0 left-0 flex items-center pl-3">
							<svg class="h-5 w-5 text-blue-500 group-hover:text-blue-400" viewBox="0 0 20 20" fill="currentColor" aria-hidden="true">
								<path fill-rule="evenodd" d="M10 1a4.5 4.5 0 00-4.5 4.5V9H5a2 2 0 00-2 2v6a2 2 0 002 2h10a2 2 0 002-2v-6a2 2 0 00-2-2h-.5V5.5A4.5 4.5 0 0010 1zm3 8V5.5a3 3 0 10-6 0V9h6z" clip-rule="evenodd" />
							</svg>
						</span>
						Sign in
					{/if}
				</button>
			</div>
		</form>

		<div class="mt-4 text-center">
			<p class="text-xs text-gray-500">
				Valid credentials: <strong>admin / admin</strong>, <strong>user1 / password</strong>
			</p>
		</div>
	</div>
</div>
