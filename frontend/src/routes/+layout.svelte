<script lang="ts">
	import '../app.css';
	import { browser } from '$app/environment';
	import { page } from '$app/stores';
	import { goto } from '$app/navigation';
	import { onMount } from 'svelte';
	import { api } from '$lib/api/client';
	import { authStore } from '$lib/stores/auth.svelte';
	import Navbar from '$lib/components/Navbar.svelte';
	import Breadcrumbs from '$lib/components/Breadcrumbs.svelte';
	import BackendStartingBanner from '$lib/components/BackendStartingBanner.svelte';
	import { Toaster } from '$lib/components/ui/sonner';

	const { children } = $props();

	let renderingError = $state<Error | null>(null);

	// Basic Error Boundary mechanism for Svelte 5
	// Note: In Svelte 5, we can use the 'onerror' hook at the root
	$effect(() => {
		const handleError = (error: Error) => {
			console.error('Unhandled rendering error:', error);
			renderingError = error;
		};
		window.addEventListener('error', (e) => handleError(e.error));
		window.addEventListener('unhandledrejection', (e) => handleError(e.reason));
	});

	// Public routes that don't require authentication
	const publicRoutes = ['/login'];
	const isPublicRoute = $derived(publicRoutes.includes($page.url.pathname));

	onMount(async () => {
		// Initialize dark mode
		if (
			localStorage.theme === 'dark' ||
			(!('theme' in localStorage) && window.matchMedia('(prefers-color-scheme: dark)').matches)
		) {
			document.documentElement.classList.add('dark');
		} else {
			document.documentElement.classList.remove('dark');
		}

		// Avoid calling /api/auth/me on public routes (for example /login)
		// to prevent unnecessary backend calls and noisy browser console errors.
		if (publicRoutes.includes(window.location.pathname)) {
			authStore.setUser(null);
			document.documentElement.dataset.appReady = 'true';
			return;
		}

		try {
			const user = await api.getCurrentUser();
			authStore.setUser(user);
		} catch {
			authStore.setUser(null);
		} finally {
			document.documentElement.dataset.appReady = 'true';
		}
	});

	// Redirect logic based on auth state - only run in browser
	$effect(() => {
		if (browser && !authStore.loading) {
			const isPublicRoute = publicRoutes.includes($page.url.pathname);

			if (!authStore.isAuthenticated && !isPublicRoute) {
				goto('/login');
			} else if (authStore.isAuthenticated && $page.url.pathname === '/login') {
				goto('/');
			}
		}
	});
</script>

<!-- Backend startup banner - shows when backend is cold starting on Railway -->
<BackendStartingBanner />

<div class="min-h-screen flex flex-col bg-gray-50 dark:bg-gray-900 transition-colors duration-200">
	{#if authStore.loading && !isPublicRoute}
		<div class="flex-1 flex items-center justify-center">
			<div class="text-center">
				<div class="w-12 h-12 border-4 border-blue-600 border-t-transparent rounded-full animate-spin mx-auto"></div>
				<p class="mt-4 text-gray-600 dark:text-gray-400">Loading...</p>
			</div>
		</div>
	{:else}
		{#if authStore.isAuthenticated}
			<Navbar />
			<Breadcrumbs />
		{/if}
		<main class="flex-1">
			{#if renderingError}
				<div class="p-8 max-w-4xl mx-auto">
					<div class="bg-red-50 border border-red-200 text-red-700 px-6 py-4 rounded-lg shadow-sm">
						<h1 class="text-xl font-bold mb-2">Something went wrong</h1>
						<p class="mb-4">The application encountered an unexpected error.</p>
						<pre class="bg-red-100 p-4 rounded text-sm overflow-auto max-h-60 mb-6 font-mono">{renderingError.message}</pre>
						<button
							onclick={() => window.location.reload()}
							class="px-4 py-2 bg-red-600 text-white rounded-md hover:bg-red-700 transition-colors font-medium"
						>
							Reload Application
						</button>
					</div>
				</div>
			{:else}
				{@render children()}
			{/if}
		</main>
	{/if}
</div>

<Toaster />
