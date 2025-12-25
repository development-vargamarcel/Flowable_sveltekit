<script lang="ts">
	import { onMount } from 'svelte';
	import { goto } from '$app/navigation';
	import { api } from '$lib/api/client';
	import { authStore } from '$lib/stores/auth.svelte';
	import TaskList from '$lib/components/TaskList.svelte';
	import type { Task } from '$lib/types';

	let allTasks = $state<Task[]>([]);
	let loading = $state(true);
	let error = $state('');
	let activeTab = $state<'all' | 'assigned' | 'claimable'>('all');

	let filteredTasks = $derived(() => {
		switch (activeTab) {
			case 'assigned':
				return allTasks.filter(t => t.assignee === authStore.user?.username);
			case 'claimable':
				return allTasks.filter(t => !t.assignee);
			default:
				return allTasks;
		}
	});

	onMount(async () => {
		await loadTasks();
	});

	async function loadTasks() {
		loading = true;
		error = '';
		try {
			allTasks = await api.getTasks();
		} catch (err) {
			error = err instanceof Error ? err.message : 'Failed to load tasks';
		} finally {
			loading = false;
		}
	}

	function handleTaskClick(taskId: string) {
		goto(`/tasks/${taskId}`);
	}

	function getTabClass(tab: string): string {
		const base = 'px-4 py-2 text-sm font-medium rounded-lg transition-colors';
		return activeTab === tab
			? `${base} bg-blue-600 text-white`
			: `${base} text-gray-600 hover:bg-gray-100`;
	}
</script>

<svelte:head>
	<title>Tasks - BPM Demo</title>
</svelte:head>

<div class="max-w-7xl mx-auto px-4 py-8">
	<div class="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 mb-6">
		<div>
			<h1 class="text-2xl font-bold text-gray-900">Tasks</h1>
			<p class="text-gray-600 mt-1">Manage your workflow tasks</p>
		</div>

		<button onclick={loadTasks} class="btn btn-secondary" disabled={loading}>
			{loading ? 'Refreshing...' : 'Refresh'}
		</button>
	</div>

	<!-- Tabs -->
	<div class="flex space-x-2 mb-6">
		<button class={getTabClass('all')} onclick={() => activeTab = 'all'}>
			All ({allTasks.length})
		</button>
		<button class={getTabClass('assigned')} onclick={() => activeTab = 'assigned'}>
			My Tasks ({allTasks.filter(t => t.assignee === authStore.user?.username).length})
		</button>
		<button class={getTabClass('claimable')} onclick={() => activeTab = 'claimable'}>
			Available ({allTasks.filter(t => !t.assignee).length})
		</button>
	</div>

	{#if loading}
		<div class="text-center py-12">
			<div class="w-12 h-12 border-4 border-blue-600 border-t-transparent rounded-full animate-spin mx-auto"></div>
			<p class="mt-4 text-gray-600">Loading tasks...</p>
		</div>
	{:else if error}
		<div class="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded-lg">
			{error}
		</div>
	{:else}
		<TaskList
			tasks={filteredTasks()}
			onTaskClick={handleTaskClick}
			emptyMessage={activeTab === 'assigned'
				? "You don't have any assigned tasks"
				: activeTab === 'claimable'
					? "No tasks available to claim"
					: "No tasks found"}
		/>
	{/if}
</div>
