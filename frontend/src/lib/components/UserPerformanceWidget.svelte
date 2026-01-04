<script lang="ts">
	import { onMount } from 'svelte';
	import { api } from '$lib/api/client';

	let data = $state<{ userId: string; tasksCompleted: number; avgDurationHours: number }[]>([]);
	let loading = $state(true);

	onMount(async () => {
		try {
			data = await api.getUserPerformanceAnalytics();
		} catch (e) {
			console.error('Failed to load user analytics', e);
		} finally {
			loading = false;
		}
	});

	function formatDuration(hours: number): string {
		if (hours < 1) return `${Math.round(hours * 60)}m`;
		return `${hours.toFixed(1)}h`;
	}
</script>

<div class="bg-white p-4 rounded-lg shadow h-full overflow-hidden flex flex-col">
	<h3 class="font-semibold text-gray-800 mb-4">Top Contributors</h3>
	
	{#if loading}
		<div class="animate-pulse space-y-4">
			<div class="h-4 bg-gray-200 rounded w-full"></div>
			<div class="h-4 bg-gray-200 rounded w-full"></div>
			<div class="h-4 bg-gray-200 rounded w-full"></div>
		</div>
	{:else if data.length === 0}
		<div class="text-center text-gray-500 py-8">No data available</div>
	{:else}
		<div class="overflow-y-auto flex-1">
			<table class="min-w-full text-sm">
				<thead>
					<tr class="border-b text-gray-500 text-left">
						<th class="py-2 font-medium">User</th>
						<th class="py-2 font-medium text-right">Tasks</th>
						<th class="py-2 font-medium text-right">Avg Time</th>
					</tr>
				</thead>
				<tbody class="divide-y">
					{#each data.slice(0, 10) as user, i}
						<tr>
							<td class="py-2 flex items-center gap-2">
								<span class="w-6 h-6 rounded-full bg-gray-100 flex items-center justify-center text-xs text-gray-600 font-bold">
									{i + 1}
								</span>
								<span class="truncate max-w-[120px]" title={user.userId}>{user.userId}</span>
							</td>
							<td class="py-2 text-right font-medium">{user.tasksCompleted}</td>
							<td class="py-2 text-right text-gray-500">{formatDuration(user.avgDurationHours)}</td>
						</tr>
					{/each}
				</tbody>
			</table>
		</div>
	{/if}
</div>
