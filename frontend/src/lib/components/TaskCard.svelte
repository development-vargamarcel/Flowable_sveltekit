<script lang="ts">
	import type { Task } from '$lib/types';
	import { getProcessCardClasses, getPriorityClasses, getPriorityLabel } from '$lib/utils/theme';
	import { formatDate } from '$lib/utils/form-helpers';

	interface Props {
		task: Task;
		onclick?: () => void;
	}

	const { task, onclick }: Props = $props();

	import { getVariableDisplay } from '$lib/utils';
	const displays = $derived(getVariableDisplay(task.variables));
</script>

<button
	type="button"
	{onclick}
	class="w-full text-left card border-2 hover:shadow-lg transition-all {getProcessCardClasses(task.processDefinitionKey)}"
>
	<div class="flex justify-between items-start mb-3">
		<div class="flex-1">
			<h3 class="font-semibold text-gray-900">{task.name}</h3>
			<p class="text-sm text-gray-600">{task.processName}</p>
		</div>
		<span class="px-2 py-1 text-xs font-medium rounded border {getPriorityClasses(task.priority)}">
			{getPriorityLabel(task.priority)}
		</span>
	</div>

	{#if task.description}
		<p class="text-sm text-gray-600 mb-3 line-clamp-2">{task.description}</p>
	{/if}

	{#if displays.length > 0}
		<div class="flex flex-wrap gap-2 mb-3">
			{#each displays as { label, value }}
				<span class="px-2 py-1 text-xs bg-white rounded border border-gray-200">
					<span class="text-gray-500">{label}:</span>
					<span class="font-medium ml-1">{value}</span>
				</span>
			{/each}
		</div>
	{/if}

	<div class="flex justify-between items-center text-xs text-gray-500">
		<span>
			{#if task.assignee}
				Assigned to: {task.assignee}
			{:else}
				<span class="text-orange-600">Unassigned</span>
			{/if}
		</span>
		<span>{formatDate(task.createTime)}</span>
	</div>

	{#if task.businessKey}
		<div class="mt-2 text-xs text-gray-400">
			Ref: {task.businessKey}
		</div>
	{/if}
</button>
