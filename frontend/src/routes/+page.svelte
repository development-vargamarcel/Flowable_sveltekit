<script lang="ts">
	import { onMount, onDestroy } from 'svelte';
	import { goto } from '$app/navigation';
	import { api } from '$lib/api/client';
	import { authStore } from '$lib/stores/auth.svelte';
	import { processStore } from '$lib/stores/processes.svelte';
	import TaskList from '$lib/components/TaskList.svelte';
	import ProcessCard from '$lib/components/ProcessCard.svelte';
	import type { Task } from '$lib/types';

	let tasks = $state<Task[]>([]);
	let loading = $state(true);
	let error = $state('');

	// Subscribe to process changes for reactive updates
	let unsubscribe: (() => void) | null = null;

	// Use process instances from store
	const processes = $derived(processStore.myInstances);

	onMount(async () => {
		// Subscribe to process changes from other components
		unsubscribe = processStore.onProcessChange(() => {
			// Force refresh when processes change elsewhere
			loadData(true);
		});

		await loadData();
	});

	onDestroy(() => {
		if (unsubscribe) {
			unsubscribe();
		}
	});

	async function loadData(forceRefresh = false) {
		loading = true;
		error = '';
		try {
			const [tasksData] = await Promise.all([
				api.getTasks(),
				processStore.loadMyInstances(() => api.getMyProcesses(), forceRefresh)
			]);
			tasks = tasksData;
		} catch (err) {
			error = err instanceof Error ? err.message : 'Failed to load data';
		} finally {
			loading = false;
		}
	}

	function handleTaskClick(taskId: string) {
		goto(`/tasks/${taskId}`);
	}
</script>

<svelte:head>
	<title>Dashboard - BPM Demo</title>
</svelte:head>

<div class="max-w-7xl mx-auto px-4 py-8">
	<div class="mb-8">
		<h1 class="text-2xl font-bold text-gray-900">
			Welcome, {authStore.user?.displayName}
		</h1>
		<p class="text-gray-600 mt-1">Here's an overview of your workflow activities</p>
	</div>

	{#if loading}
		<div class="text-center py-12">
			<div class="w-12 h-12 border-4 border-blue-600 border-t-transparent rounded-full animate-spin mx-auto"></div>
			<p class="mt-4 text-gray-600">Loading dashboard...</p>
		</div>
	{:else if error}
		<div class="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded-lg">
			{error}
		</div>
	{:else}
		<!-- Stats -->
		<div class="grid grid-cols-2 md:grid-cols-4 gap-4 mb-8">
			<div class="card text-center">
				<div class="text-3xl font-bold text-blue-600">{tasks.length}</div>
				<div class="text-sm text-gray-600">Total Tasks</div>
			</div>
			<div class="card text-center">
				<div class="text-3xl font-bold text-orange-600">
					{tasks.filter(t => !t.assignee).length}
				</div>
				<div class="text-sm text-gray-600">Unassigned</div>
			</div>
			<div class="card text-center">
				<div class="text-3xl font-bold text-green-600">
					{tasks.filter(t => t.assignee === authStore.user?.username).length}
				</div>
				<div class="text-sm text-gray-600">My Tasks</div>
			</div>
			<div class="card text-center">
				<div class="text-3xl font-bold text-purple-600">{processes.length}</div>
				<div class="text-sm text-gray-600">My Processes</div>
			</div>
		</div>

		<!-- Workflow Dashboard Link -->
		<div class="mb-8">
			<a
				href="/dashboard"
				class="block bg-gradient-to-r from-indigo-500 to-purple-600 rounded-lg p-6 text-white hover:from-indigo-600 hover:to-purple-700 transition-all shadow-lg"
			>
				<div class="flex items-center justify-between">
					<div>
						<h2 class="text-xl font-bold">Workflow Dashboard</h2>
						<p class="text-indigo-100 mt-1">View all past, ongoing, and planned processes with escalation tracking</p>
					</div>
					<svg class="w-8 h-8 text-white opacity-75" fill="none" viewBox="0 0 24 24" stroke="currentColor">
						<path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 19v-6a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2a2 2 0 002-2zm0 0V9a2 2 0 012-2h2a2 2 0 012 2v10m-6 0a2 2 0 002 2h2a2 2 0 002-2m0 0V5a2 2 0 012-2h2a2 2 0 012 2v14a2 2 0 01-2 2h-2a2 2 0 01-2-2z" />
					</svg>
				</div>
			</a>
		</div>

		<!-- Quick Actions -->
		<div class="mb-8">
			<h2 class="text-lg font-semibold text-gray-900 mb-4">Start a New Process</h2>
			<div class="grid sm:grid-cols-2 lg:grid-cols-3 gap-4">
				<ProcessCard
					title="Purchase Request"
					description="Multi-level hierarchical approval"
					icon="🛒"
					color="purple"
					href="/processes/purchase"
				/>
				<ProcessCard
					title="Project Approval"
					description="Multi-stakeholder parallel review"
					icon="📊"
					color="indigo"
					href="/processes/project"
				/>
				<ProcessCard
					title="Expense Request"
					description="Submit an expense for approval"
					icon="$"
					color="green"
					href="/processes/expense"
				/>
				<ProcessCard
					title="Leave Request"
					description="Request time off from work"
					icon="📅"
					color="blue"
					href="/processes/leave"
				/>
				<ProcessCard
					title="Create Task"
					description="Assign a task to team members"
					icon="📋"
					color="amber"
					href="/processes/task"
				/>
			</div>
		</div>

		<!-- Recent Tasks -->
		<div>
			<div class="flex items-center justify-between mb-4">
				<h2 class="text-lg font-semibold text-gray-900">Your Tasks</h2>
				<a href="/tasks" class="text-sm text-blue-600 hover:text-blue-700">View all →</a>
			</div>

			<TaskList
				tasks={tasks.slice(0, 6)}
				onTaskClick={handleTaskClick}
				emptyMessage="No tasks assigned to you"
			/>
		</div>
	{/if}
</div>
