<script lang="ts">
	/**
	 * Dashboard Skeleton - Shows content-aware placeholder while dashboard loads
	 * This provides a better UX than a simple spinner by showing the expected layout
	 */
	import Skeleton from './Skeleton.svelte';
</script>

<div class="max-w-7xl mx-auto px-4 py-8">
	<!-- Header -->
	<div class="mb-8">
		<div class="h-8 w-64 bg-gray-200 rounded animate-pulse mb-2"></div>
		<div class="h-4 w-96 bg-gray-200 rounded animate-pulse"></div>
	</div>

	<!-- SLA Stats Skeleton -->
	<div class="bg-white rounded-lg shadow p-4 mb-8">
		<div class="flex items-center justify-between mb-4">
			<div class="h-5 w-32 bg-gray-200 rounded animate-pulse"></div>
			<div class="h-8 w-24 bg-gray-200 rounded animate-pulse"></div>
		</div>
		<div class="grid grid-cols-2 md:grid-cols-4 gap-4">
			{#each Array(4) as _}
				<div class="text-center p-3">
					<div class="h-8 w-12 mx-auto bg-gray-200 rounded animate-pulse mb-2"></div>
					<div class="h-3 w-20 mx-auto bg-gray-200 rounded animate-pulse"></div>
				</div>
			{/each}
		</div>
	</div>

	<!-- Stats Overview Skeleton -->
	<div class="grid grid-cols-2 md:grid-cols-4 lg:grid-cols-7 gap-4 mb-8">
		{#each Array(7) as _}
			<Skeleton variant="stat-card" />
		{/each}
	</div>

	<!-- Analytics Widgets Skeleton -->
	<div class="mb-8 grid gap-6 lg:grid-cols-2">
		<div class="bg-white rounded-lg shadow p-4">
			<div class="h-5 w-40 bg-gray-200 rounded animate-pulse mb-4"></div>
			<div class="h-48 bg-gray-200 rounded animate-pulse"></div>
		</div>
		<div class="bg-white rounded-lg shadow p-4">
			<div class="h-5 w-40 bg-gray-200 rounded animate-pulse mb-4"></div>
			<div class="h-48 bg-gray-200 rounded animate-pulse"></div>
		</div>
	</div>

	<!-- Bottleneck Widget Skeleton -->
	<div class="bg-white rounded-lg shadow p-4 mb-8">
		<div class="h-5 w-44 bg-gray-200 rounded animate-pulse mb-4"></div>
		<div class="space-y-3">
			{#each Array(3) as _}
				<div class="flex items-center justify-between p-3 bg-gray-50 rounded-lg">
					<div class="flex items-center gap-3">
						<div class="h-8 w-8 bg-gray-200 rounded-full animate-pulse"></div>
						<div>
							<div class="h-4 w-32 bg-gray-200 rounded animate-pulse mb-1"></div>
							<div class="h-3 w-20 bg-gray-200 rounded animate-pulse"></div>
						</div>
					</div>
					<div class="h-6 w-16 bg-gray-200 rounded animate-pulse"></div>
				</div>
			{/each}
		</div>
	</div>

	<!-- Process Type Distribution Skeleton -->
	<div class="bg-white rounded-lg shadow p-4 mb-8">
		<div class="h-5 w-48 bg-gray-200 rounded animate-pulse mb-3"></div>
		<div class="flex flex-wrap gap-3">
			{#each Array(5) as _}
				<div class="h-10 w-32 bg-gray-200 rounded-lg animate-pulse"></div>
			{/each}
		</div>
	</div>

	<!-- Tab Navigation Skeleton -->
	<div class="border-b border-gray-200 mb-6">
		<nav class="flex space-x-8">
			{#each Array(4) as _}
				<div class="py-4 px-1">
					<div class="h-4 w-24 bg-gray-200 rounded animate-pulse"></div>
				</div>
			{/each}
		</nav>
	</div>

	<!-- Filter Bar Skeleton -->
	<div class="flex gap-4 mb-4">
		<div class="h-10 w-40 bg-gray-200 rounded-lg animate-pulse"></div>
		<div class="ml-auto h-10 w-24 bg-gray-200 rounded-lg animate-pulse"></div>
	</div>

	<!-- Process List Skeleton -->
	<div class="bg-white rounded-lg shadow overflow-hidden">
		<table class="min-w-full divide-y divide-gray-200">
			<thead class="bg-gray-50">
				<tr>
					<th class="px-4 py-3"><div class="h-3 w-16 bg-gray-300 rounded animate-pulse"></div></th>
					<th class="px-4 py-3"><div class="h-3 w-24 bg-gray-300 rounded animate-pulse"></div></th>
					<th class="px-4 py-3"><div class="h-3 w-12 bg-gray-300 rounded animate-pulse"></div></th>
					<th class="px-4 py-3"><div class="h-3 w-20 bg-gray-300 rounded animate-pulse"></div></th>
					<th class="px-4 py-3"><div class="h-3 w-10 bg-gray-300 rounded animate-pulse"></div></th>
					<th class="px-4 py-3"><div class="h-3 w-16 bg-gray-300 rounded animate-pulse"></div></th>
					<th class="px-4 py-3"><div class="h-3 w-16 bg-gray-300 rounded animate-pulse"></div></th>
					<th class="px-4 py-3"><div class="h-3 w-16 bg-gray-300 rounded animate-pulse"></div></th>
				</tr>
			</thead>
			<tbody class="divide-y divide-gray-200">
				{#each Array(5) as _}
					<Skeleton variant="table-row" />
				{/each}
			</tbody>
		</table>
	</div>
</div>
