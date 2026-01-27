<script lang="ts">
	import { onMount } from 'svelte';
	import { api } from '$lib/api/client';

	interface Props {
		processDefinitionKey?: string;
	}

	const { processDefinitionKey = '' }: Props = $props();

	let data = $state<{ label: string; count: number }[]>([]);
	let loading = $state(true);
	let maxCount = $state(0);

	onMount(async () => {
		try {
			data = await api.getProcessDurationAnalytics(processDefinitionKey);
			maxCount = Math.max(...data.map(d => d.count), 1); // Avoid div by zero
		} catch (e) {
			console.error('Failed to load duration analytics', e);
		} finally {
			loading = false;
		}
	});
</script>

<div class="bg-white p-4 rounded-lg shadow h-full">
	<h3 class="font-semibold text-gray-800 mb-4">Process Duration Distribution</h3>
	
	{#if loading}
		<div class="animate-pulse space-y-4">
			<div class="h-4 bg-gray-200 rounded w-3/4"></div>
			<div class="h-32 bg-gray-100 rounded"></div>
		</div>
	{:else if data.length === 0}
		<div class="text-center text-gray-500 py-8">No data available</div>
	{:else}
		<div class="flex items-end gap-2 h-48 pt-4">
			{#each data as item}
				<div class="flex-1 flex flex-col items-center group relative">
					<div 
						class="w-full bg-blue-500 rounded-t hover:bg-blue-600 transition-all relative"
						style="height: {(item.count / maxCount) * 100}%"
					>
						<div class="absolute -top-6 left-1/2 -translate-x-1/2 bg-gray-800 text-white text-xs px-2 py-1 rounded opacity-0 group-hover:opacity-100 transition-opacity whitespace-nowrap z-10">
							{item.count} processes
						</div>
					</div>
					<div class="mt-2 text-xs text-gray-500 text-center -rotate-45 origin-top-left translate-y-2 h-8 w-full overflow-visible whitespace-nowrap">
						{item.label}
					</div>
				</div>
			{/each}
		</div>
        <div class="mt-8 text-center text-xs text-gray-400">
            Based on last 1000 finished processes
        </div>
	{/if}
</div>
