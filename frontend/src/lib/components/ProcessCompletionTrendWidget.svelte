<script lang="ts">
	import { onMount } from 'svelte';
	import { api } from '$lib/api/client';

	let data = $state<{ date: string; count: number }[]>([]);
	let loading = $state(true);
	let maxCount = $state(0);
    let days = $state(7);

    async function loadData() {
        loading = true;
        try {
			data = await api.getProcessCompletionTrend(days);
			maxCount = Math.max(...data.map(d => d.count), 1); // Avoid div by zero
		} catch (e) {
			console.error('Failed to load completion trend', e);
		} finally {
			loading = false;
		}
    }

	onMount(async () => {
		await loadData();
	});
</script>

<div class="bg-white p-4 rounded-lg shadow h-full flex flex-col">
    <div class="flex justify-between items-center mb-4">
	    <h3 class="font-semibold text-gray-800">Completion Trend</h3>
        <select
            bind:value={days}
            onchange={loadData}
            class="text-xs border border-gray-300 rounded px-2 py-1"
        >
            <option value={7}>Last 7 Days</option>
            <option value={14}>Last 14 Days</option>
            <option value={30}>Last 30 Days</option>
        </select>
    </div>

	{#if loading}
		<div class="animate-pulse space-y-4 flex-1">
			<div class="h-4 bg-gray-200 rounded w-3/4"></div>
			<div class="h-32 bg-gray-100 rounded"></div>
		</div>
	{:else if data.length === 0}
		<div class="text-center text-gray-500 py-8 flex-1">No data available</div>
	{:else}
		<div class="flex items-end gap-2 h-48 pt-4 flex-1">
			{#each data as item}
				<div class="flex-1 flex flex-col items-center group relative h-full justify-end">
					<div
						class="w-full bg-green-500 rounded-t hover:bg-green-600 transition-all relative"
						style="height: {(item.count / maxCount) * 100}%"
					>
						<div class="absolute -top-8 left-1/2 -translate-x-1/2 bg-gray-800 text-white text-xs px-2 py-1 rounded opacity-0 group-hover:opacity-100 transition-opacity whitespace-nowrap z-10 pointer-events-none">
							<div class="font-bold">{item.count}</div>
                            <div class="text-[10px]">{item.date}</div>
						</div>
					</div>
					<div class="mt-2 text-[10px] text-gray-500 text-center -rotate-45 origin-top-left translate-y-2 h-8 w-full overflow-visible whitespace-nowrap">
						{item.date.slice(5)}
					</div>
				</div>
			{/each}
		</div>
	{/if}
</div>
