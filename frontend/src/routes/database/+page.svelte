<script lang="ts">
	import { onMount } from 'svelte';
	import { api } from '$lib/api/client';
	import type { TableDataResponse } from '$lib/types';

	let loading = $state(true);
	let error = $state('');
	let tables = $state<string[]>([]);
	let selectedTable = $state('');
	let tableData = $state<TableDataResponse | null>(null);
	let pageSize = $state(20);
	let loadingData = $state(false);

	onMount(async () => {
		await loadTables();
	});

	async function loadTables() {
		loading = true;
		error = '';
		try {
			tables = await api.getDatabaseTables();
		} catch (err) {
			error = err instanceof Error ? err.message : 'Failed to load tables';
		} finally {
			loading = false;
		}
	}

	async function loadTableData(page: number = 0) {
		if (!selectedTable) return;

		loadingData = true;
		error = '';
		try {
			tableData = await api.getTableData(selectedTable, page, pageSize);
		} catch (err) {
			error = err instanceof Error ? err.message : 'Failed to load table data';
			tableData = null;
		} finally {
			loadingData = false;
		}
	}

	async function handleTableSelect(tableName: string) {
		selectedTable = tableName;
		await loadTableData(0);
	}

	async function goToPage(page: number) {
		if (page < 0 || (tableData && page >= tableData.totalPages)) return;
		await loadTableData(page);
	}

	async function handlePageSizeChange() {
		await loadTableData(0);
	}

	function formatCellValue(value: unknown): string {
		if (value === null || value === undefined) {
			return 'NULL';
		}
		if (typeof value === 'object') {
			return JSON.stringify(value);
		}
		const str = String(value);
		// Truncate long values for display
		if (str.length > 100) {
			return str.substring(0, 100) + '...';
		}
		return str;
	}

	function getCellClass(value: unknown): string {
		if (value === null || value === undefined) {
			return 'text-gray-400 italic';
		}
		if (typeof value === 'number') {
			return 'text-blue-700 font-mono';
		}
		if (typeof value === 'boolean') {
			return value ? 'text-green-700' : 'text-red-700';
		}
		return 'text-gray-900';
	}

	// Generate pagination range
	function getPaginationRange(): (number | string)[] {
		if (!tableData || tableData.totalPages <= 1) return [];

		const current = tableData.page;
		const total = tableData.totalPages;
		const range: (number | string)[] = [];

		// Always show first page
		range.push(0);

		// Show ellipsis if current is far from start
		if (current > 3) {
			range.push('...');
		}

		// Show pages around current
		for (let i = Math.max(1, current - 1); i <= Math.min(total - 2, current + 1); i++) {
			if (!range.includes(i)) {
				range.push(i);
			}
		}

		// Show ellipsis if current is far from end
		if (current < total - 4) {
			range.push('...');
		}

		// Always show last page if more than 1 page
		if (total > 1 && !range.includes(total - 1)) {
			range.push(total - 1);
		}

		return range;
	}

	let paginationRange = $derived(getPaginationRange());
</script>

<svelte:head>
	<title>Database Tables - BPM Demo</title>
</svelte:head>

<div class="max-w-7xl mx-auto px-4 py-8">
	<div class="mb-8">
		<h1 class="text-2xl font-bold text-gray-900">Database Table Viewer</h1>
		<p class="text-gray-600 mt-1">Browse and inspect database tables with pagination</p>
	</div>

	{#if loading}
		<div class="text-center py-12">
			<div class="w-12 h-12 border-4 border-blue-600 border-t-transparent rounded-full animate-spin mx-auto"></div>
			<p class="mt-4 text-gray-600">Loading tables...</p>
		</div>
	{:else if error && !tableData}
		<div class="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded-lg">
			{error}
			<button onclick={() => loadTables()} class="ml-4 underline">Retry</button>
		</div>
	{:else}
		<div class="grid grid-cols-1 lg:grid-cols-4 gap-6">
			<!-- Table List Sidebar -->
			<div class="lg:col-span-1">
				<div class="bg-white rounded-lg shadow p-4">
					<h2 class="font-semibold text-gray-800 mb-3 flex items-center gap-2">
						<svg class="w-5 h-5 text-gray-500" fill="none" viewBox="0 0 24 24" stroke="currentColor">
							<path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 7v10c0 2.21 3.582 4 8 4s8-1.79 8-4V7M4 7c0 2.21 3.582 4 8 4s8-1.79 8-4M4 7c0-2.21 3.582-4 8-4s8 1.79 8 4" />
						</svg>
						Tables ({tables.length})
					</h2>
					<div class="max-h-[60vh] overflow-y-auto space-y-1">
						{#each tables as table}
							<button
								onclick={() => handleTableSelect(table)}
								class="w-full text-left px-3 py-2 rounded-lg text-sm transition-colors
									{selectedTable === table
										? 'bg-blue-600 text-white'
										: 'hover:bg-gray-100 text-gray-700'}"
							>
								{table}
							</button>
						{/each}
					</div>
				</div>
			</div>

			<!-- Table Data View -->
			<div class="lg:col-span-3">
				{#if !selectedTable}
					<div class="bg-white rounded-lg shadow p-8 text-center">
						<svg class="w-16 h-16 text-gray-300 mx-auto mb-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
							<path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 17V7m0 10a2 2 0 01-2 2H5a2 2 0 01-2-2V7a2 2 0 012-2h2a2 2 0 012 2m0 10a2 2 0 002 2h2a2 2 0 002-2M9 7a2 2 0 012-2h2a2 2 0 012 2m0 10V7m0 10a2 2 0 002 2h2a2 2 0 002-2V7a2 2 0 00-2-2h-2a2 2 0 00-2 2" />
						</svg>
						<p class="text-gray-500">Select a table from the list to view its data</p>
					</div>
				{:else if loadingData}
					<div class="bg-white rounded-lg shadow p-8 text-center">
						<div class="w-10 h-10 border-4 border-blue-600 border-t-transparent rounded-full animate-spin mx-auto"></div>
						<p class="mt-4 text-gray-600">Loading table data...</p>
					</div>
				{:else if error}
					<div class="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded-lg">
						{error}
						<button onclick={() => loadTableData(tableData?.page ?? 0)} class="ml-4 underline">Retry</button>
					</div>
				{:else if tableData}
					<!-- Table Header -->
					<div class="bg-white rounded-lg shadow mb-4 p-4">
						<div class="flex flex-wrap items-center justify-between gap-4">
							<div>
								<h2 class="font-semibold text-gray-800 text-lg">{tableData.tableName}</h2>
								<p class="text-sm text-gray-500">
									{tableData.totalRows.toLocaleString()} total rows
								</p>
							</div>
							<div class="flex items-center gap-4">
								<label class="flex items-center gap-2 text-sm text-gray-600">
									Rows per page:
									<select
										bind:value={pageSize}
										onchange={() => handlePageSizeChange()}
										class="px-2 py-1 border border-gray-300 rounded text-sm focus:ring-2 focus:ring-blue-500"
									>
										<option value={10}>10</option>
										<option value={20}>20</option>
										<option value={50}>50</option>
										<option value={100}>100</option>
									</select>
								</label>
								<button
									onclick={() => loadTableData(tableData?.page ?? 0)}
									class="px-3 py-1.5 bg-blue-600 text-white rounded hover:bg-blue-700 text-sm flex items-center gap-1"
								>
									<svg class="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
										<path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15" />
									</svg>
									Refresh
								</button>
							</div>
						</div>
					</div>

					<!-- Data Table -->
					<div class="bg-white rounded-lg shadow overflow-hidden">
						<div class="overflow-x-auto">
							<table class="min-w-full divide-y divide-gray-200">
								<thead class="bg-gray-50">
									<tr>
										{#each tableData.columns as column}
											<th class="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider whitespace-nowrap">
												{column}
											</th>
										{/each}
									</tr>
								</thead>
								<tbody class="divide-y divide-gray-200">
									{#each tableData.rows as row, index}
										<tr class="hover:bg-gray-50 {index % 2 === 0 ? 'bg-white' : 'bg-gray-50/50'}">
											{#each tableData.columns as column}
												<td class="px-4 py-2 text-sm whitespace-nowrap max-w-xs overflow-hidden text-ellipsis {getCellClass(row[column])}">
													{formatCellValue(row[column])}
												</td>
											{/each}
										</tr>
									{:else}
										<tr>
											<td colspan={tableData.columns.length} class="px-4 py-8 text-center text-gray-500">
												No data found in this table.
											</td>
										</tr>
									{/each}
								</tbody>
							</table>
						</div>

						<!-- Pagination -->
						{#if tableData.totalPages > 1}
							<div class="bg-gray-50 px-4 py-3 border-t border-gray-200">
								<div class="flex flex-wrap items-center justify-between gap-4">
									<div class="text-sm text-gray-600">
										Showing {(tableData.page * tableData.size) + 1} - {Math.min((tableData.page + 1) * tableData.size, tableData.totalRows)} of {tableData.totalRows.toLocaleString()}
									</div>
									<div class="flex items-center gap-1">
										<!-- First Page -->
										<button
											onclick={() => goToPage(0)}
											disabled={tableData.page === 0}
											class="px-2 py-1 rounded text-sm transition-colors
												{tableData.page === 0
													? 'text-gray-300 cursor-not-allowed'
													: 'text-gray-600 hover:bg-gray-200'}"
											aria-label="First page"
										>
											<svg class="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
												<path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M11 19l-7-7 7-7m8 14l-7-7 7-7" />
											</svg>
										</button>

										<!-- Previous Page -->
										<button
											onclick={() => goToPage(tableData!.page - 1)}
											disabled={tableData.page === 0}
											class="px-2 py-1 rounded text-sm transition-colors
												{tableData.page === 0
													? 'text-gray-300 cursor-not-allowed'
													: 'text-gray-600 hover:bg-gray-200'}"
											aria-label="Previous page"
										>
											<svg class="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
												<path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 19l-7-7 7-7" />
											</svg>
										</button>

										<!-- Page Numbers -->
										{#each paginationRange as item}
											{#if item === '...'}
												<span class="px-2 py-1 text-gray-400">...</span>
											{:else}
												<button
													onclick={() => goToPage(item as number)}
													class="px-3 py-1 rounded text-sm transition-colors
														{tableData.page === item
															? 'bg-blue-600 text-white'
															: 'text-gray-600 hover:bg-gray-200'}"
												>
													{(item as number) + 1}
												</button>
											{/if}
										{/each}

										<!-- Next Page -->
										<button
											onclick={() => goToPage(tableData!.page + 1)}
											disabled={tableData.page >= tableData.totalPages - 1}
											class="px-2 py-1 rounded text-sm transition-colors
												{tableData.page >= tableData.totalPages - 1
													? 'text-gray-300 cursor-not-allowed'
													: 'text-gray-600 hover:bg-gray-200'}"
											aria-label="Next page"
										>
											<svg class="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
												<path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7" />
											</svg>
										</button>

										<!-- Last Page -->
										<button
											onclick={() => goToPage(tableData!.totalPages - 1)}
											disabled={tableData.page >= tableData.totalPages - 1}
											class="px-2 py-1 rounded text-sm transition-colors
												{tableData.page >= tableData.totalPages - 1
													? 'text-gray-300 cursor-not-allowed'
													: 'text-gray-600 hover:bg-gray-200'}"
											aria-label="Last page"
										>
											<svg class="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
												<path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 5l7 7-7 7M5 5l7 7-7 7" />
											</svg>
										</button>
									</div>
								</div>
							</div>
						{/if}
					</div>
				{/if}
			</div>
		</div>
	{/if}
</div>
