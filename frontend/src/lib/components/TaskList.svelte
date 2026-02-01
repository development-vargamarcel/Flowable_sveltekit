<script lang="ts">
	import type { Task } from '$lib/types';
	import TaskCard from './TaskCard.svelte';
	import EmptyState from './EmptyState.svelte';

	interface Props {
		tasks: Task[];
		onTaskClick: (taskId: string) => void;
		onClaim?: (taskId: string) => void;
		onUnclaim?: (taskId: string) => void;
		onDelegate?: (taskId: string) => void;
		emptyMessage?: string;
	}

	const {
		tasks,
		onTaskClick,
		onClaim,
		onUnclaim,
		onDelegate,
		emptyMessage = 'No tasks found'
	}: Props = $props();

	const sortedTasks = $derived(
		[...tasks].sort((a, b) => {
			// Sort by priority (high first), then by creation time (newest first)
			if (b.priority !== a.priority) {
				return b.priority - a.priority;
			}
			return new Date(b.createTime).getTime() - new Date(a.createTime).getTime();
		})
	);
</script>

{#if sortedTasks.length === 0}
	<EmptyState message={emptyMessage} />
{:else}
	<div class="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
		{#each sortedTasks as task (task.id)}
			<TaskCard
				{task}
				onclick={() => onTaskClick(task.id)}
				{onClaim}
				{onUnclaim}
				{onDelegate}
			/>
		{/each}
	</div>
{/if}
