<script lang="ts">
	import { goto } from '$app/navigation';
	import { api } from '$lib/api/client';
	import { authStore } from '$lib/stores/auth.svelte';
	import NotificationBell from './NotificationBell.svelte';
	import { navigationSchema } from '$lib/nav-schema';
	import NavLink from './NavLink.svelte';
	import { Menu } from '@lucide/svelte';
	import * as Sheet from '$lib/components/ui/sheet';

	let open = $state(false);

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

				<!-- Desktop navigation -->
				{#if authStore.isAuthenticated}
					<div class="hidden sm:ml-8 sm:flex sm:space-x-2">
						{#each navigationSchema as item}
							<NavLink {item} />
						{/each}
					</div>
				{/if}
			</div>

			{#if authStore.isAuthenticated && authStore.user}
				<div class="flex items-center space-x-4">
					<div class="hidden sm:flex items-center space-x-4">
						<NotificationBell />
						<div class="flex items-center space-x-2">
							<span class="text-sm text-gray-600">{authStore.user.displayName}</span>
							{#each authStore.user.roles as role}
								<span
									class="px-2 py-0.5 text-xs font-medium rounded-full {getRoleBadgeColor(role)}"
								>
									{role}
								</span>
							{/each}
						</div>
						<button onclick={handleLogout} class="text-sm text-gray-600 hover:text-gray-900">
							Logout
						</button>
					</div>

					<!-- Mobile menu button -->
					<div class="sm:hidden">
						<Sheet.Root bind:open>
							<Sheet.Trigger class="p-2 rounded-md text-gray-600 hover:text-gray-900">
								<Menu class="h-6 w-6" />
							</Sheet.Trigger>
							<Sheet.Content class="w-[300px] sm:w-[400px]">
								<Sheet.Header>
									<Sheet.Title>Menu</Sheet.Title>
								</Sheet.Header>
								<div class="mt-4">
									<div class="flex flex-col space-y-2">
										{#each navigationSchema as item}
											{#if item.children}
												<div class="px-3">
													<h3 class="text-sm font-semibold text-gray-500 uppercase tracking-wider">
														{item.title}
													</h3>
													<div class="mt-2 flex flex-col space-y-1">
														{#each item.children as child}
															<a
																href={child.href}
																class="flex items-center px-3 py-2 text-base font-medium text-gray-700 hover:bg-gray-100 rounded-md"
																onclick={() => (open = false)}
															>
																<svelte:component this={child.icon} class="h-5 w-5 mr-3" />
																{child.title}
															</a>
														{/each}
													</div>
												</div>
											{:else}
												<a
													href={item.href}
													class="flex items-center px-3 py-2 text-base font-medium text-gray-700 hover:bg-gray-100 rounded-md"
													onclick={() => (open = false)}
												>
													<svelte:component this={item.icon} class="h-5 w-5 mr-3" />
													{item.title}
												</a>
											{/if}
										{/each}
									</div>
									<div class="mt-6 border-t pt-4">
										<button
											onclick={() => {
												handleLogout();
												open = false;
											}}
											class="w-full text-left flex items-center px-3 py-2 text-base font-medium text-gray-700 hover:bg-gray-100 rounded-md"
										>
											Logout
										</button>
									</div>
								</div>
							</Sheet.Content>
						</Sheet.Root>
					</div>
				</div>
			{/if}
		</div>
	</div>
</nav>
