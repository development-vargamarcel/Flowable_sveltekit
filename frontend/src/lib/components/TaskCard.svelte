<script lang="ts">
	import type { Task } from '$lib/types';
	import { authStore } from '$lib/stores/auth.svelte';
	import { getProcessCardClasses, getPriorityClasses, getPriorityLabel } from '$lib/utils/theme';
	import { formatDate } from '$lib/utils/form-helpers';

	interface Props {
		task: Task;
		onclick?: () => void;
		onClaim?: (taskId: string) => void;
		onUnclaim?: (taskId: string) => void;
		onDelegate?: (taskId: string) => void;
	}

	const { task, onclick, onClaim, onUnclaim, onDelegate }: Props = $props();

	import { getVariableDisplay } from '$lib/utils';
	const displays = $derived(getVariableDisplay(task.variables || null));

	function handleAction(e: Event, action?: (id: string) => void) {
		e.stopPropagation();
		if (action) action(task.id);
	}
</script>

<div
	role="button"
	tabindex="0"
	onkeydown={(e) => {
		if (onclick && (e.key === 'Enter' || e.key === ' ')) {
			e.preventDefault();
			onclick();
		}
	}}
	{onclick}
	class="w-full text-left card border-2 hover:shadow-lg transition-all dark:bg-gray-800 dark:border-gray-700 {getProcessCardClasses(
		task.processDefinitionKey || ''
	)} cursor-pointer"
>
	<div class="flex justify-between items-start mb-3">
		<div class="flex-1">
			<h3 class="font-semibold text-gray-900 dark:text-gray-100">{task.name}</h3>
			<p class="text-sm text-gray-600 dark:text-gray-400">{task.processName}</p>
		</div>
		<span class="px-2 py-1 text-xs font-medium rounded border {getPriorityClasses(task.priority)}">
			{getPriorityLabel(task.priority)}
		</span>
	</div>

	{#if task.description}
		<p class="text-sm text-gray-600 dark:text-gray-300 mb-3 line-clamp-2">{task.description}</p>
	{/if}

	{#if displays.length > 0}
		<div class="flex flex-wrap gap-2 mb-3">
			{#each displays as { label, value }}
				<span class="px-2 py-1 text-xs bg-white dark:bg-gray-700 rounded border border-gray-200 dark:border-gray-600">
					<span class="text-gray-500 dark:text-gray-400">{label}:</span>
					<span class="font-medium ml-1 dark:text-gray-200">{value}</span>
				</span>
			{/each}
		</div>
	{/if}

	<div class="flex justify-between items-center text-xs text-gray-500 dark:text-gray-400">
		<span>
			{#if task.assignee}
				Assigned to: {task.assignee}
			{:else}
				<span class="text-orange-600 dark:text-orange-400">Unassigned</span>
			{/if}
		</span>
		<span>{formatDate(task.createTime)}</span>
	</div>

	{#if task.businessKey}
		<div class="mt-2 text-xs text-gray-400">
			Ref: {task.businessKey}
		</div>
	{/if}

	<!-- Quick Actions -->
	{#if !task.assignee || (authStore.user && task.assignee === authStore.user.username)}
		<div
			class="mt-3 pt-2 border-t border-gray-200 dark:border-gray-700 flex gap-2"
			role="group"
		>
			{#if !task.assignee}
				<button
					onclick={(e) => handleAction(e, onClaim)}
					class="px-2 py-1 text-xs font-medium text-white bg-blue-600 rounded hover:bg-blue-700 transition-colors"
				>
					Claim
				</button>
			{:else if authStore.user && task.assignee === authStore.user.username}
				<button
					onclick={(e) => handleAction(e, onUnclaim)}
					class="px-2 py-1 text-xs font-medium text-gray-700 bg-gray-100 border border-gray-300 rounded hover:bg-gray-200 transition-colors dark:bg-gray-700 dark:text-gray-300 dark:border-gray-600 dark:hover:bg-gray-600"
				>
					Unclaim
				</button>
				<button
					onclick={(e) => handleAction(e, onDelegate)}
					class="px-2 py-1 text-xs font-medium text-gray-700 bg-gray-100 border border-gray-300 rounded hover:bg-gray-200 transition-colors dark:bg-gray-700 dark:text-gray-300 dark:border-gray-600 dark:hover:bg-gray-600"
				>
					Delegate
				</button>
			{/if}
		</div>
	{/if}
</div>
