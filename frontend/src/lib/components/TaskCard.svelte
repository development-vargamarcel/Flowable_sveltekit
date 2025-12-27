<script lang="ts">
	import type { Task } from '$lib/types';

	interface Props {
		task: Task;
		onclick?: () => void;
	}

	const { task, onclick }: Props = $props();

	const displays = $derived(getVariableDisplay(task.variables));

	function getPriorityColor(priority: number): string {
		if (priority >= 75) return 'bg-red-100 text-red-800 border-red-200';
		if (priority >= 50) return 'bg-yellow-100 text-yellow-800 border-yellow-200';
		return 'bg-green-100 text-green-800 border-green-200';
	}

	function getPriorityLabel(priority: number): string {
		if (priority >= 75) return 'High';
		if (priority >= 50) return 'Medium';
		return 'Low';
	}

	function getProcessColor(processKey: string): string {
		switch (processKey) {
			case 'expense-approval':
				return 'bg-emerald-50 border-emerald-200';
			case 'leave-request':
				return 'bg-sky-50 border-sky-200';
			case 'task-assignment':
				return 'bg-amber-50 border-amber-200';
			default:
				return 'bg-gray-50 border-gray-200';
		}
	}

	function formatDate(dateString: string): string {
		return new Date(dateString).toLocaleDateString('en-US', {
			month: 'short',
			day: 'numeric',
			hour: '2-digit',
			minute: '2-digit'
		});
	}

	function getVariableDisplay(variables: Record<string, unknown>): { label: string; value: string }[] {
		const displays: { label: string; value: string }[] = [];

		if (variables.amount !== undefined) {
			displays.push({ label: 'Amount', value: `$${Number(variables.amount).toFixed(2)}` });
		}
		if (variables.category) {
			displays.push({ label: 'Category', value: String(variables.category) });
		}
		if (variables.leaveType) {
			displays.push({ label: 'Type', value: String(variables.leaveType) });
		}
		if (variables.days !== undefined) {
			displays.push({ label: 'Days', value: String(variables.days) });
		}
		if (variables.title) {
			displays.push({ label: 'Title', value: String(variables.title) });
		}
		if (variables.employeeName) {
			displays.push({ label: 'From', value: String(variables.employeeName) });
		}

		return displays.slice(0, 3);
	}
</script>

<button
	type="button"
	{onclick}
	class="w-full text-left card border-2 hover:shadow-lg transition-all {getProcessColor(task.processDefinitionKey)}"
>
	<div class="flex justify-between items-start mb-3">
		<div class="flex-1">
			<h3 class="font-semibold text-gray-900">{task.name}</h3>
			<p class="text-sm text-gray-600">{task.processName}</p>
		</div>
		<span class="px-2 py-1 text-xs font-medium rounded border {getPriorityColor(task.priority)}">
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
