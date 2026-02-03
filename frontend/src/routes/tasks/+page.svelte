<script lang="ts">
	import { onMount } from 'svelte';
	import { goto } from '$app/navigation';
	import { api } from '$lib/api/client';
	import { toast } from 'svelte-sonner';
	import TaskList from '$lib/components/TaskList.svelte';
	import TaskFilters from '$lib/components/TaskFilters.svelte';
	import DelegateTaskModal from '$lib/components/DelegateTaskModal.svelte';
	import type { Task } from '$lib/types';
	import Loading from '$lib/components/Loading.svelte';
	import ErrorDisplay from '$lib/components/ErrorDisplay.svelte';
	import { Download } from '@lucide/svelte';
	import { exportToCSV } from '$lib/utils';

	let allTasks = $state<Task[]>([]);
	let loading = $state(true);
	let error = $state('');
	
	let filters = $state({
		text: '',
		assignee: '',
		priority: ''
	});

	let showDelegateModal = $state(false);
	let delegateTaskId = $state<string | null>(null);

	onMount(async () => {
		await loadTasks();
	});

	async function loadTasks() {
		loading = true;
		error = '';
		try {
			// Convert filters to API params
			// If filters.assignee is empty, we don't filter by assignee.
			// The original "Tabs" logic was client-side or specific endpoints.
			// Now we use the powerful search endpoint.
			
			const apiFilters: any = {};
			if (filters.text) apiFilters.text = filters.text;
			if (filters.assignee) apiFilters.assignee = filters.assignee;
			if (filters.priority) apiFilters.priority = Number(filters.priority);

			allTasks = await api.getTasks(apiFilters);
		} catch (err) {
			error = err instanceof Error ? err.message : 'Failed to load tasks';
		} finally {
			loading = false;
		}
	}

	function handleExport() {
		if (allTasks.length === 0) {
			toast.error('No tasks to export');
			return;
		}
		const exportData = allTasks.map(t => ({
			ID: t.id,
			Name: t.name,
			Assignee: t.assignee || 'Unassigned',
			Priority: t.priority,
			Created: t.createTime
		}));
		exportToCSV(exportData, `tasks_export_${new Date().toISOString().split('T')[0]}.csv`);
		toast.success('Tasks exported successfully');
	}

	function handleFilterChange(event: CustomEvent) {
		filters = event.detail;
		loadTasks();
	}

	function handleTaskClick(taskId: string) {
		goto(`/tasks/${taskId}`);
	}

	async function handleClaim(taskId: string) {
		try {
			await api.claimTask(taskId);
			toast.success('Task claimed successfully');
			await loadTasks();
		} catch (e) {
			toast.error('Failed to claim task');
		}
	}

	async function handleUnclaim(taskId: string) {
		try {
			await api.unclaimTask(taskId);
			toast.success('Task unclaimed successfully');
			await loadTasks();
		} catch (e) {
			toast.error('Failed to unclaim task');
		}
	}

	function handleDelegate(taskId: string) {
		delegateTaskId = taskId;
		showDelegateModal = true;
	}

	function onDelegateSuccess() {
		loadTasks();
	}

	async function handleBulkClaim(taskIds: string[]) {
		if (!confirm(`Are you sure you want to claim ${taskIds.length} tasks?`)) return;
		
		loading = true;
		try {
			await Promise.all(taskIds.map(id => api.claimTask(id)));
			toast.success(`Successfully claimed ${taskIds.length} tasks`);
			await loadTasks();
		} catch (e) {
			console.error('Bulk claim failed:', e);
			toast.error('Failed to claim some tasks');
			await loadTasks(); // Reload to reflect partial success
		} finally {
			loading = false;
		}
	}

	async function handleBulkUnclaim(taskIds: string[]) {
		if (!confirm(`Are you sure you want to unclaim ${taskIds.length} tasks?`)) return;

		loading = true;
		try {
			await Promise.all(taskIds.map(id => api.unclaimTask(id)));
			toast.success(`Successfully unclaimed ${taskIds.length} tasks`);
			await loadTasks();
		} catch (e) {
			console.error('Bulk unclaim failed:', e);
			toast.error('Failed to unclaim some tasks');
			await loadTasks();
		} finally {
			loading = false;
		}
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

		<div class="flex gap-2">
			<button onclick={handleExport} class="btn btn-outline flex items-center gap-2" disabled={loading || allTasks.length === 0}>
				<Download class="w-4 h-4" />
				Export CSV
			</button>
			<button onclick={loadTasks} class="btn btn-secondary" disabled={loading}>
				{loading ? 'Refreshing...' : 'Refresh'}
			</button>
		</div>
	</div>

    <TaskFilters on:change={handleFilterChange} />

	{#if loading}
		<Loading text="Loading tasks..." />
	{:else if error}
		<ErrorDisplay {error} onRetry={loadTasks} title="Error Loading Tasks" />
	{:else}
		<TaskList
			tasks={allTasks}
			onTaskClick={handleTaskClick}
			onClaim={handleClaim}
			onUnclaim={handleUnclaim}
			onDelegate={handleDelegate}
			onBulkClaim={handleBulkClaim}
			onBulkUnclaim={handleBulkUnclaim}
			emptyMessage="No tasks found matching your filters."
		/>
	{/if}

	<DelegateTaskModal
		open={showDelegateModal}
		taskId={delegateTaskId}
		onClose={() => (showDelegateModal = false)}
		onSuccess={onDelegateSuccess}
	/>
</div>
