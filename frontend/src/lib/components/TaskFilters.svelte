<script lang="ts">
	import { Search, X } from '@lucide/svelte';
	import { createEventDispatcher } from 'svelte';

	interface Filters {
		text: string;
		assignee: string;
		priority: string;
		sortBy: string;
	}

	let filters = $state<Filters>({
		text: '',
		assignee: '',
		priority: '',
		sortBy: 'created_desc'
	});

	const dispatch = createEventDispatcher<{
		change: Filters;
	}>();

	let timer: ReturnType<typeof setTimeout>;

	function handleChange() {
		clearTimeout(timer);
		timer = setTimeout(() => {
			dispatch('change', filters);
		}, 300);
	}

	function clearFilters() {
		filters = {
			text: '',
			assignee: '',
			priority: '',
			sortBy: 'created_desc'
		};
		dispatch('change', filters);
	}

    const hasActiveFilters = $derived(!!filters.text || !!filters.assignee || !!filters.priority || filters.sortBy !== 'created_desc');
</script>

<div class="bg-white rounded-lg shadow-sm border border-gray-200 p-4 mb-6">
	<div class="flex flex-col md:flex-row gap-4">
		<!-- Search -->
		<div class="flex-grow relative">
			<div class="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
				<Search class="h-4 w-4 text-gray-400" />
			</div>
			<input
				type="text"
				bind:value={filters.text}
				oninput={handleChange}
				placeholder="Search tasks by name or description..."
				class="pl-10 block w-full rounded-md border-gray-300 shadow-sm focus:border-blue-500 focus:ring-blue-500 sm:text-sm border p-2"
			/>
		</div>

		<!-- Filters -->
		<div class="flex flex-wrap gap-4">
			<div class="w-40">
				<select
					bind:value={filters.assignee}
					onchange={handleChange}
					class="block w-full rounded-md border-gray-300 shadow-sm focus:border-blue-500 focus:ring-blue-500 sm:text-sm border p-2"
				>
					<option value="">All Assignees</option>
					<option value="admin">Admin</option>
					<option value="user1">User 1</option>
                    <option value="unassigned">Unassigned</option>
				</select>
			</div>

			<div class="w-32">
				<select
					bind:value={filters.priority}
					onchange={handleChange}
					class="block w-full rounded-md border-gray-300 shadow-sm focus:border-blue-500 focus:ring-blue-500 sm:text-sm border p-2"
				>
					<option value="">All Priorities</option>
					<option value="25">Low</option>
					<option value="50">Medium</option>
					<option value="75">High</option>
                    <option value="100">Critical</option>
				</select>
			</div>

			<div class="w-40">
				<select
					bind:value={filters.sortBy}
					onchange={handleChange}
					class="block w-full rounded-md border-gray-300 shadow-sm focus:border-blue-500 focus:ring-blue-500 sm:text-sm border p-2"
				>
					<option value="created_desc">Newest First</option>
					<option value="created_asc">Oldest First</option>
					<option value="priority_desc">Highest Priority</option>
					<option value="priority_asc">Lowest Priority</option>
					<option value="due_asc">Due Date (Earliest)</option>
					<option value="due_desc">Due Date (Latest)</option>
				</select>
			</div>

            {#if hasActiveFilters}
                <button
                    onclick={clearFilters}
                    class="inline-flex items-center px-3 py-2 border border-gray-300 shadow-sm text-sm font-medium rounded-md text-gray-700 bg-white hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500"
                    title="Clear Filters"
                >
                    <X class="h-4 w-4 text-gray-500" />
                </button>
            {/if}
		</div>
	</div>
</div>
