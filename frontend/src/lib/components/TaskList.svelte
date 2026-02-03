<script lang="ts">
	import type { Task } from '$lib/types';
	import TaskCard from './TaskCard.svelte';
	import EmptyState from './EmptyState.svelte';
	import { CheckSquare, Square, UserCheck, UserX } from '@lucide/svelte';

	interface Props {
		tasks: Task[];
		sortBy?: string;
		onTaskClick: (taskId: string) => void;
		onClaim?: (taskId: string) => void;
		onUnclaim?: (taskId: string) => void;
		onDelegate?: (taskId: string) => void;
		onBulkClaim?: (taskIds: string[]) => void;
		onBulkUnclaim?: (taskIds: string[]) => void;
		emptyMessage?: string;
	}

	const {
		tasks,
		sortBy = 'created_desc',
		onTaskClick,
		onClaim,
		onUnclaim,
		onDelegate,
		onBulkClaim,
		onBulkUnclaim,
		emptyMessage = 'No tasks found'
	}: Props = $props();

	let selectedIds = $state<Set<string>>(new Set());

	const sortedTasks = $derived(
		[...tasks].sort((a, b) => {
			switch (sortBy) {
				case 'created_asc':
					return new Date(a.createTime).getTime() - new Date(b.createTime).getTime();
				case 'created_desc':
					return new Date(b.createTime).getTime() - new Date(a.createTime).getTime();
				case 'priority_desc':
					return b.priority - a.priority;
				case 'priority_asc':
					return a.priority - b.priority;
				case 'due_asc':
					return (new Date(a.dueDate || '9999-12-31').getTime()) - (new Date(b.dueDate || '9999-12-31').getTime());
				case 'due_desc':
					return (new Date(b.dueDate || '1970-01-01').getTime()) - (new Date(a.dueDate || '1970-01-01').getTime());
				default:
					return new Date(b.createTime).getTime() - new Date(a.createTime).getTime();
			}
		})
	);

	function handleSelect(taskId: string, selected: boolean) {
		const newSet = new Set(selectedIds);
		if (selected) {
			newSet.add(taskId);
		} else {
			newSet.delete(taskId);
		}
		selectedIds = newSet;
	}

	function toggleSelectAll() {
		if (selectedIds.size === sortedTasks.length) {
			selectedIds = new Set();
		} else {
			selectedIds = new Set(sortedTasks.map((t) => t.id));
		}
	}

	function handleBulkClaim() {
		if (onBulkClaim) onBulkClaim(Array.from(selectedIds));
		selectedIds = new Set();
	}

	function handleBulkUnclaim() {
		if (onBulkUnclaim) onBulkUnclaim(Array.from(selectedIds));
		selectedIds = new Set();
	}
</script>

{#if sortedTasks.length === 0}
	<EmptyState message={emptyMessage} />
{:else}
	<!-- Bulk Actions Toolbar -->
	{#if onBulkClaim || onBulkUnclaim}
		<div class="flex flex-wrap items-center justify-between gap-4 mb-4 bg-white p-3 rounded-lg border border-gray-200 shadow-sm">
			<div class="flex items-center gap-2">
				<button
					onclick={toggleSelectAll}
					class="flex items-center gap-2 px-3 py-1.5 text-sm font-medium text-gray-700 hover:bg-gray-100 rounded-md transition-colors"
				>
					{#if selectedIds.size === sortedTasks.length && sortedTasks.length > 0}
						<CheckSquare class="w-4 h-4 text-blue-600" />
						Deselect All
					{:else}
						<Square class="w-4 h-4 text-gray-400" />
						Select All
					{/if}
				</button>
				<span class="text-sm text-gray-500 border-l pl-3 ml-1">
					{selectedIds.size} selected
				</span>
			</div>

			{#if selectedIds.size > 0}
				<div class="flex items-center gap-2">
					{#if onBulkClaim}
						<button
							onclick={handleBulkClaim}
							class="flex items-center gap-2 px-3 py-1.5 text-sm font-medium text-white bg-blue-600 rounded-md hover:bg-blue-700 transition-colors"
						>
							<UserCheck class="w-4 h-4" />
							Claim Selected
						</button>
					{/if}
					{#if onBulkUnclaim}
						<button
							onclick={handleBulkUnclaim}
							class="flex items-center gap-2 px-3 py-1.5 text-sm font-medium text-gray-700 bg-white border border-gray-300 rounded-md hover:bg-gray-50 transition-colors"
						>
							<UserX class="w-4 h-4" />
							Unclaim Selected
						</button>
					{/if}
				</div>
			{/if}
		</div>
	{/if}

	<div class="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
		{#each sortedTasks as task (task.id)}
			<TaskCard
				{task}
				selected={selectedIds.has(task.id)}
				onclick={() => onTaskClick(task.id)}
				{onClaim}
				{onUnclaim}
				{onDelegate}
				onSelect={handleSelect}
			/>
		{/each}
	</div>
{/if}
