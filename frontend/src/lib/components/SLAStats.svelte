<script lang="ts">
  import { onMount } from 'svelte';
  import { api } from '$lib/api/client';

  let overdueTasks = $state(0);
  let nearBreachTasks = $state(0);
  let healthScore = $state(100);
  let loading = $state(true);
  let error = $state<string | null>(null);
  let isFirstLoad = $state(true);

  async function loadStats() {
    loading = true;
    error = null;
    try {
      const stats = await api.getSlaStats();
      overdueTasks = stats.breachedSlas;
      nearBreachTasks = stats.atRiskSlas;
      if (stats.totalActiveSlas > 0) {
        healthScore = Math.round(((stats.totalActiveSlas - stats.breachedSlas) / stats.totalActiveSlas) * 100);
      } else {
        healthScore = 100;
      }
    } catch (e) {
      console.error('Failed to load SLA stats', e);
      error = 'Failed to load SLA statistics';
    } finally {
      loading = false;
      isFirstLoad = false;
    }
  }

  async function handleRefresh() {
      // Trigger backend check then reload stats
      loading = true;
      try {
          await api.checkSLABreaches();
          await loadStats();
      } catch (e) {
          console.error(e);
          // loadStats handles its own error setting, but checkSLABreaches might fail too
      } finally {
          loading = false;
      }
  }

  onMount(() => {
    loadStats();
    // Trigger a check in background without blocking initial load if possible,
    // or just rely on loadStats getting current DB state.
    // The original code called checkSLABreaches on mount.
    api.checkSLABreaches().catch(console.error);
  });
</script>

<div class="bg-white rounded-lg shadow-sm border border-gray-200 p-4 mb-6">
  <div class="flex items-center justify-between mb-4">
    <h3 class="text-lg font-semibold text-gray-900">SLA Overview</h3>
    <button onclick={handleRefresh} class="text-sm text-blue-600 hover:text-blue-800 disabled:opacity-50" disabled={loading}>
      {loading ? 'Refreshing...' : 'Refresh Check'}
    </button>
  </div>

  {#if isFirstLoad}
      <div class="flex justify-center py-8">
          <div class="w-8 h-8 border-4 border-blue-600 border-t-transparent rounded-full animate-spin"></div>
      </div>
  {:else if error}
      <div class="bg-red-50 text-red-700 p-4 rounded-lg flex justify-between items-center border border-red-100">
          <span class="text-sm">{error}</span>
          <button onclick={loadStats} class="text-sm font-medium hover:text-red-900">Retry</button>
      </div>
  {:else}
      <div class="grid grid-cols-1 md:grid-cols-3 gap-4">
        <div class="bg-red-50 p-4 rounded-lg border border-red-100 transition-all hover:shadow-sm">
          <div class="text-red-800 text-sm font-medium mb-1">Overdue Tasks</div>
          <div class="text-3xl font-bold text-red-600">{overdueTasks}</div>
          <div class="text-xs text-red-500 mt-1">Breached SLA</div>
        </div>

        <div class="bg-yellow-50 p-4 rounded-lg border border-yellow-100 transition-all hover:shadow-sm">
          <div class="text-yellow-800 text-sm font-medium mb-1">Warning Zone</div>
          <div class="text-3xl font-bold text-yellow-600">{nearBreachTasks}</div>
          <div class="text-xs text-yellow-500 mt-1">> 80% SLA time used</div>
        </div>

        <div class="bg-blue-50 p-4 rounded-lg border border-blue-100 transition-all hover:shadow-sm">
          <div class="text-blue-800 text-sm font-medium mb-1">Health Score</div>
          <div class="text-3xl font-bold text-blue-600">{healthScore}%</div>
          <div class="text-xs text-blue-500 mt-1">Tasks within SLA</div>
        </div>
      </div>
  {/if}
</div>
