<script lang="ts">
	import { page } from '$app/stores';
	import { goto } from '$app/navigation';
	import { onMount } from 'svelte';
	import { api } from '$lib/api/client';
	import Toast from '$lib/components/Toast.svelte';
	import DynamicForm from '$lib/components/DynamicForm.svelte';
	import type { ProcessDefinition, FormDefinition } from '$lib/types';

	let processDefinition = $state<ProcessDefinition | null>(null);
	let formDefinition = $state<FormDefinition | null>(null);
	let loading = $state(true);
	let loadingForm = $state(false);
	let submitting = $state(false);
	let error = $state('');
	let toast = $state<{ message: string; type: 'success' | 'error' } | null>(null);
	let businessKey = $state('');

	// Form data
	let formValues = $state<Record<string, unknown>>({});
	let dynamicFormRef = $state<DynamicForm | undefined>(undefined);

	onMount(async () => {
		await loadProcessDefinition();
	});

	async function loadProcessDefinition() {
		loading = true;
		error = '';

		try {
			const processId = $page.params.id;
			if (!processId) {
				error = 'Process ID is required';
				return;
			}

			// Load all process definitions to find the one we need
			const processes = await api.getProcesses();
			processDefinition = processes.find(p => p.id === processId) || null;

			if (!processDefinition) {
				error = 'Process definition not found';
				return;
			}

			// Try to load start form definition
			loadingForm = true;
			try {
				formDefinition = await api.getStartFormDefinition(processId);
			} catch {
				// No start form defined - will show default message
				formDefinition = null;
			} finally {
				loadingForm = false;
			}
		} catch (err) {
			error = err instanceof Error ? err.message : 'Failed to load process definition';
		} finally {
			loading = false;
		}
	}

	async function handleSubmit() {
		if (!processDefinition) return;

		// Validate dynamic form if present
		if (hasDynamicForm && dynamicFormRef) {
			if (!dynamicFormRef.validate()) {
				toast = { message: 'Please fix the form errors before submitting', type: 'error' };
				return;
			}
		}

		submitting = true;
		try {
			const variables = hasDynamicForm && dynamicFormRef
				? dynamicFormRef.getValues()
				: formValues;

			const trimmedBusinessKey = businessKey.trim();
			const result = await api.startProcess(
				processDefinition.key,
				variables,
				trimmedBusinessKey.length > 0 ? trimmedBusinessKey : undefined
			);

			const reference = result.processInstance.businessKey || result.processInstance.id;
			toast = { message: `Process started successfully! Reference: ${reference}`, type: 'success' };

			// Redirect to tasks after short delay
			setTimeout(() => goto('/tasks'), 2000);
		} catch (err) {
			toast = { message: err instanceof Error ? err.message : 'Failed to start process', type: 'error' };
		} finally {
			submitting = false;
		}
	}

	function handleFormChange(values: Record<string, unknown>) {
		formValues = values;
	}

	// Check if there's a dynamic form with fields/grids
	const hasDynamicForm = $derived(
		formDefinition &&
		((formDefinition.fields && formDefinition.fields.length > 0) ||
		(formDefinition.grids && formDefinition.grids.length > 0))
	);
</script>

<svelte:head>
	<title>{processDefinition?.name || 'Start Process'} - BPM Demo</title>
</svelte:head>

{#if toast}
	<Toast message={toast.message} type={toast.type} onClose={() => toast = null} />
{/if}

<div class="max-w-4xl mx-auto px-4 py-8">
	<a href="/processes/manage" class="inline-flex items-center text-sm text-gray-600 hover:text-gray-900 mb-6">
		<svg class="w-4 h-4 mr-1" fill="none" viewBox="0 0 24 24" stroke="currentColor">
			<path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 19l-7-7 7-7" />
		</svg>
		Back to Processes
	</a>

	{#if loading}
		<div class="text-center py-12">
			<div class="w-12 h-12 border-4 border-blue-600 border-t-transparent rounded-full animate-spin mx-auto"></div>
			<p class="mt-4 text-gray-600">Loading process...</p>
		</div>
	{:else if error}
		<div class="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded-lg">
			{error}
		</div>
	{:else if processDefinition}
		<!-- Process Header -->
		<div class="card mb-6">
			<div class="flex items-start justify-between">
				<div>
					<h1 class="text-2xl font-bold text-gray-900">{processDefinition.name}</h1>
					<p class="text-sm text-gray-500 mt-1">Key: {processDefinition.key}</p>
					<p class="text-sm text-gray-500">Version: {processDefinition.version}</p>
				</div>
				<span class="px-3 py-1 bg-blue-100 text-blue-800 text-sm font-medium rounded-full">
					Start New Instance
				</span>
			</div>

			{#if processDefinition.description}
				<p class="text-gray-700 mt-4">{processDefinition.description}</p>
			{/if}
		</div>

		<!-- Start Form -->
		<div class="card">
			<h2 class="text-lg font-semibold text-gray-900 mb-4">Process Information</h2>

			<form onsubmit={(e) => { e.preventDefault(); handleSubmit(); }} class="space-y-4">
				{#if loadingForm}
					<div class="py-4 text-center">
						<div class="w-6 h-6 border-2 border-blue-600 border-t-transparent rounded-full animate-spin mx-auto"></div>
						<p class="mt-2 text-sm text-gray-600">Loading form...</p>
					</div>
				{:else if hasDynamicForm && formDefinition}
					<!-- Dynamic form with designer-defined fields -->
					<div class="mb-6">
						<DynamicForm
							bind:this={dynamicFormRef}
							fields={formDefinition.fields}
							                                                        grids={formDefinition.grids || []}
							                                                        gridConfig={formDefinition.gridConfig || { columns: 1, gap: 4 }}
							                                                        onValuesChange={handleFormChange}
							
						/>
					</div>
				{:else}
					<!-- No form defined - show info message -->
					<div class="p-4 bg-blue-50 border border-blue-200 rounded-lg">
						<div class="flex items-start">
							<svg class="w-5 h-5 text-blue-600 mt-0.5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
								<path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
							</svg>
							<div class="ml-3">
								<p class="text-blue-800 font-medium">No start form defined</p>
								<p class="text-blue-700 text-sm mt-1">
									This process does not have a start form configured. You can add form fields to the start event in the Process Designer.
								</p>
							</div>
						</div>
					</div>
				{/if}

				<div>
					<label for="businessKey" class="mb-1 block text-sm font-medium text-gray-700">
						Business Key <span class="text-gray-400">(optional)</span>
					</label>
					<input
						id="businessKey"
						type="text"
						bind:value={businessKey}
						class="w-full rounded-md border border-gray-300 px-3 py-2 text-sm focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500"
						placeholder="e.g., ORDER-001, REQ-2024-001"
					/>
					<p class="mt-1 text-xs text-gray-500">Provide a human-friendly identifier for this process instance.</p>
				</div>

				<div class="flex justify-end space-x-3 pt-4 border-t">
					<a href="/processes/manage" class="btn btn-secondary">Cancel</a>
					<button
						type="submit"
						class="btn btn-primary"
						disabled={submitting}
					>
						{#if submitting}
							<span class="inline-flex items-center">
								<svg class="animate-spin -ml-1 mr-2 h-4 w-4 text-white" fill="none" viewBox="0 0 24 24">
									<circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
									<path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
								</svg>
								Starting...
							</span>
						{:else}
							Start Process
						{/if}
					</button>
				</div>
			</form>
		</div>
	{/if}
</div>
