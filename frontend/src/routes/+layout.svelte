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

	// Public routes that don't require authentication
	const publicRoutes = ['/login'];

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

		// Try to get current user on mount
		try {
			const user = await api.getCurrentUser();
			authStore.setUser(user);
		} catch {
			authStore.setUser(null);
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
	{#if authStore.loading}
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
			{@render children()}
		</main>
	{/if}
</div>

<Toaster />
