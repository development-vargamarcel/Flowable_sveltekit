<script lang="ts">
  import { onMount } from 'svelte';
  import { api } from '$lib/api/client';

  let bottlenecks = $state<any[]>([]);
  let loading = $state(true);

  onMount(async () => {
    try {
      bottlenecks = await api.getBottlenecks();
    } catch (e) {
      console.error('Failed to load bottlenecks', e);
    } finally {
      loading = false;
    }
  });

  function formatDuration(hours: number) {
    if (hours < 1) return `${Math.round(hours * 60)} min`;
    if (hours < 24) return `${hours.toFixed(1)} hrs`;
    return `${(hours / 24).toFixed(1)} days`;
  }
</script>

<div class="rounded-lg bg-white p-6 shadow-sm">
  <h3 class="mb-4 text-lg font-medium text-gray-900">Task Bottlenecks</h3>
  
  {#if loading}
    <div class="animate-pulse space-y-3">
      <div class="h-4 w-3/4 rounded bg-gray-200"></div>
      <div class="h-4 w-1/2 rounded bg-gray-200"></div>
    </div>
  {:else if bottlenecks.length === 0}
    <p class="text-sm text-gray-500">No bottlenecks detected (need more history).</p>
  {:else}
    <div class="overflow-x-auto">
      <table class="min-w-full divide-y divide-gray-200">
        <thead>
          <tr>
            <th class="px-3 py-2 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Process</th>
            <th class="px-3 py-2 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Task</th>
            <th class="px-3 py-2 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Avg Time</th>
            <th class="px-3 py-2 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Outliers</th>
          </tr>
        </thead>
        <tbody class="bg-white divide-y divide-gray-200">
          {#each bottlenecks as b}
            <tr>
              <td class="px-3 py-2 whitespace-nowrap text-sm text-gray-500">{b.processDefinitionKey}</td>
              <td class="px-3 py-2 whitespace-nowrap text-sm font-medium text-gray-900">{b.taskName}</td>
              <td class="px-3 py-2 whitespace-nowrap text-sm text-gray-500">
                <span class={b.avgDurationHours > 24 ? "text-red-500 font-bold" : ""}>
                    {formatDuration(b.avgDurationHours)}
                </span>
              </td>
              <td class="px-3 py-2 whitespace-nowrap text-sm text-gray-500">
                <span class="inline-flex items-center px-2 py-0.5 rounded text-xs font-medium bg-yellow-100 text-yellow-800">
                    {b.slowInstanceCount} / {b.totalInstances}
                </span>
              </td>
            </tr>
          {/each}
        </tbody>
      </table>
    </div>
  {/if}
</div>
