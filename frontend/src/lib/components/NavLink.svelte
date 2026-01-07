<script lang="ts">
	import type { NavigationItem } from '$lib/nav-schema';
	import { page } from '$app/stores';
	import { ChevronDown } from '@lucide/svelte';
	import * as DropdownMenu from '$lib/components/ui/dropdown-menu';
	import { cn } from '$lib/utils';

	let { item }: { item: NavigationItem } = $props();

	function isActive(href: string, currentPath: string): boolean {
		if (href === '/') {
			return currentPath === '/';
		}
		return currentPath.startsWith(href);
	}
</script>

{#if item.children}
	<DropdownMenu.Root>
		<DropdownMenu.Trigger
			class={cn(
				'flex items-center space-x-1 px-3 py-2 text-sm font-medium text-gray-600 hover:text-gray-900 hover:bg-gray-50 rounded-md',
				item.children.some((child) => isActive(child.href, $page.url.pathname)) &&
					'bg-gray-100 text-gray-900'
			)}
		>
			<span>{item.title}</span>
			<ChevronDown class="h-4 w-4" />
		</DropdownMenu.Trigger>
		<DropdownMenu.Content>
			{#each item.children as child}
				<DropdownMenu.Item href={child.href}>
					<svelte:component this={child.icon} class="h-4 w-4 mr-2" />
					{child.title}
				</DropdownMenu.Item>
			{/each}
		</DropdownMenu.Content>
	</DropdownMenu.Root>
{:else}
	<a
		href={item.href}
		class={cn(
			'px-3 py-2 text-sm font-medium text-gray-600 hover:text-gray-900 hover:bg-gray-50 rounded-md',
			isActive(item.href, $page.url.pathname) && 'bg-gray-100 text-gray-900'
		)}
	>
		{item.title}
	</a>
{/if}
