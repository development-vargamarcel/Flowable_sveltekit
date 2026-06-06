<script lang="ts">
	import { goto } from '$app/navigation';
	import { api } from '$lib/api/client';
	import { authStore } from '$lib/stores/auth.svelte';
	import NotificationBell from './NotificationBell.svelte';
	import { navigationSchema } from '$lib/nav-schema';
	import NavLink from './NavLink.svelte';
	import { LogOut, Menu, Moon, Search, Sun, User } from '@lucide/svelte';
	import * as Sheet from '$lib/components/ui/sheet';
	import GlobalSearch from './GlobalSearch.svelte';
	import { page } from '$app/stores';
	import { cn } from '$lib/utils';
	import type { Component } from 'svelte';

	let open = $state(false);
	let isDark = $state(false);

	$effect(() => {
		if (
			localStorage.theme === 'dark' ||
			(!('theme' in localStorage) && window.matchMedia('(prefers-color-scheme: dark)').matches)
		) {
			isDark = true;
			document.documentElement.classList.add('dark');
		} else {
			isDark = false;
			document.documentElement.classList.remove('dark');
		}
	});

	function toggleDarkMode() {
		isDark = !isDark;
		if (isDark) {
			document.documentElement.classList.add('dark');
			localStorage.setItem('theme', 'dark');
		} else {
			document.documentElement.classList.remove('dark');
			localStorage.setItem('theme', 'light');
		}
	}

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
				return 'bg-violet-100 text-violet-800 dark:bg-violet-900/40 dark:text-violet-200';
			case 'DIRECTOR':
				return 'bg-rose-100 text-rose-800 dark:bg-rose-900/40 dark:text-rose-200';
			case 'MANAGER':
				return 'bg-indigo-100 text-indigo-800 dark:bg-indigo-900/40 dark:text-indigo-200';
			case 'SUPERVISOR':
				return 'bg-blue-100 text-blue-800 dark:bg-blue-900/40 dark:text-blue-200';
			default:
				return 'bg-gray-100 text-gray-800 dark:bg-gray-700 dark:text-gray-100';
		}
	}

	function isActive(href: string, currentPath: string): boolean {
		if (href === '/') return currentPath === '/';
		return currentPath.startsWith(href);
	}

	async function handleMobileLogout() {
		open = false;
		await handleLogout();
	}
</script>

<nav class="sticky top-0 z-40 border-b border-gray-200 bg-white/95 shadow-sm backdrop-blur dark:border-gray-700 dark:bg-gray-800/95">
	<div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
		<div class="flex min-h-16 items-center justify-between gap-3 py-2">
			<div class="flex min-w-0 items-center">
				<a href="/" class="flex min-w-0 items-center space-x-2 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500 focus:ring-offset-2 dark:focus:ring-offset-gray-800">
					<div class="flex h-8 w-8 shrink-0 items-center justify-center rounded-lg bg-blue-600">
						<span class="text-sm font-bold text-white">BPM</span>
					</div>
					<span class="truncate font-semibold text-gray-900 dark:text-white">Flowable Demo</span>
				</a>

				<!-- Desktop navigation -->
				{#if authStore.isAuthenticated}
					<div class="hidden lg:ml-8 lg:flex lg:space-x-2">
						{#each navigationSchema as item}
							<NavLink {item} />
						{/each}
					</div>
				{/if}
			</div>

			{#if authStore.isAuthenticated && authStore.user}
				<div class="flex min-w-0 items-center gap-1 sm:gap-2 lg:gap-4">
					<GlobalSearch />

					<button
						onclick={toggleDarkMode}
						class="rounded-full p-2 text-gray-500 hover:bg-gray-100 hover:text-gray-700 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:ring-offset-2 dark:text-gray-400 dark:hover:bg-gray-700 dark:hover:text-gray-200 dark:focus:ring-offset-gray-800"
						aria-label="Toggle dark mode"
						title="Toggle dark mode"
					>
						{#if isDark}
							<Sun class="w-5 h-5" />
						{:else}
							<Moon class="w-5 h-5" />
						{/if}
					</button>

					<NotificationBell />

					<div class="hidden lg:flex items-center space-x-4">
						<div class="flex items-center space-x-2 min-w-0">
							<a href="/profile" class="max-w-40 truncate text-sm font-medium text-gray-600 hover:text-gray-900 dark:text-gray-300 dark:hover:text-white"
								>{authStore.user.displayName}</a
							>
							{#each authStore.user.roles as role}
								<span
									class="rounded-full px-2 py-0.5 text-xs font-medium {getRoleBadgeColor(role)}"
								>
									{role}
								</span>
							{/each}
						</div>
						<button onclick={handleLogout} class="text-sm text-gray-600 hover:text-gray-900 dark:text-gray-300 dark:hover:text-white">
							Logout
						</button>
					</div>

					<!-- Mobile menu button -->
					<div class="flex items-center lg:hidden">
						<Sheet.Root bind:open>
							<Sheet.Trigger
								class="inline-flex items-center justify-center rounded-md p-2 text-gray-600 hover:bg-gray-100 hover:text-gray-900 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:ring-offset-2 dark:text-gray-300 dark:hover:bg-gray-700 dark:hover:text-white dark:focus:ring-offset-gray-800"
								aria-label="Open main menu"
							>
								<Menu class="h-6 w-6" />
							</Sheet.Trigger>
							<Sheet.Content class="flex h-full w-[min(22rem,calc(100vw-2rem))] flex-col overflow-y-auto p-0 sm:w-[400px]">
								<Sheet.Header>
									<div class="border-b border-gray-200 px-5 pb-4 pt-6 dark:border-gray-700">
										<Sheet.Title>Main menu</Sheet.Title>
										<div class="mt-4 flex items-start gap-3">
											<div class="flex h-10 w-10 shrink-0 items-center justify-center rounded-full bg-blue-100 text-blue-700 dark:bg-blue-900/40 dark:text-blue-200">
												<User class="h-5 w-5" />
											</div>
											<div class="min-w-0">
												<a
													href="/profile"
													class="block truncate text-sm font-semibold text-gray-900 hover:text-blue-700 dark:text-gray-100 dark:hover:text-blue-300"
													onclick={() => (open = false)}
												>
													{authStore.user.displayName}
												</a>
												<div class="mt-2 flex flex-wrap gap-1.5">
													{#each authStore.user.roles as role}
														<span class="rounded-full px-2 py-0.5 text-xs font-medium {getRoleBadgeColor(role)}">
															{role}
														</span>
													{/each}
												</div>
											</div>
										</div>
									</div>
								</Sheet.Header>
								<div class="flex flex-1 flex-col px-3 py-4">
									<div class="mb-3 flex items-center gap-2 rounded-lg border border-gray-200 bg-gray-50 px-3 py-2 text-sm text-gray-500 md:hidden dark:border-gray-700 dark:bg-gray-800 dark:text-gray-300">
										<Search class="h-4 w-4" />
										<span>Use task filters from the Tasks page</span>
									</div>

									<div class="flex flex-col space-y-1">
										{#each navigationSchema as item}
											{@const Icon = item.icon as Component}
											<a
												href={item.href}
												aria-current={isActive(item.href, $page.url.pathname) ? 'page' : undefined}
												class={cn(
													'flex items-center rounded-md px-3 py-3 text-base font-medium text-gray-700 transition-colors hover:bg-gray-100 hover:text-gray-950 focus:outline-none focus:ring-2 focus:ring-blue-500 dark:text-gray-200 dark:hover:bg-gray-800 dark:hover:text-white',
													isActive(item.href, $page.url.pathname) &&
														'bg-blue-50 text-blue-700 dark:bg-blue-950/50 dark:text-blue-200'
												)}
												onclick={() => (open = false)}
											>
												<Icon class="h-5 w-5 mr-3" />
												{item.title}
											</a>
										{/each}
									</div>

									<div class="mt-auto border-t border-gray-200 pt-4 dark:border-gray-700">
										<button
											type="button"
											onclick={toggleDarkMode}
											class="mb-2 flex w-full items-center rounded-md px-3 py-3 text-base font-medium text-gray-700 transition-colors hover:bg-gray-100 hover:text-gray-950 focus:outline-none focus:ring-2 focus:ring-blue-500 dark:text-gray-200 dark:hover:bg-gray-800 dark:hover:text-white"
										>
											{#if isDark}
												<Sun class="mr-3 h-5 w-5" />
												Light mode
											{:else}
												<Moon class="mr-3 h-5 w-5" />
												Dark mode
											{/if}
										</button>
										<a
											href="/profile"
											class="flex items-center rounded-md px-3 py-3 text-base font-medium text-gray-700 transition-colors hover:bg-gray-100 hover:text-gray-950 focus:outline-none focus:ring-2 focus:ring-blue-500 dark:text-gray-200 dark:hover:bg-gray-800 dark:hover:text-white"
											onclick={() => (open = false)}
										>
											<User class="mr-3 h-5 w-5" />
											Profile
										</a>
										<button
											onclick={handleMobileLogout}
											class="flex w-full items-center rounded-md px-3 py-3 text-left text-base font-medium text-gray-700 transition-colors hover:bg-gray-100 hover:text-gray-950 focus:outline-none focus:ring-2 focus:ring-blue-500 dark:text-gray-200 dark:hover:bg-gray-800 dark:hover:text-white"
										>
											<LogOut class="mr-3 h-5 w-5" />
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
