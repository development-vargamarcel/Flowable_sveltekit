<script lang="ts">
	import '../app.css';
	import { page } from '$app/stores';
	import { goto } from '$app/navigation';
	import { onMount } from 'svelte';
	import { api } from '$lib/api/client';
	import { authStore } from '$lib/stores/auth.svelte';
	import Navbar from '$lib/components/Navbar.svelte';

	const { children } = $props();

	// Public routes that don't require authentication
	const publicRoutes = ['/login'];

	onMount(async () => {
		// Try to get current user on mount
		try {
			const user = await api.getCurrentUser();
			authStore.setUser(user);
		} catch {
			authStore.setUser(null);
		}
	});

	// Redirect logic based on auth state
	$effect(() => {
		if (!authStore.loading) {
			const isPublicRoute = publicRoutes.includes($page.url.pathname);

			if (!authStore.isAuthenticated && !isPublicRoute) {
				goto('/login');
			} else if (authStore.isAuthenticated && $page.url.pathname === '/login') {
				goto('/');
			}
		}
	});
</script>

<div class="min-h-screen flex flex-col">
	{#if authStore.loading}
		<div class="flex-1 flex items-center justify-center">
			<div class="text-center">
				<div class="w-12 h-12 border-4 border-blue-600 border-t-transparent rounded-full animate-spin mx-auto"></div>
				<p class="mt-4 text-gray-600">Loading...</p>
			</div>
		</div>
	{:else}
		{#if authStore.isAuthenticated}
			<Navbar />
		{/if}
		<main class="flex-1">
			{@render children()}
		</main>
	{/if}
</div>
