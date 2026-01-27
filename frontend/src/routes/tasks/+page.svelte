<script lang="ts">
	import { onMount } from 'svelte';
	import { goto } from '$app/navigation';
	import { api } from '$lib/api/client';
	import { authStore } from '$lib/stores/auth.svelte';
	import TaskList from '$lib/components/TaskList.svelte';
	import type { Task } from '$lib/types';
	import Loading from '$lib/components/Loading.svelte';
	import ErrorDisplay from '$lib/components/ErrorDisplay.svelte';

	let allTasks = $state<Task[]>([]);
	let loading = $state(true);
	let error = $state('');
	let activeTab = $state<'all' | 'assigned' | 'claimable'>('all');
	let searchQuery = $state('');
	let sortOption = $state<'newest' | 'oldest' | 'priority'>('newest');

	const filteredTasks = $derived.by(() => {
		let tasks = allTasks;

		// Filter by tab
		switch (activeTab) {
			case 'assigned':
				tasks = tasks.filter((t) => t.assignee === authStore.user?.username);
				break;
			case 'claimable':
				tasks = tasks.filter((t) => !t.assignee);
				break;
		}

		// Filter by search query
		if (searchQuery) {
			const query = searchQuery.toLowerCase();
			tasks = tasks.filter(
				(t) =>
					t.name.toLowerCase().includes(query) ||
					(t.assignee && t.assignee.toLowerCase().includes(query)) ||
					(t.description && t.description.toLowerCase().includes(query))
			);
		}

		// Sort
		return [...tasks].sort((a, b) => {
			if (sortOption === 'priority') {
				// Higher priority number first
				return b.priority - a.priority;
			} else if (sortOption === 'oldest') {
				return new Date(a.createTime).getTime() - new Date(b.createTime).getTime();
			} else {
				// newest
				return new Date(b.createTime).getTime() - new Date(a.createTime).getTime();
			}
		});
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

	<!-- Tabs & Filters -->
	<div class="flex flex-col lg:flex-row lg:items-center justify-between gap-4 mb-6">
		<div class="flex space-x-2 overflow-x-auto pb-1 lg:pb-0">
			<button class={getTabClass('all')} onclick={() => (activeTab = 'all')}>
				All ({allTasks.length})
			</button>
			<button class={getTabClass('assigned')} onclick={() => (activeTab = 'assigned')}>
				My Tasks ({allTasks.filter((t) => t.assignee === authStore.user?.username).length})
			</button>
			<button class={getTabClass('claimable')} onclick={() => (activeTab = 'claimable')}>
				Available ({allTasks.filter((t) => !t.assignee).length})
			</button>
		</div>

		<div class="flex flex-col sm:flex-row gap-2">
			<div class="relative">
				<div class="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
					<svg class="h-4 w-4 text-gray-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
						<path
							stroke-linecap="round"
							stroke-linejoin="round"
							stroke-width="2"
							d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z"
						/>
					</svg>
				</div>
				<input
					type="text"
					bind:value={searchQuery}
					placeholder="Search tasks..."
					class="pl-10 px-3 py-2 border border-gray-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-blue-500 w-full sm:w-64"
				/>
			</div>
			<select
				bind:value={sortOption}
				class="px-3 py-2 border border-gray-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-blue-500 bg-white"
			>
				<option value="newest">Newest First</option>
				<option value="oldest">Oldest First</option>
				<option value="priority">Priority</option>
			</select>
		</div>
	</div>

	{#if loading}
		<Loading text="Loading tasks..." />
	{:else if error}
		<ErrorDisplay {error} onRetry={loadTasks} title="Error Loading Tasks" />
	{:else}
		<TaskList
			tasks={filteredTasks}
			onTaskClick={handleTaskClick}
			emptyMessage={activeTab === 'assigned'
				? "You don't have any assigned tasks"
				: activeTab === 'claimable'
					? 'No tasks available to claim'
					: 'No tasks found'}
		/>
	{/if}
</div>
