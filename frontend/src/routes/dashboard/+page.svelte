<script lang="ts">
	import { onMount, onDestroy } from 'svelte';
	import { goto } from '$app/navigation';
	import { api, ApiError } from '$lib/api/client';
	import { processStore } from '$lib/stores/processes.svelte';
	import type { WorkflowHistory, Page, SlaStats } from '$lib/types';
	import EscalationBadge from '$lib/components/EscalationBadge.svelte';
	import SLAStats from '$lib/components/SLAStats.svelte';
	import Pagination from '$lib/components/Pagination.svelte';
	import DurationHistogram from '$lib/components/DurationHistogram.svelte';
	import UserPerformanceWidget from '$lib/components/UserPerformanceWidget.svelte';
	import BottleneckWidget from '$lib/components/BottleneckWidget.svelte';
	import ErrorDisplay from '$lib/components/ErrorDisplay.svelte';
	import EmptyState from '$lib/components/EmptyState.svelte';
	import ProcessDetailsModal from '$lib/components/ProcessDetailsModal.svelte';
	import DashboardSkeleton from '$lib/components/DashboardSkeleton.svelte';
	import { Card, CardContent } from '$lib/components/ui/card';

	// Loading states - separate for initial load vs refresh
	let initialLoading = $state(true);
	let refreshing = $state(false);
	let error = $state<ApiError | string | null>(null);
	let activeTab = $state<'all' | 'active' | 'completed' | 'my-approvals'>('all');
	let selectedProcess = $state<WorkflowHistory | null>(null);
	let statusFilter = $state<string>('');
	let typeFilter = $state<string>('');
	let currentPage = $state(0);

	// Subscribe to process changes for reactive updates
	let unsubscribe: (() => void) | null = null;

	// Use dashboard from store - show stale data while refreshing (SWR pattern)
	const dashboard = $derived(processStore.dashboard);
	
	// Show loading skeleton only on initial load when no data exists
	const showSkeleton = $derived(initialLoading && !dashboard);
	
	// Show refresh indicator when we have data but are updating
	const isRefreshing = $derived(refreshing && dashboard !== null);

	onMount(async () => {
		// Subscribe to process changes from other components
		unsubscribe = processStore.onProcessChange(() => {
			// Force refresh when processes change elsewhere (background refresh)
			loadDashboard(true, 0, true);
		});

		await loadDashboard(false, 0, false);
	});

	onDestroy(() => {
		if (unsubscribe) {
			unsubscribe();
		}
	});

	/**
	 * Load dashboard with SWR (stale-while-revalidate) pattern
	 * - Shows existing data immediately
	 * - Fetches fresh data in background
	 * - Updates UI when fresh data arrives
	 */
	async function loadDashboard(forceRefresh = false, page = 0, isBackgroundRefresh = false) {
		// Set appropriate loading state
		if (isBackgroundRefresh || dashboard) {
			refreshing = true;
		} else {
			initialLoading = true;
		}
		
		// Clear error only on non-background refresh
		if (!isBackgroundRefresh) {
			error = null;
		}
		
		try {
			await processStore.loadDashboard(
				() => api.getDashboard(page, 10, statusFilter, typeFilter),
				forceRefresh
			);
			currentPage = page;
			// Clear any previous errors on success
			error = null;
		} catch (err) {
			// Only show error if we don't have stale data to show
			if (!dashboard) {
				if (err instanceof ApiError) {
					error = err;
				} else {
					error = err instanceof Error ? err.message : 'Failed to load dashboard';
				}
			} else {
				// Log error but don't disrupt the UI if we have stale data
				console.warn('Dashboard refresh failed, showing stale data:', err);
			}
		} finally {
			initialLoading = false;
			refreshing = false;
		}
	}

	function getDisplayProcesses(): Page<WorkflowHistory> | null {
		if (!dashboard) return null;

		switch (activeTab) {
			case 'active':
				return dashboard.activeProcesses;
			case 'completed':
				return dashboard.recentCompleted;
			case 'my-approvals':
				return dashboard.myPendingApprovals;
			default:
				return dashboard.activeProcesses;
		}
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

	function handlePageChange(page: number) {
		loadDashboard(false, page);
	}
</script>

<svelte:head>
	<title>Workflow Dashboard - BPM Demo</title>
</svelte:head>

<!-- Show skeleton on initial load without data -->
{#if showSkeleton}
	<DashboardSkeleton />
{:else}
	<div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-4 sm:py-8">
		<!-- Header with refresh indicator -->
		<div class="mb-6 sm:mb-8 flex flex-col sm:flex-row sm:items-center sm:justify-between gap-2">
			<div>
				<h1 class="text-xl sm:text-2xl font-bold text-gray-900">Workflow Dashboard</h1>
				<p class="text-sm sm:text-base text-gray-600 mt-1">Centralized view of all past, ongoing, and planned processes</p>
			</div>
			{#if isRefreshing}
				<div class="flex items-center gap-2 text-sm text-gray-500">
					<div class="w-4 h-4 border-2 border-blue-600 border-t-transparent rounded-full animate-spin"></div>
					<span>Updating...</span>
				</div>
			{/if}
		</div>

		{#if error && !dashboard}
			<ErrorDisplay {error} onRetry={() => loadDashboard()} title="Error Loading Dashboard" />
		{:else if dashboard}
		<SLAStats />

		<!-- Stats Overview -->
		<div class="grid grid-cols-2 md:grid-cols-4 lg:grid-cols-7 gap-4 mb-8">
			<Card class="text-center">
				<CardContent class="pt-6">
					<div class="text-2xl font-bold text-blue-600">{dashboard.stats.totalActive}</div>
					<div class="text-xs text-gray-600">Active</div>
				</CardContent>
			</Card>
			<Card class="text-center">
				<CardContent class="pt-6">
					<div class="text-2xl font-bold text-green-600">{dashboard.stats.totalCompleted}</div>
					<div class="text-xs text-gray-600">Completed</div>
				</CardContent>
			</Card>
			<Card class="text-center">
				<CardContent class="pt-6">
					<div class="text-2xl font-bold text-orange-600">{dashboard.stats.totalPending}</div>
					<div class="text-xs text-gray-600">Pending Tasks</div>
				</CardContent>
			</Card>
			<Card class="text-center">
				<CardContent class="pt-6">
					<div class="text-2xl font-bold text-purple-600">{dashboard.stats.myTasks}</div>
					<div class="text-xs text-gray-600">My Tasks</div>
				</CardContent>
			</Card>
			<Card class="text-center">
				<CardContent class="pt-6">
					<div class="text-2xl font-bold text-indigo-600">{dashboard.stats.myProcesses}</div>
					<div class="text-xs text-gray-600">My Processes</div>
				</CardContent>
			</Card>
			<Card class="text-center">
				<CardContent class="pt-6">
					<div class="text-2xl font-bold text-red-600">{dashboard.stats.pendingEscalations}</div>
					<div class="text-xs text-gray-600">Escalated</div>
				</CardContent>
			</Card>
			<Card class="text-center">
				<CardContent class="pt-6">
					<div class="text-2xl font-bold text-teal-600">
						{dashboard.stats.avgCompletionTimeHours}h
					</div>
					<div class="text-xs text-gray-600">Avg. Time</div>
				</CardContent>
			</Card>
		</div>

		<!-- Escalation Metrics -->
		{#if dashboard.escalationMetrics.totalEscalations > 0 || dashboard.escalationMetrics.totalDeEscalations > 0}
			<div class="bg-amber-50 border border-amber-200 rounded-lg p-4 mb-8">
				<h3 class="font-semibold text-amber-800 mb-2">Escalation Metrics</h3>
				<div class="grid grid-cols-2 md:grid-cols-4 gap-4">
					<div>
						<span class="text-2xl font-bold text-amber-700"
							>{dashboard.escalationMetrics.totalEscalations}</span
						>
						<span class="text-sm text-amber-600 ml-1">Total Escalations</span>
					</div>
					<div>
						<span class="text-2xl font-bold text-green-700"
							>{dashboard.escalationMetrics.totalDeEscalations}</span
						>
						<span class="text-sm text-green-600 ml-1">De-escalations</span>
					</div>
					<div>
						<span class="text-2xl font-bold text-red-700"
							>{dashboard.escalationMetrics.activeEscalatedProcesses}</span
						>
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

		<!-- Row 3: Analytics Widgets -->
		<div class="mb-8 grid gap-6 lg:grid-cols-2">
			<DurationHistogram processDefinitionKey="" />
			<UserPerformanceWidget />
		</div>

		<!-- Row 4: Bottleneck Analysis -->
		<div class="mb-8">
			<BottleneckWidget />
		</div>

		<!-- Process Type Distribution -->
		<div class="bg-white rounded-lg shadow p-4 mb-8">
			<h3 class="font-semibold text-gray-800 mb-3">Active Processes by Type</h3>
			<div class="flex flex-wrap gap-3">
				{#each Object.entries(dashboard.activeByType) as [type, count]}
					<button
						onclick={() => {
							typeFilter = typeFilter === type ? '' : type;
						}}
						class="flex items-center gap-2 px-3 py-2 rounded-lg transition-all
							{typeFilter === type
							? 'bg-blue-600 text-white'
							: 'bg-gray-100 hover:bg-gray-200'}"
					>
						<span class="text-lg">{getProcessTypeIcon(type)}</span>
						<span class="font-medium">{type.replace(/-/g, ' ')}</span>
						<span
							class="px-2 py-0.5 rounded-full text-xs font-bold
							{typeFilter === type ? 'bg-blue-500' : 'bg-gray-300'}"
						>
							{count}
						</span>
					</button>
				{/each}
			</div>
		</div>

		<!-- Tab Navigation - Scrollable on mobile -->
		<div class="border-b border-gray-200 mb-4 sm:mb-6 -mx-4 sm:mx-0 px-4 sm:px-0">
			<nav class="flex space-x-4 sm:space-x-8 overflow-x-auto pb-px scrollbar-hide" role="tablist" aria-label="Process filters">
				{#each [{ id: 'all', label: 'All Processes', shortLabel: 'All', count: dashboard.activeProcesses.totalElements + dashboard.recentCompleted.totalElements }, { id: 'active', label: 'Active', shortLabel: 'Active', count: dashboard.activeProcesses.totalElements }, { id: 'completed', label: 'Completed', shortLabel: 'Done', count: dashboard.recentCompleted.totalElements }, { id: 'my-approvals', label: 'My Pending Approvals', shortLabel: 'My Approvals', count: dashboard.myPendingApprovals.totalElements }] as tab}
					<button
						role="tab"
						aria-selected={activeTab === tab.id}
						onclick={() => {
							activeTab = tab.id as typeof activeTab;
						}}
						class="py-3 sm:py-4 px-1 border-b-2 font-medium text-xs sm:text-sm transition-colors whitespace-nowrap flex-shrink-0
							{activeTab === tab.id
							? 'border-blue-500 text-blue-600'
							: 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'}"
					>
						<span class="hidden sm:inline">{tab.label}</span>
						<span class="sm:hidden">{tab.shortLabel}</span>
						<span
							class="ml-1 sm:ml-2 py-0.5 px-1.5 sm:px-2 rounded-full text-xs
							{activeTab === tab.id
								? 'bg-blue-100 text-blue-600'
								: 'bg-gray-100 text-gray-600'}"
						>
							{tab.count}
						</span>
					</button>
				{/each}
			</nav>
		</div>

		<!-- Filter Bar - Stack on mobile -->
		<div class="flex flex-col sm:flex-row gap-2 sm:gap-4 mb-4">
			<div class="flex gap-2 flex-1 min-w-0">
				<select
					bind:value={statusFilter}
					aria-label="Filter by status"
					class="flex-1 sm:flex-none px-3 py-2 border border-gray-300 rounded-lg text-sm focus:ring-2 focus:ring-blue-500 min-w-0"
				>
					<option value="">All Statuses</option>
					<option value="ACTIVE">Active</option>
					<option value="COMPLETED">Completed</option>
					<option value="SUSPENDED">Suspended</option>
				</select>
				{#if typeFilter}
					<button
						onclick={() => {
							typeFilter = '';
						}}
						class="px-3 py-2 bg-gray-100 hover:bg-gray-200 rounded-lg text-sm flex items-center gap-2 truncate max-w-[150px] sm:max-w-none"
					>
						<span class="truncate">Clear: {typeFilter}</span>
						<span class="text-gray-500 flex-shrink-0">×</span>
					</button>
				{/if}
			</div>
			<button
				onclick={() => loadDashboard()}
				class="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 text-sm w-full sm:w-auto"
			>
				Refresh
			</button>
		</div>

		<!-- Process List -->
		<div class="bg-white rounded-lg shadow overflow-hidden">
			<table class="min-w-full divide-y divide-gray-200">
				<thead class="bg-gray-50">
					<tr>
						<th class="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase"
							>Process</th
						>
						<th class="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase"
							>Business Key</th
						>
						<th class="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase"
							>Status</th
						>
						<th class="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase"
							>Current Step</th
						>
						<th class="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase"
							>Level</th
						>
						<th class="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase"
							>Started</th
						>
						<th class="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase"
							>Duration</th
						>
						<th class="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase"
							>Actions</th
						>
					</tr>
				</thead>
				<tbody class="divide-y divide-gray-200">
					{#if displayProcesses}
						{#each displayProcesses.content as process}
							<tr class="hover:bg-gray-50">
								<td class="px-4 py-3">
									<div class="flex items-center gap-2">
										<span class="text-lg">{getProcessTypeIcon(process.processDefinitionKey)}</span>
										<div>
											<div class="font-medium text-gray-900">
												{process.processDefinitionName || process.processDefinitionKey}
											</div>
											<div class="text-xs text-gray-500">
												{process.initiatorName || process.initiatorId}
											</div>
										</div>
									</div>
								</td>
								<td class="px-4 py-3 text-sm font-mono text-gray-600">{process.businessKey}</td>
								<td class="px-4 py-3">
									<span
										class="px-2 py-1 rounded-full text-xs font-medium {getStatusColor(
											process.status || ''
										)}"
									>
										{process.status}
									</span>
									{#if process.escalationCount != null && process.escalationCount > 0}
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
									<span
										class="px-2 py-1 rounded text-xs font-medium bg-indigo-100 text-indigo-800"
									>
										{process.currentLevel}
									</span>
								</td>
								<td class="px-4 py-3 text-sm text-gray-600">{formatDate(process.startTime)}</td>
								<td class="px-4 py-3 text-sm text-gray-600"
									>{formatDuration(process.durationInMillis || null)}</td
								>
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
								<td colspan="8" class="px-4 py-0">
									<EmptyState message="No processes found matching the current filters." />
								</td>
							</tr>
						{/each}
					{/if}
				</tbody>
			</table>
			{#if displayProcesses && displayProcesses.totalPages > 1}
				<div class="p-4">
					<Pagination
						currentPage={displayProcesses.number}
						totalPages={displayProcesses.totalPages}
						onPageChange={handlePageChange}
					/>
				</div>
			{/if}
		</div>
		{/if}
	</div>
{/if}

<ProcessDetailsModal process={selectedProcess} onClose={closeProcessDetails} />

<style>
	/* Hide scrollbar for tab navigation on mobile while keeping functionality */
	.scrollbar-hide {
		-ms-overflow-style: none;
		scrollbar-width: none;
	}
	.scrollbar-hide::-webkit-scrollbar {
		display: none;
	}
</style>
