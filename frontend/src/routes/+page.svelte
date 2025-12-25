<script lang="ts">
	import { onMount } from 'svelte';
	import { goto } from '$app/navigation';
	import { api } from '$lib/api/client';
	import { authStore } from '$lib/stores/auth.svelte';
	import TaskList from '$lib/components/TaskList.svelte';
	import ProcessCard from '$lib/components/ProcessCard.svelte';
	import type { Task, ProcessInstance } from '$lib/types';

	let tasks = $state<Task[]>([]);
	let processes = $state<ProcessInstance[]>([]);
	let loading = $state(true);
	let error = $state('');

	onMount(async () => {
		await loadData();
	});

	async function loadData() {
		loading = true;
		error = '';
		try {
			const [tasksData, processesData] = await Promise.all([
				api.getTasks(),
				api.getMyProcesses()
			]);
			tasks = tasksData;
			processes = processesData;
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

		<!-- Quick Actions -->
		<div class="mb-8">
			<h2 class="text-lg font-semibold text-gray-900 mb-4">Start a New Process</h2>
			<div class="grid sm:grid-cols-3 gap-4">
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
