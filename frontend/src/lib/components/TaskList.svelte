<script lang="ts">
	import type { Task } from '$lib/types';
	import TaskCard from './TaskCard.svelte';

	interface Props {
		tasks: Task[];
		onTaskClick: (taskId: string) => void;
		emptyMessage?: string;
	}

	let { tasks, onTaskClick, emptyMessage = 'No tasks found' }: Props = $props();

	let sortedTasks = $derived(
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
	<div class="text-center py-12">
		<div class="w-16 h-16 mx-auto bg-gray-100 rounded-full flex items-center justify-center mb-4">
			<svg class="w-8 h-8 text-gray-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
				<path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2" />
			</svg>
		</div>
		<p class="text-gray-500">{emptyMessage}</p>
	</div>
{:else}
	<div class="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
		{#each sortedTasks as task (task.id)}
			<TaskCard {task} onclick={() => onTaskClick(task.id)} />
		{/each}
	</div>
{/if}
