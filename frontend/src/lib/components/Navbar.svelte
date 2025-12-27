<script lang="ts">
	import { goto } from '$app/navigation';
	import { api } from '$lib/api/client';
	import { authStore } from '$lib/stores/auth.svelte';

	async function handleLogout() {
		try {
			await api.logout();
			authStore.clear();
			goto('/login');
		} catch (error) {
			console.error('Logout failed:', error);
		}
	}

	function getRoleBadgeColor(role: string): string {
		switch (role) {
			case 'EXECUTIVE':
				return 'bg-purple-100 text-purple-800';
			case 'DIRECTOR':
				return 'bg-pink-100 text-pink-800';
			case 'MANAGER':
				return 'bg-indigo-100 text-indigo-800';
			case 'SUPERVISOR':
				return 'bg-blue-100 text-blue-800';
			default:
				return 'bg-gray-100 text-gray-800';
		}
	}
</script>

<nav class="bg-white shadow-sm border-b border-gray-200">
	<div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
		<div class="flex justify-between h-16">
			<div class="flex items-center">
				<a href="/" class="flex items-center space-x-2">
					<div class="w-8 h-8 bg-blue-600 rounded-lg flex items-center justify-center">
						<span class="text-white font-bold text-sm">BPM</span>
					</div>
					<span class="font-semibold text-gray-900">Flowable Demo</span>
				</a>

				{#if authStore.isAuthenticated}
					<div class="hidden sm:ml-8 sm:flex sm:space-x-4">
						<a
							href="/"
							class="px-3 py-2 text-sm font-medium text-gray-600 hover:text-gray-900 hover:bg-gray-50 rounded-md"
						>
							Home
						</a>
						<a
							href="/dashboard"
							class="px-3 py-2 text-sm font-medium text-gray-600 hover:text-gray-900 hover:bg-gray-50 rounded-md"
						>
							Workflow Dashboard
						</a>
						<a
							href="/tasks"
							class="px-3 py-2 text-sm font-medium text-gray-600 hover:text-gray-900 hover:bg-gray-50 rounded-md"
						>
							Tasks
						</a>
						<a
							href="/processes"
							class="px-3 py-2 text-sm font-medium text-gray-600 hover:text-gray-900 hover:bg-gray-50 rounded-md"
						>
							Start Process
						</a>
					</div>
				{/if}
			</div>

			{#if authStore.isAuthenticated && authStore.user}
				<div class="flex items-center space-x-4">
					<div class="flex items-center space-x-2">
						<span class="text-sm text-gray-600">{authStore.user.displayName}</span>
						{#each authStore.user.roles as role}
							<span class="px-2 py-0.5 text-xs font-medium rounded-full {getRoleBadgeColor(role)}">
								{role}
							</span>
						{/each}
					</div>
					<button
						onclick={handleLogout}
						class="text-sm text-gray-600 hover:text-gray-900"
					>
						Logout
					</button>
				</div>
			{/if}
		</div>
	</div>

	<!-- Mobile menu -->
	{#if authStore.isAuthenticated}
		<div class="sm:hidden border-t border-gray-200">
			<div class="flex space-x-1 px-2 py-2">
				<a href="/" class="flex-1 text-center px-3 py-2 text-sm font-medium text-gray-600 hover:bg-gray-50 rounded-md">
					Home
				</a>
				<a href="/dashboard" class="flex-1 text-center px-3 py-2 text-sm font-medium text-gray-600 hover:bg-gray-50 rounded-md">
					Workflows
				</a>
				<a href="/tasks" class="flex-1 text-center px-3 py-2 text-sm font-medium text-gray-600 hover:bg-gray-50 rounded-md">
					Tasks
				</a>
				<a href="/processes" class="flex-1 text-center px-3 py-2 text-sm font-medium text-gray-600 hover:bg-gray-50 rounded-md">
					Start
				</a>
			</div>
		</div>
	{/if}
</nav>
