<script lang="ts">
	import { page } from '$app/stores';
	import { goto } from '$app/navigation';
	import { onMount } from 'svelte';
	import { api } from '$lib/api/client';
	import { authStore } from '$lib/stores/auth.svelte';
	import Toast from '$lib/components/Toast.svelte';
	import DynamicForm from '$lib/components/DynamicForm.svelte';
	import type { TaskDetails, FormDefinition } from '$lib/types';

	let taskDetails = $state<TaskDetails | null>(null);
	let formDefinition = $state<FormDefinition | null>(null);
	let loading = $state(true);
	let loadingForm = $state(false);
	let submitting = $state(false);
	let error = $state('');
	let toast = $state<{ message: string; type: 'success' | 'error' } | null>(null);

	// Form data for legacy forms
	let decision = $state('');
	let comments = $state('');

	// Form data for dynamic forms
	let dynamicFormValues = $state<Record<string, unknown>>({});
	let dynamicFormRef = $state<DynamicForm | undefined>(undefined);

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

			// Try to load form definition for this task
			loadingForm = true;
			try {
				formDefinition = await api.getTaskFormDefinition(taskId);
			} catch {
				// No custom form defined - will use legacy form
				formDefinition = null;
			} finally {
				loadingForm = false;
			}
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

		const formKey = taskDetails.task.formKey;

		// Use dynamic form if available
		if (hasDynamicForm) {
			// Validate dynamic form
			if (dynamicFormRef && !dynamicFormRef.validate()) {
				toast = { message: 'Please fix the form errors before submitting', type: 'error' };
				return;
			}

			submitting = true;
			try {
				const formValues = dynamicFormRef?.getValues() || dynamicFormValues;

				// Add decision and comments if they exist in form values
				const variables: Record<string, unknown> = { ...formValues };

				// Ensure decision and comments are included
				if (decision) {
					variables.decision = decision;
				}
				if (comments) {
					variables.comments = comments;
				}

				await api.completeTask(taskDetails.task.id, variables);
				toast = { message: 'Task completed successfully', type: 'success' };

				setTimeout(() => goto('/tasks'), 1500);
			} catch (err) {
				toast = { message: err instanceof Error ? err.message : 'Failed to complete task', type: 'error' };
			} finally {
				submitting = false;
			}
		} else {
			// Legacy form handling
			// Validate decision for approval forms
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
	}

	function handleDynamicFormChange(values: Record<string, unknown>) {
		dynamicFormValues = values;
		// Extract decision and comments if present
		if (values.decision !== undefined) {
			decision = String(values.decision);
		}
		if (values.comments !== undefined) {
			comments = String(values.comments);
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
			case 'purchase-approval-form':
				return {
					type: 'approval',
					showDecision: true,
					decisions: [
						{ value: 'approved', label: 'Approve' },
						{ value: 'rejected', label: 'Reject' },
						{ value: 'escalate', label: 'Escalate to Higher Level' },
						{ value: 'de_escalate', label: 'De-escalate to Lower Level' },
						{ value: 'request_changes', label: 'Request Changes' }
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

	function isGridData(key: string, value: unknown): boolean {
		if (typeof value !== 'string') return false;
		try {
			const parsed = JSON.parse(value);
			return Array.isArray(parsed) && parsed.length > 0 && typeof parsed[0] === 'object';
		} catch {
			return false;
		}
	}

	function parseGridData(value: string): Array<Record<string, unknown>> {
		try {
			return JSON.parse(value);
		} catch {
			return [];
		}
	}

	function formatCurrency(value: unknown): string {
		if (typeof value === 'number') {
			return `$${value.toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`;
		}
		return String(value);
	}

	// Check if there's a dynamic form with fields/grids
	const hasDynamicForm = $derived(
		formDefinition &&
		((formDefinition.fields && formDefinition.fields.length > 0) ||
		(formDefinition.grids && formDefinition.grids.length > 0))
	);

	// Get excluded keys for the request details section
	const excludedKeys = ['decision', 'comments', 'completedBy', 'completedAt', 'initiator', 'startedBy', 'startedAt'];

	// Get variables to display (excluding system variables)
	function getDisplayVariables(): Array<[string, unknown]> {
		if (!taskDetails) return [];
		return Object.entries(taskDetails.variables).filter(([key]) => !excludedKeys.includes(key));
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
		{@const displayVariables = getDisplayVariables()}

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

		<!-- Request Details (Process Variables) -->
		{#if displayVariables.length > 0}
			<div class="card mb-6">
				<h2 class="text-lg font-semibold text-gray-900 mb-4">Request Details</h2>
				<div class="grid grid-cols-2 gap-4">
					{#each displayVariables as [key, value]}
						{#if isGridData(key, value)}
							{@const items = parseGridData(value as string)}
							<!-- Grid Data Display -->
							<div class="col-span-2 py-2 border-b border-gray-100">
								<dt class="text-sm text-gray-500 mb-3">{formatLabel(key)}</dt>
								<dd>
									{#if items.length > 0}
										<div class="overflow-x-auto">
											<table class="min-w-full divide-y divide-gray-200 border border-gray-200 rounded-lg">
												<thead class="bg-gray-50">
													<tr>
														{#each Object.keys(items[0]) as column}
															<th class="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
																{formatLabel(column)}
															</th>
														{/each}
													</tr>
												</thead>
												<tbody class="bg-white divide-y divide-gray-200">
													{#each items as item}
														<tr>
															{#each Object.entries(item) as [col, val]}
																<td class="px-4 py-3 text-sm text-gray-900">
																	{#if col.toLowerCase().includes('amount') || col.toLowerCase().includes('price') || col.toLowerCase().includes('total') || col.toLowerCase().includes('cost')}
																		{formatCurrency(val)}
																	{:else}
																		{val || '-'}
																	{/if}
																</td>
															{/each}
														</tr>
													{/each}
												</tbody>
											</table>
										</div>
									{:else}
										<p class="text-gray-500 text-sm">No items</p>
									{/if}
								</dd>
							</div>
						{:else}
							<div class="py-2 border-b border-gray-100">
								<dt class="text-sm text-gray-500">{formatLabel(key)}</dt>
								<dd class="text-gray-900 font-medium">{formatValue(key, value)}</dd>
							</div>
						{/if}
					{/each}
				</div>
			</div>
		{/if}

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
					<!-- Dynamic Form (if designer-defined fields exist) -->
					{#if loadingForm}
						<div class="py-4 text-center">
							<div class="w-6 h-6 border-2 border-blue-600 border-t-transparent rounded-full animate-spin mx-auto"></div>
							<p class="mt-2 text-sm text-gray-600">Loading form...</p>
						</div>
					{:else if hasDynamicForm && formDefinition}
						<div class="mb-6">
							<DynamicForm
								bind:this={dynamicFormRef}
								fields={formDefinition.fields}
								grids={formDefinition.grids}
								gridConfig={formDefinition.gridConfig}
								values={taskDetails.variables}
								onValuesChange={handleDynamicFormChange}
							/>
						</div>
					{/if}

					<!-- Legacy Decision Form (fallback or supplement) -->
					{#if formConfig.showDecision && !hasDynamicForm}
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

					<!-- Comments (always show unless dynamic form has its own) -->
					{#if !hasDynamicForm || !formDefinition?.fields.some(f => f.name === 'comments')}
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
					{/if}

					<div class="flex justify-end space-x-3 pt-4">
						<a href="/tasks" class="btn btn-secondary">Cancel</a>
						<button
							type="submit"
							class="btn btn-success"
							disabled={submitting}
						>
							{#if submitting}
								<span class="inline-flex items-center">
									<svg class="animate-spin -ml-1 mr-2 h-4 w-4 text-white" fill="none" viewBox="0 0 24 24">
										<circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
										<path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
									</svg>
									Submitting...
								</span>
							{:else}
								Complete Task
							{/if}
						</button>
					</div>
				</form>
			{/if}
		</div>
	{/if}
</div>
