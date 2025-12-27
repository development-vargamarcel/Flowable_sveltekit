<script lang="ts">
	import { onMount } from 'svelte';
	import { goto } from '$app/navigation';
	import { api } from '$lib/api/client';
	import type { Dashboard, WorkflowHistory } from '$lib/types';
	import ProcessTimeline from '$lib/components/ProcessTimeline.svelte';
	import EscalationBadge from '$lib/components/EscalationBadge.svelte';

	let dashboard = $state<Dashboard | null>(null);
	let loading = $state(true);
	let error = $state('');
	let activeTab = $state<'all' | 'active' | 'completed' | 'my-approvals'>('all');
	let selectedProcess = $state<WorkflowHistory | null>(null);
	let statusFilter = $state<string>('');
	let typeFilter = $state<string>('');

	onMount(async () => {
		await loadDashboard();
	});

	async function loadDashboard() {
		loading = true;
		error = '';
		try {
			dashboard = await api.getDashboard();
		} catch (err) {
			error = err instanceof Error ? err.message : 'Failed to load dashboard';
		} finally {
			loading = false;
		}
	}

	function getDisplayProcesses(): WorkflowHistory[] {
		if (!dashboard) return [];

		let processes: WorkflowHistory[] = [];
		switch (activeTab) {
			case 'active':
				processes = dashboard.activeProcesses;
				break;
			case 'completed':
				processes = dashboard.recentCompleted;
				break;
			case 'my-approvals':
				processes = dashboard.myPendingApprovals;
				break;
			default:
				processes = [...dashboard.activeProcesses, ...dashboard.recentCompleted];
		}

		if (statusFilter) {
			processes = processes.filter(p => p.status === statusFilter);
		}
		if (typeFilter) {
			processes = processes.filter(p => p.processDefinitionKey === typeFilter);
		}

		return processes;
	}

	function formatDate(dateStr: string): string {
		return new Date(dateStr).toLocaleString();
	}

	function formatDuration(millis: number | null): string {
		if (!millis) return 'N/A';
		const hours = Math.floor(millis / 3600000);
		const minutes = Math.floor((millis % 3600000) / 60000);
		if (hours > 24) {
			const days = Math.floor(hours / 24);
			return `${days}d ${hours % 24}h`;
		}
		return `${hours}h ${minutes}m`;
	}

	function getStatusColor(status: string): string {
		switch (status) {
			case 'ACTIVE':
				return 'bg-blue-100 text-blue-800';
			case 'COMPLETED':
				return 'bg-green-100 text-green-800';
			case 'SUSPENDED':
				return 'bg-yellow-100 text-yellow-800';
			case 'TERMINATED':
				return 'bg-red-100 text-red-800';
			default:
				return 'bg-gray-100 text-gray-800';
		}
	}

	function getProcessTypeIcon(key: string): string {
		switch (key) {
			case 'expense-approval':
				return '$';
			case 'leave-request':
				return '📅';
			case 'purchase-request':
				return '🛒';
			case 'project-approval':
				return '📊';
			case 'task-assignment':
				return '📋';
			default:
				return '📄';
		}
	}

	function viewProcessDetails(process: WorkflowHistory) {
		selectedProcess = process;
	}

	function closeProcessDetails() {
		selectedProcess = null;
	}

	function navigateToTask(taskId: string) {
		goto(`/tasks/${taskId}`);
	}

	let displayProcesses = $derived(getDisplayProcesses());
</script>

<svelte:head>
	<title>Workflow Dashboard - BPM Demo</title>
</svelte:head>

<div class="max-w-7xl mx-auto px-4 py-8">
	<div class="mb-8">
		<h1 class="text-2xl font-bold text-gray-900">Workflow Dashboard</h1>
		<p class="text-gray-600 mt-1">Centralized view of all past, ongoing, and planned processes</p>
	</div>

	{#if loading}
		<div class="text-center py-12">
			<div class="w-12 h-12 border-4 border-blue-600 border-t-transparent rounded-full animate-spin mx-auto"></div>
			<p class="mt-4 text-gray-600">Loading dashboard...</p>
		</div>
	{:else if error}
		<div class="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded-lg">
			{error}
			<button onclick={() => loadDashboard()} class="ml-4 underline">Retry</button>
		</div>
	{:else if dashboard}
		<!-- Stats Overview -->
		<div class="grid grid-cols-2 md:grid-cols-4 lg:grid-cols-7 gap-4 mb-8">
			<div class="card text-center">
				<div class="text-2xl font-bold text-blue-600">{dashboard.stats.totalActive}</div>
				<div class="text-xs text-gray-600">Active</div>
			</div>
			<div class="card text-center">
				<div class="text-2xl font-bold text-green-600">{dashboard.stats.totalCompleted}</div>
				<div class="text-xs text-gray-600">Completed</div>
			</div>
			<div class="card text-center">
				<div class="text-2xl font-bold text-orange-600">{dashboard.stats.totalPending}</div>
				<div class="text-xs text-gray-600">Pending Tasks</div>
			</div>
			<div class="card text-center">
				<div class="text-2xl font-bold text-purple-600">{dashboard.stats.myTasks}</div>
				<div class="text-xs text-gray-600">My Tasks</div>
			</div>
			<div class="card text-center">
				<div class="text-2xl font-bold text-indigo-600">{dashboard.stats.myProcesses}</div>
				<div class="text-xs text-gray-600">My Processes</div>
			</div>
			<div class="card text-center">
				<div class="text-2xl font-bold text-red-600">{dashboard.stats.pendingEscalations}</div>
				<div class="text-xs text-gray-600">Escalated</div>
			</div>
			<div class="card text-center">
				<div class="text-2xl font-bold text-teal-600">{dashboard.stats.avgCompletionTimeHours}h</div>
				<div class="text-xs text-gray-600">Avg. Time</div>
			</div>
		</div>

		<!-- Escalation Metrics -->
		{#if dashboard.escalationMetrics.totalEscalations > 0 || dashboard.escalationMetrics.totalDeEscalations > 0}
			<div class="bg-amber-50 border border-amber-200 rounded-lg p-4 mb-8">
				<h3 class="font-semibold text-amber-800 mb-2">Escalation Metrics</h3>
				<div class="grid grid-cols-2 md:grid-cols-4 gap-4">
					<div>
						<span class="text-2xl font-bold text-amber-700">{dashboard.escalationMetrics.totalEscalations}</span>
						<span class="text-sm text-amber-600 ml-1">Total Escalations</span>
					</div>
					<div>
						<span class="text-2xl font-bold text-green-700">{dashboard.escalationMetrics.totalDeEscalations}</span>
						<span class="text-sm text-green-600 ml-1">De-escalations</span>
					</div>
					<div>
						<span class="text-2xl font-bold text-red-700">{dashboard.escalationMetrics.activeEscalatedProcesses}</span>
						<span class="text-sm text-red-600 ml-1">Currently Escalated</span>
					</div>
					<div class="flex flex-wrap gap-2">
						{#each Object.entries(dashboard.escalationMetrics.escalationsByLevel) as [level, count]}
							<span class="px-2 py-1 bg-amber-100 text-amber-800 rounded text-xs">
								{level}: {count}
							</span>
						{/each}
					</div>
				</div>
			</div>
		{/if}

		<!-- Process Type Distribution -->
		<div class="bg-white rounded-lg shadow p-4 mb-8">
			<h3 class="font-semibold text-gray-800 mb-3">Active Processes by Type</h3>
			<div class="flex flex-wrap gap-3">
				{#each Object.entries(dashboard.activeByType) as [type, count]}
					<button
						onclick={() => { typeFilter = typeFilter === type ? '' : type; }}
						class="flex items-center gap-2 px-3 py-2 rounded-lg transition-all
							{typeFilter === type ? 'bg-blue-600 text-white' : 'bg-gray-100 hover:bg-gray-200'}"
					>
						<span class="text-lg">{getProcessTypeIcon(type)}</span>
						<span class="font-medium">{type.replace(/-/g, ' ')}</span>
						<span class="px-2 py-0.5 rounded-full text-xs font-bold
							{typeFilter === type ? 'bg-blue-500' : 'bg-gray-300'}">
							{count}
						</span>
					</button>
				{/each}
			</div>
		</div>

		<!-- Tab Navigation -->
		<div class="border-b border-gray-200 mb-6">
			<nav class="flex space-x-8">
				{#each [
					{ id: 'all', label: 'All Processes', count: dashboard.activeProcesses.length + dashboard.recentCompleted.length },
					{ id: 'active', label: 'Active', count: dashboard.activeProcesses.length },
					{ id: 'completed', label: 'Completed', count: dashboard.recentCompleted.length },
					{ id: 'my-approvals', label: 'My Pending Approvals', count: dashboard.myPendingApprovals.length }
				] as tab}
					<button
						onclick={() => { activeTab = tab.id as typeof activeTab; }}
						class="py-4 px-1 border-b-2 font-medium text-sm transition-colors
							{activeTab === tab.id
								? 'border-blue-500 text-blue-600'
								: 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'}"
					>
						{tab.label}
						<span class="ml-2 py-0.5 px-2 rounded-full text-xs
							{activeTab === tab.id ? 'bg-blue-100 text-blue-600' : 'bg-gray-100 text-gray-600'}">
							{tab.count}
						</span>
					</button>
				{/each}
			</nav>
		</div>

		<!-- Filter Bar -->
		<div class="flex gap-4 mb-4">
			<select
				bind:value={statusFilter}
				class="px-3 py-2 border border-gray-300 rounded-lg text-sm focus:ring-2 focus:ring-blue-500"
			>
				<option value="">All Statuses</option>
				<option value="ACTIVE">Active</option>
				<option value="COMPLETED">Completed</option>
				<option value="SUSPENDED">Suspended</option>
			</select>
			{#if typeFilter}
				<button
					onclick={() => { typeFilter = ''; }}
					class="px-3 py-2 bg-gray-100 hover:bg-gray-200 rounded-lg text-sm flex items-center gap-2"
				>
					Clear Type Filter: {typeFilter}
					<span class="text-gray-500">×</span>
				</button>
			{/if}
			<button
				onclick={() => loadDashboard()}
				class="ml-auto px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 text-sm"
			>
				Refresh
			</button>
		</div>

		<!-- Process List -->
		<div class="bg-white rounded-lg shadow overflow-hidden">
			<table class="min-w-full divide-y divide-gray-200">
				<thead class="bg-gray-50">
					<tr>
						<th class="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">Process</th>
						<th class="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">Business Key</th>
						<th class="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">Status</th>
						<th class="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">Current Step</th>
						<th class="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">Level</th>
						<th class="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">Started</th>
						<th class="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">Duration</th>
						<th class="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">Actions</th>
					</tr>
				</thead>
				<tbody class="divide-y divide-gray-200">
					{#each displayProcesses as process}
						<tr class="hover:bg-gray-50">
							<td class="px-4 py-3">
								<div class="flex items-center gap-2">
									<span class="text-lg">{getProcessTypeIcon(process.processDefinitionKey)}</span>
									<div>
										<div class="font-medium text-gray-900">
											{process.processDefinitionName || process.processDefinitionKey}
										</div>
										<div class="text-xs text-gray-500">{process.initiatorName || process.initiatorId}</div>
									</div>
								</div>
							</td>
							<td class="px-4 py-3 text-sm font-mono text-gray-600">{process.businessKey}</td>
							<td class="px-4 py-3">
								<span class="px-2 py-1 rounded-full text-xs font-medium {getStatusColor(process.status)}">
									{process.status}
								</span>
								{#if process.escalationCount > 0}
									<EscalationBadge count={process.escalationCount} />
								{/if}
							</td>
							<td class="px-4 py-3 text-sm text-gray-600">
								{#if process.currentTaskName}
									<span class="font-medium">{process.currentTaskName}</span>
									{#if process.currentAssignee}
										<span class="text-gray-400 ml-1">({process.currentAssignee})</span>
									{/if}
								{:else}
									<span class="text-gray-400">-</span>
								{/if}
							</td>
							<td class="px-4 py-3">
								<span class="px-2 py-1 rounded text-xs font-medium bg-indigo-100 text-indigo-800">
									{process.currentLevel}
								</span>
							</td>
							<td class="px-4 py-3 text-sm text-gray-600">{formatDate(process.startTime)}</td>
							<td class="px-4 py-3 text-sm text-gray-600">{formatDuration(process.durationInMillis)}</td>
							<td class="px-4 py-3">
								<div class="flex gap-2">
									<button
										onclick={() => viewProcessDetails(process)}
										class="px-2 py-1 text-xs bg-blue-100 text-blue-700 rounded hover:bg-blue-200"
									>
										Details
									</button>
									{#if process.currentTaskId}
										<button
											onclick={() => navigateToTask(process.currentTaskId!)}
											class="px-2 py-1 text-xs bg-green-100 text-green-700 rounded hover:bg-green-200"
										>
											View Task
										</button>
									{/if}
								</div>
							</td>
						</tr>
					{:else}
						<tr>
							<td colspan="8" class="px-4 py-8 text-center text-gray-500">
								No processes found matching the current filters.
							</td>
						</tr>
					{/each}
				</tbody>
			</table>
		</div>
	{/if}
</div>

<!-- Process Details Modal -->
{#if selectedProcess}
	<div class="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 p-4">
		<div class="bg-white rounded-lg shadow-xl max-w-4xl w-full max-h-[90vh] overflow-y-auto">
			<div class="sticky top-0 bg-white border-b px-6 py-4 flex items-center justify-between">
				<div>
					<h2 class="text-xl font-bold text-gray-900">
						{selectedProcess.processDefinitionName || selectedProcess.processDefinitionKey}
					</h2>
					<p class="text-sm text-gray-500">{selectedProcess.businessKey}</p>
				</div>
				<button
					onclick={() => closeProcessDetails()}
					class="p-2 text-gray-400 hover:text-gray-600"
				>
					<svg class="w-6 h-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
						<path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
					</svg>
				</button>
			</div>

			<div class="p-6">
				<!-- Status and Info -->
				<div class="grid grid-cols-2 md:grid-cols-4 gap-4 mb-6">
					<div class="bg-gray-50 rounded-lg p-3">
						<div class="text-xs text-gray-500 uppercase">Status</div>
						<div class="mt-1">
							<span class="px-2 py-1 rounded-full text-sm font-medium {getStatusColor(selectedProcess.status)}">
								{selectedProcess.status}
							</span>
						</div>
					</div>
					<div class="bg-gray-50 rounded-lg p-3">
						<div class="text-xs text-gray-500 uppercase">Current Level</div>
						<div class="mt-1 font-medium text-indigo-700">{selectedProcess.currentLevel}</div>
					</div>
					<div class="bg-gray-50 rounded-lg p-3">
						<div class="text-xs text-gray-500 uppercase">Escalations</div>
						<div class="mt-1 font-medium text-amber-700">{selectedProcess.escalationCount}</div>
					</div>
					<div class="bg-gray-50 rounded-lg p-3">
						<div class="text-xs text-gray-500 uppercase">Duration</div>
						<div class="mt-1 font-medium">{formatDuration(selectedProcess.durationInMillis)}</div>
					</div>
				</div>

				<!-- Timeline -->
				<div class="mb-6">
					<h3 class="font-semibold text-gray-800 mb-3">Process Timeline</h3>
					<ProcessTimeline
						taskHistory={selectedProcess.taskHistory}
						escalationHistory={selectedProcess.escalationHistory}
						approvals={selectedProcess.approvals}
					/>
				</div>

				<!-- Approvals -->
				{#if selectedProcess.approvals.length > 0}
					<div class="mb-6">
						<h3 class="font-semibold text-gray-800 mb-3">Approval History</h3>
						<div class="space-y-2">
							{#each selectedProcess.approvals as approval}
								<div class="flex items-center justify-between bg-gray-50 rounded-lg p-3">
									<div class="flex items-center gap-3">
										<span class="w-8 h-8 flex items-center justify-center rounded-full
											{approval.decision === 'APPROVED' ? 'bg-green-100 text-green-600' :
											 approval.decision === 'REJECTED' ? 'bg-red-100 text-red-600' :
											 'bg-amber-100 text-amber-600'}">
											{approval.stepOrder}
										</span>
										<div>
											<div class="font-medium">{approval.taskName}</div>
											<div class="text-sm text-gray-500">{approval.approverId} ({approval.approverLevel})</div>
										</div>
									</div>
									<div class="text-right">
										<span class="px-2 py-1 rounded text-xs font-medium
											{approval.decision === 'APPROVED' ? 'bg-green-100 text-green-700' :
											 approval.decision === 'REJECTED' ? 'bg-red-100 text-red-700' :
											 'bg-amber-100 text-amber-700'}">
											{approval.decision}
										</span>
										<div class="text-xs text-gray-500 mt-1">{formatDate(approval.timestamp)}</div>
									</div>
								</div>
							{/each}
						</div>
					</div>
				{/if}

				<!-- Escalation History -->
				{#if selectedProcess.escalationHistory.length > 0}
					<div class="mb-6">
						<h3 class="font-semibold text-gray-800 mb-3">Escalation History</h3>
						<div class="space-y-2">
							{#each selectedProcess.escalationHistory as escalation}
								<div class="flex items-center justify-between bg-amber-50 rounded-lg p-3">
									<div class="flex items-center gap-3">
										<span class="text-lg">
											{escalation.type === 'ESCALATE' ? '⬆️' : '⬇️'}
										</span>
										<div>
											<div class="font-medium">
												{escalation.fromLevel} → {escalation.toLevel}
											</div>
											<div class="text-sm text-gray-600">{escalation.reason}</div>
										</div>
									</div>
									<div class="text-right text-sm text-gray-500">
										<div>{escalation.fromUserId}</div>
										<div>{formatDate(escalation.timestamp)}</div>
									</div>
								</div>
							{/each}
						</div>
					</div>
				{/if}

				<!-- Variables -->
				<div>
					<h3 class="font-semibold text-gray-800 mb-3">Process Variables</h3>
					<div class="bg-gray-50 rounded-lg p-4 overflow-x-auto">
						<pre class="text-sm text-gray-700">{JSON.stringify(selectedProcess.variables, null, 2)}</pre>
					</div>
				</div>
			</div>
		</div>
	</div>
{/if}
