<script lang="ts">
	import { onMount, onDestroy } from 'svelte';
	import { goto } from '$app/navigation';
	import { browser } from '$app/environment';
	import { api, ApiError } from '$lib/api/client';
	import { toast } from 'svelte-sonner';
	import { processStore } from '$lib/stores/processes.svelte';
	import type { WorkflowHistory, Page } from '$lib/types';
	import EscalationBadge from '$lib/components/EscalationBadge.svelte';
	import SLAStats from '$lib/components/SLAStats.svelte';
	import Pagination from '$lib/components/Pagination.svelte';
	import DurationHistogram from '$lib/components/DurationHistogram.svelte';
	import UserPerformanceWidget from '$lib/components/UserPerformanceWidget.svelte';
	import BottleneckWidget from '$lib/components/BottleneckWidget.svelte';
	import ProcessCompletionTrendWidget from '$lib/components/ProcessCompletionTrendWidget.svelte';
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
	let searchQuery = $state<string>('');
	let sortKey = $state<'started-desc' | 'started-asc' | 'duration-desc' | 'duration-asc' | 'escalations-desc'>(
		'started-desc'
	);
	let escalatedOnly = $state(false);
	let autoRefreshEnabled = $state(false);
	let autoRefreshInterval = $state(60);
	let autoRefreshPaused = $state(false);
	let nextRefreshAt = $state<Date | null>(null);
	let refreshCountdown = $state(0);
	let lastUpdatedAt = $state<Date | null>(null);
	const dashboardFiltersKey = 'dashboard.filters.v1';
	let filtersLoaded = $state(false);

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

		if (browser) {
			const params = new URLSearchParams(window.location.search);
			const hasUrlFilters = params.size > 0;
			if (hasUrlFilters) {
				statusFilter = params.get('status') ?? '';
				typeFilter = params.get('type') ?? '';
				searchQuery = params.get('search') ?? '';
				sortKey = (params.get('sort') as typeof sortKey) ?? 'started-desc';
				escalatedOnly = params.get('escalated') === 'true';
				activeTab = (params.get('tab') as typeof activeTab) ?? 'all';
			} else {
				const storedFilters = localStorage.getItem(dashboardFiltersKey);
				if (storedFilters) {
					try {
						const parsed = JSON.parse(storedFilters) as {
							statusFilter?: string;
							typeFilter?: string;
							searchQuery?: string;
							sortKey?: typeof sortKey;
							escalatedOnly?: boolean;
							autoRefreshEnabled?: boolean;
							autoRefreshInterval?: number;
							activeTab?: typeof activeTab;
						};
						statusFilter = parsed.statusFilter ?? '';
						typeFilter = parsed.typeFilter ?? '';
						searchQuery = parsed.searchQuery ?? '';
						sortKey = parsed.sortKey ?? 'started-desc';
						escalatedOnly = parsed.escalatedOnly ?? false;
						autoRefreshEnabled = parsed.autoRefreshEnabled ?? false;
						autoRefreshInterval = parsed.autoRefreshInterval ?? 60;
						activeTab = parsed.activeTab ?? 'all';
					} catch (error) {
						console.warn('Failed to parse stored dashboard filters', error);
					}
				}
			}
			filtersLoaded = true;
		}

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
			// Clear any previous errors on success
			error = null;
			lastUpdatedAt = new Date();
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

	const displayProcesses = $derived(getDisplayProcesses());
	const filteredProcesses = $derived(getFilteredProcesses());
	const filteredCount = $derived(filteredProcesses.length);
	const escalatedCount = $derived(
		filteredProcesses.filter((process) => (process.escalationCount || 0) > 0).length
	);
	const filteredSummary = $derived(getFilteredSummary());

	function handlePageChange(page: number) {
		loadDashboard(false, page);
	}

	function getFilteredProcesses(): WorkflowHistory[] {
		if (!displayProcesses) return [];

		const query = searchQuery.trim().toLowerCase();
		let items = [...displayProcesses.content];

		if (escalatedOnly) {
			items = items.filter((process) => (process.escalationCount || 0) > 0);
		}

		if (query) {
			items = items.filter((process) => {
				const haystack = [
					process.businessKey,
					process.processDefinitionName,
					process.processDefinitionKey,
					process.initiatorName,
					process.initiatorId,
					process.currentTaskName,
					process.currentAssignee
				]
					.filter(Boolean)
					.join(' ')
					.toLowerCase();
				return haystack.includes(query);
			});
		}

		items.sort((a, b) => {
			switch (sortKey) {
				case 'started-asc':
					return new Date(a.startTime).getTime() - new Date(b.startTime).getTime();
				case 'duration-desc':
					return (b.durationInMillis || 0) - (a.durationInMillis || 0);
				case 'duration-asc':
					return (a.durationInMillis || 0) - (b.durationInMillis || 0);
				case 'escalations-desc':
					return (b.escalationCount || 0) - (a.escalationCount || 0);
				default:
					return new Date(b.startTime).getTime() - new Date(a.startTime).getTime();
			}
		});

		return items;
	}

	function getFilteredSummary(): {
		averageDuration: number | null;
		oldestStart: string | null;
		newestStart: string | null;
	} {
		if (filteredProcesses.length === 0) {
			return { averageDuration: null, oldestStart: null, newestStart: null };
		}

		const durations = filteredProcesses
			.map((process) => process.durationInMillis ?? null)
			.filter((duration): duration is number => duration !== null);
		const averageDuration =
			durations.length > 0
				? Math.round(durations.reduce((sum, value) => sum + value, 0) / durations.length)
				: null;

		const startTimes = filteredProcesses.map((process) => new Date(process.startTime).getTime());
		const oldestStart = new Date(Math.min(...startTimes)).toISOString();
		const newestStart = new Date(Math.max(...startTimes)).toISOString();

		return { averageDuration, oldestStart, newestStart };
	}

	function resetFilters() {
		statusFilter = '';
		typeFilter = '';
		searchQuery = '';
		sortKey = 'started-desc';
		escalatedOnly = false;
		activeTab = 'all';
		loadDashboard();
	}

	function buildDashboardQuery(): URLSearchParams {
		const params = new URLSearchParams();
		if (statusFilter) params.set('status', statusFilter);
		if (typeFilter) params.set('type', typeFilter);
		if (searchQuery) params.set('search', searchQuery);
		if (sortKey !== 'started-desc') params.set('sort', sortKey);
		if (escalatedOnly) params.set('escalated', 'true');
		if (activeTab !== 'all') params.set('tab', activeTab);
		return params;
	}

	function syncFiltersToQuery() {
		if (!browser) return;
		const params = buildDashboardQuery();
		const query = params.toString();
		const nextUrl = query ? `${window.location.pathname}?${query}` : window.location.pathname;
		window.history.replaceState({}, '', nextUrl);
	}

	function exportCsv() {
		if (!browser || filteredProcesses.length === 0) return;

		const headers = [
			'Process',
			'Business Key',
			'Status',
			'Current Step',
			'Assignee',
			'Level',
			'Started',
			'Duration (ms)',
			'Escalations'
		];

		const rows = filteredProcesses.map((process) => [
			process.processDefinitionName || process.processDefinitionKey,
			process.businessKey || '',
			process.status || '',
			process.currentTaskName || '',
			process.currentAssignee || '',
			process.currentLevel || '',
			process.startTime || '',
			process.durationInMillis?.toString() || '',
			process.escalationCount?.toString() || '0'
		]);

		const escapeValue = (value: string) => `"${value.replace(/"/g, '""')}"`;
		const csvContent = [headers, ...rows]
			.map((row) => row.map((value) => escapeValue(String(value))).join(','))
			.join('\n');

		const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' });
		const url = URL.createObjectURL(blob);
		const link = document.createElement('a');
		link.href = url;
		link.download = `dashboard-processes-${new Date().toISOString().slice(0, 10)}.csv`;
		document.body.appendChild(link);
		link.click();
		document.body.removeChild(link);
		URL.revokeObjectURL(url);
	}

	async function copyShareLink() {
		if (!browser) return;
		syncFiltersToQuery();
		try {
			await navigator.clipboard.writeText(window.location.href);
			toast.success('Dashboard link copied to clipboard');
		} catch (error) {
			console.error('Failed to copy dashboard link', error);
			toast.error('Unable to copy link. Please copy from the address bar.');
		}
	}

	$effect(() => {
		if (!browser || !filtersLoaded) return;
		const payload = JSON.stringify({
			statusFilter,
			typeFilter,
			searchQuery,
			sortKey,
			escalatedOnly,
			autoRefreshEnabled,
			autoRefreshInterval,
			activeTab
		});
		localStorage.setItem(dashboardFiltersKey, payload);
		syncFiltersToQuery();
	});

	$effect(() => {
		if (!browser || !autoRefreshEnabled) return;
		nextRefreshAt = new Date(Date.now() + autoRefreshInterval * 1000);
		const intervalId = window.setInterval(() => {
			if (document.hidden) {
				autoRefreshPaused = true;
				return;
			}
			autoRefreshPaused = false;
			loadDashboard(true, displayProcesses?.number ?? 0, true);
			nextRefreshAt = new Date(Date.now() + autoRefreshInterval * 1000);
		}, autoRefreshInterval * 1000);
		return () => window.clearInterval(intervalId);
	});

	$effect(() => {
		if (!browser || !autoRefreshEnabled) {
			autoRefreshPaused = false;
			nextRefreshAt = null;
			refreshCountdown = 0;
			return;
		}

		const tickId = window.setInterval(() => {
			if (!nextRefreshAt) {
				refreshCountdown = 0;
				return;
			}
			const remainingSeconds = Math.max(
				0,
				Math.ceil((nextRefreshAt.getTime() - Date.now()) / 1000)
			);
			refreshCountdown = remainingSeconds;
		}, 1000);

		const handleVisibility = () => {
			if (!autoRefreshEnabled) return;
			if (document.hidden) {
				autoRefreshPaused = true;
				nextRefreshAt = null;
				refreshCountdown = 0;
				return;
			}
			autoRefreshPaused = false;
			nextRefreshAt = new Date(Date.now() + autoRefreshInterval * 1000);
		};

		document.addEventListener('visibilitychange', handleVisibility);

		return () => {
			window.clearInterval(tickId);
			document.removeEventListener('visibilitychange', handleVisibility);
		};
	});
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
				{#if lastUpdatedAt}
					<p class="text-xs text-gray-500 mt-1">
						Last updated {lastUpdatedAt.toLocaleTimeString()}
					</p>
				{/if}
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

		<!-- Row 4: Bottleneck Analysis & Trend -->
		<div class="mb-8 grid gap-6 lg:grid-cols-2">
			<BottleneckWidget />
			<ProcessCompletionTrendWidget />
		</div>

		<!-- Process Type Distribution -->
		<div class="bg-white rounded-lg shadow p-4 mb-8">
			<h3 class="font-semibold text-gray-800 mb-3">Active Processes by Type</h3>
			<div class="flex flex-wrap gap-3">
					{#each Object.entries(dashboard.activeByType) as [type, count]}
						<button
							onclick={() => {
								typeFilter = typeFilter === type ? '' : type;
								loadDashboard(true, 0);
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
			<div class="flex space-x-4 sm:space-x-8 overflow-x-auto pb-px scrollbar-hide" role="tablist" aria-label="Process filters">
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
			</div>
		</div>

		<!-- Filter Bar - Stack on mobile -->
		<div class="flex flex-col gap-3 sm:gap-4 mb-4">
			<div class="flex flex-col sm:flex-row gap-2 sm:gap-4">
				<div class="flex flex-1 gap-2 min-w-0">
					<input
						type="search"
						bind:value={searchQuery}
						placeholder="Search by key, initiator, task, or process"
						class="flex-1 px-3 py-2 border border-gray-300 rounded-lg text-sm focus:ring-2 focus:ring-blue-500 min-w-0"
					/>
						<select
							bind:value={statusFilter}
							onchange={() => loadDashboard(true, 0)}
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
									loadDashboard(true, 0);
								}}
								class="px-3 py-2 bg-gray-100 hover:bg-gray-200 rounded-lg text-sm flex items-center gap-2 truncate max-w-[150px] sm:max-w-none"
							>
							<span class="truncate">Clear: {typeFilter}</span>
							<span class="text-gray-500 flex-shrink-0">×</span>
						</button>
					{/if}
				</div>
				<div class="flex flex-col sm:flex-row flex-1 gap-2">
					<select
						bind:value={sortKey}
						aria-label="Sort processes"
						class="flex-1 sm:flex-none px-3 py-2 border border-gray-300 rounded-lg text-sm focus:ring-2 focus:ring-blue-500"
					>
						<option value="started-desc">Newest first</option>
						<option value="started-asc">Oldest first</option>
						<option value="duration-desc">Longest duration</option>
						<option value="duration-asc">Shortest duration</option>
						<option value="escalations-desc">Most escalations</option>
					</select>
					<label class="flex items-center gap-2 text-sm text-gray-600">
						<input
							type="checkbox"
							bind:checked={escalatedOnly}
							class="h-4 w-4 rounded border-gray-300 text-blue-600 focus:ring-blue-600"
						/>
						Escalated only
					</label>
				</div>
				<div class="flex flex-wrap gap-2">
					<div class="flex items-center gap-2 px-3 py-2 bg-white text-gray-700 rounded-lg border border-gray-300 text-sm">
						<label class="flex items-center gap-2">
							<input
								type="checkbox"
							bind:checked={autoRefreshEnabled}
								class="h-4 w-4 rounded border-gray-300 text-blue-600 focus:ring-blue-600"
							/>
							Auto refresh
						</label>
						<select
						aria-label="Auto refresh interval"
						value={autoRefreshInterval}
						onchange={(event) => {
							autoRefreshInterval = Number((event.target as HTMLSelectElement).value);
						}}
						disabled={!autoRefreshEnabled}
						class="px-2 py-1 border border-gray-300 rounded text-xs text-gray-600 disabled:opacity-50"
					>
						<option value={30}>30s</option>
						<option value={60}>60s</option>
							<option value={120}>2m</option>
							<option value={300}>5m</option>
						</select>
						{#if autoRefreshEnabled}
							<span class="text-xs text-gray-500">
								{#if autoRefreshPaused}
									Paused (tab inactive)
								{:else}
									Next refresh in {refreshCountdown}s
								{/if}
							</span>
						{/if}
					</div>
				<button
					onclick={() => loadDashboard()}
					class="flex-1 px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 text-sm"
				>
						Refresh
					</button>
					<button
						onclick={exportCsv}
						disabled={filteredCount === 0}
						class="flex-1 px-4 py-2 bg-white text-gray-700 rounded-lg hover:bg-gray-50 text-sm border border-gray-300 disabled:opacity-50"
					>
						Export CSV
					</button>
					<button
						onclick={copyShareLink}
						class="flex-1 px-4 py-2 bg-white text-gray-700 rounded-lg hover:bg-gray-50 text-sm border border-gray-300"
					>
						Copy link
					</button>
					{#if statusFilter || typeFilter || searchQuery || escalatedOnly || sortKey !== 'started-desc' || activeTab !== 'all'}
						<button
							onclick={resetFilters}
							class="flex-1 px-4 py-2 bg-gray-100 text-gray-700 rounded-lg hover:bg-gray-200 text-sm"
						>
							Clear all
						</button>
					{/if}
				</div>
			</div>
			<div class="flex flex-wrap items-center gap-2 text-xs text-gray-600">
				<span class="font-medium">Active filters:</span>
				{#if statusFilter}
					<span class="px-2 py-1 rounded-full bg-blue-50 text-blue-700">Status: {statusFilter}</span>
				{/if}
				{#if typeFilter}
					<span class="px-2 py-1 rounded-full bg-indigo-50 text-indigo-700">Type: {typeFilter}</span>
				{/if}
				{#if searchQuery}
					<span class="px-2 py-1 rounded-full bg-emerald-50 text-emerald-700">
						Search: "{searchQuery}"
					</span>
				{/if}
				{#if sortKey !== 'started-desc'}
					<span class="px-2 py-1 rounded-full bg-slate-100 text-slate-700">
						Sort: {sortKey.replace(/-/g, ' ')}
					</span>
				{/if}
				{#if escalatedOnly}
					<span class="px-2 py-1 rounded-full bg-rose-50 text-rose-700">Escalated only</span>
				{/if}
				{#if activeTab !== 'all'}
					<span class="px-2 py-1 rounded-full bg-sky-50 text-sky-700">
						Tab: {activeTab.replace('-', ' ')}
					</span>
				{/if}
				{#if !statusFilter && !typeFilter && !searchQuery && !escalatedOnly && sortKey === 'started-desc' && activeTab === 'all'}
					<span class="text-gray-400">None</span>
				{/if}
			</div>
		</div>

		<!-- Process List -->
		<div class="bg-white rounded-lg shadow overflow-hidden">
			<div class="px-4 py-3 text-xs text-gray-500 border-b border-gray-200">
				<div class="flex flex-wrap gap-2">
					<span>
						Showing {filteredCount} of {displayProcesses?.content.length ?? 0} processes on this page
					</span>
					<span class="text-gray-400">·</span>
					<span>{escalatedCount} escalated</span>
					{#if filteredSummary.averageDuration}
						<span class="text-gray-400">·</span>
						<span>Avg duration {formatDuration(filteredSummary.averageDuration)}</span>
					{/if}
					{#if filteredSummary.newestStart}
						<span class="text-gray-400">·</span>
						<span>Newest {formatDate(filteredSummary.newestStart)}</span>
					{/if}
					{#if filteredSummary.oldestStart}
						<span class="text-gray-400">·</span>
						<span>Oldest {formatDate(filteredSummary.oldestStart)}</span>
					{/if}
				</div>
			</div>
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
						{#each filteredProcesses as process}
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
									{#if process.escalationCount && process.escalationCount > 0}
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
