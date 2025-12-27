<script lang="ts">
	import { page } from '$app/stores';
	import { goto } from '$app/navigation';
	import { onMount } from 'svelte';
	import { api } from '$lib/api/client';
	import { authStore } from '$lib/stores/auth.svelte';
	import Toast from '$lib/components/Toast.svelte';
	import type { TaskDetails } from '$lib/types';

	let taskDetails = $state<TaskDetails | null>(null);
	let loading = $state(true);
	let submitting = $state(false);
	let error = $state('');
	let toast = $state<{ message: string; type: 'success' | 'error' } | null>(null);

	// Form data
	let decision = $state('');
	let comments = $state('');

	onMount(async () => {
		await loadTask();
	});

	async function loadTask() {
		loading = true;
		error = '';
		try {
			const taskId = $page.params.id;
			if (!taskId) {
				error = 'Task ID is required';
				return;
			}
			taskDetails = await api.getTaskDetails(taskId);
		} catch (err) {
			error = err instanceof Error ? err.message : 'Failed to load task';
		} finally {
			loading = false;
		}
	}

	async function handleClaim() {
		if (!taskDetails) return;
		submitting = true;
		try {
			await api.claimTask(taskDetails.task.id);
			toast = { message: 'Task claimed successfully', type: 'success' };
			await loadTask();
		} catch (err) {
			toast = { message: err instanceof Error ? err.message : 'Failed to claim task', type: 'error' };
		} finally {
			submitting = false;
		}
	}

	async function handleComplete() {
		if (!taskDetails) return;

		// Validate decision for approval forms
		const formKey = taskDetails.task.formKey;
		if ((formKey?.includes('approval') || formKey?.includes('review')) && !decision) {
			toast = { message: 'Please select a decision', type: 'error' };
			return;
		}

		submitting = true;
		try {
			const variables: Record<string, unknown> = {};

			if (decision) {
				variables.decision = decision;
			}
			if (comments) {
				variables.comments = comments;
			}

			// For task assignment, update assignee
			if (formKey === 'claim-task-form') {
				variables.assignee = authStore.user?.username;
			}

			await api.completeTask(taskDetails.task.id, variables);
			toast = { message: 'Task completed successfully', type: 'success' };

			setTimeout(() => goto('/tasks'), 1500);
		} catch (err) {
			toast = { message: err instanceof Error ? err.message : 'Failed to complete task', type: 'error' };
		} finally {
			submitting = false;
		}
	}

	function getFormFields(formKey: string | null): { type: string; showDecision: boolean; decisions: { value: string; label: string }[] } {
		switch (formKey) {
			case 'expense-approval-form':
				return {
					type: 'approval',
					showDecision: true,
					decisions: [
						{ value: 'approved', label: 'Approve' },
						{ value: 'rejected', label: 'Reject' }
					]
				};
			case 'expense-review-form':
				return {
					type: 'review',
					showDecision: true,
					decisions: [
						{ value: 'recommend', label: 'Recommend for Approval' },
						{ value: 'rejected', label: 'Reject' }
					]
				};
			case 'leave-approval-form':
				return {
					type: 'approval',
					showDecision: true,
					decisions: [
						{ value: 'approved', label: 'Approve' },
						{ value: 'rejected', label: 'Reject' },
						{ value: 'escalate', label: 'Escalate to Executive' }
					]
				};
			case 'claim-task-form':
			case 'complete-task-form':
				return {
					type: 'task',
					showDecision: false,
					decisions: []
				};
			default:
				return {
					type: 'generic',
					showDecision: false,
					decisions: []
				};
		}
	}

	function formatValue(key: string, value: unknown): string {
		if (value === null || value === undefined) return '-';
		if (typeof value === 'number' && (key.includes('amount') || key.includes('Amount'))) {
			return `$${value.toFixed(2)}`;
		}
		if (typeof value === 'boolean') return value ? 'Yes' : 'No';
		return String(value);
	}

	function formatLabel(key: string): string {
		return key
			.replace(/([A-Z])/g, ' $1')
			.replace(/^./, str => str.toUpperCase())
			.trim();
	}
</script>

<svelte:head>
	<title>{taskDetails?.task.name || 'Task'} - BPM Demo</title>
</svelte:head>

{#if toast}
	<Toast message={toast.message} type={toast.type} onClose={() => toast = null} />
{/if}

<div class="max-w-4xl mx-auto px-4 py-8">
	<a href="/tasks" class="inline-flex items-center text-sm text-gray-600 hover:text-gray-900 mb-6">
		<svg class="w-4 h-4 mr-1" fill="none" viewBox="0 0 24 24" stroke="currentColor">
			<path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 19l-7-7 7-7" />
		</svg>
		Back to Tasks
	</a>

	{#if loading}
		<div class="text-center py-12">
			<div class="w-12 h-12 border-4 border-blue-600 border-t-transparent rounded-full animate-spin mx-auto"></div>
			<p class="mt-4 text-gray-600">Loading task...</p>
		</div>
	{:else if error}
		<div class="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded-lg">
			{error}
		</div>
	{:else if taskDetails}
		{@const task = taskDetails.task}
		{@const formConfig = getFormFields(task.formKey)}

		<!-- Task Header -->
		<div class="card mb-6">
			<div class="flex items-start justify-between">
				<div>
					<h1 class="text-2xl font-bold text-gray-900">{task.name}</h1>
					<p class="text-gray-600 mt-1">{task.processName}</p>
					{#if task.businessKey}
						<p class="text-sm text-gray-500 mt-1">Reference: {task.businessKey}</p>
					{/if}
				</div>
				{#if task.assignee}
					<span class="px-3 py-1 bg-green-100 text-green-800 text-sm font-medium rounded-full">
						Assigned to: {task.assignee}
					</span>
				{:else}
					<span class="px-3 py-1 bg-orange-100 text-orange-800 text-sm font-medium rounded-full">
						Unassigned
					</span>
				{/if}
			</div>

			{#if task.description}
				<p class="text-gray-700 mt-4">{task.description}</p>
			{/if}
		</div>

		<!-- Process Variables -->
		<div class="card mb-6">
			<h2 class="text-lg font-semibold text-gray-900 mb-4">Request Details</h2>
			<div class="grid grid-cols-2 gap-4">
				{#each Object.entries(taskDetails.variables) as [key, value]}
					{#if !['decision', 'comments', 'completedBy', 'completedAt', 'initiator', 'startedBy', 'startedAt'].includes(key)}
						<div class="py-2 border-b border-gray-100">
							<dt class="text-sm text-gray-500">{formatLabel(key)}</dt>
							<dd class="text-gray-900 font-medium">{formatValue(key, value)}</dd>
						</div>
					{/if}
				{/each}
			</div>
		</div>

		<!-- Action Form -->
		<div class="card">
			<h2 class="text-lg font-semibold text-gray-900 mb-4">Take Action</h2>

			{#if !task.assignee && task.formKey !== 'complete-task-form'}
				<div class="mb-4 p-4 bg-yellow-50 border border-yellow-200 rounded-lg">
					<p class="text-yellow-800">This task is not assigned. Claim it to work on it.</p>
					<button
						onclick={handleClaim}
						class="mt-2 btn btn-primary"
						disabled={submitting}
					>
						{submitting ? 'Claiming...' : 'Claim Task'}
					</button>
				</div>
			{:else}
				<form onsubmit={(e) => { e.preventDefault(); handleComplete(); }} class="space-y-4">
					{#if formConfig.showDecision}
						<fieldset>
							<legend class="label">Your Decision *</legend>
							<div class="space-y-2">
								{#each formConfig.decisions as opt}
									<label class="flex items-center space-x-3 p-3 border rounded-lg cursor-pointer hover:bg-gray-50 {decision === opt.value ? 'border-blue-500 bg-blue-50' : 'border-gray-200'}">
										<input
											type="radio"
											name="decision"
											value={opt.value}
											bind:group={decision}
											class="text-blue-600"
										/>
										<span class="font-medium">{opt.label}</span>
									</label>
								{/each}
							</div>
						</fieldset>
					{/if}

					<div>
						<label for="comments" class="label">Comments</label>
						<textarea
							id="comments"
							bind:value={comments}
							rows="3"
							class="input"
							placeholder="Add any comments..."
						></textarea>
					</div>

					<div class="flex justify-end space-x-3 pt-4">
						<a href="/tasks" class="btn btn-secondary">Cancel</a>
						<button
							type="submit"
							class="btn btn-success"
							disabled={submitting}
						>
							{submitting ? 'Submitting...' : 'Complete Task'}
						</button>
					</div>
				</form>
			{/if}
		</div>
	{/if}
</div>
