<script lang="ts">
	import { goto } from '$app/navigation';
	import { api } from '$lib/api/client';
	import { authStore } from '$lib/stores/auth.svelte';
	import Toast from '$lib/components/Toast.svelte';
	import {
		rules,
		validateField,
		validateAllFormFields,
		getInputClasses,
		type ValidationRule
	} from '$lib/utils/form-helpers';

	// Form values
	let amount = $state<number | null>(null);
	let category = $state('');
	let description = $state('');

	// Form state
	let submitting = $state(false);
	let savingDraft = $state(false);
	let draftProcessInstanceId = $state<string | undefined>(undefined);
	let toast = $state<{ message: string; type: 'success' | 'error' } | null>(null);
	const touched = $state<Record<string, boolean>>({});
	const fieldErrors = $state<Record<string, string | null>>({});

	// Constants
	const categories = ['Travel', 'Office Supplies', 'Meals', 'Equipment', 'Software', 'Training', 'Other'];
	const MAX_AMOUNT = 100000;
	const MAX_DESCRIPTION_LENGTH = 1000;

	// Validation rules
	const validationRules: Record<string, ValidationRule[]> = {
		amount: [
			rules.required('Please enter an amount'),
			rules.positive('Amount must be greater than zero'),
			rules.max(MAX_AMOUNT, `Amount cannot exceed $${MAX_AMOUNT.toLocaleString()}`)
		],
		category: [rules.required('Please select a category')],
		description: [
			rules.required('Please provide a description'),
			rules.minLength(10, 'Description must be at least 10 characters'),
			rules.maxLength(MAX_DESCRIPTION_LENGTH, `Description cannot exceed ${MAX_DESCRIPTION_LENGTH} characters`)
		]
	};

	function validateFieldOnBlur(field: string, value: unknown) {
		touched[field] = true;
		fieldErrors[field] = validateField(value, validationRules[field] || []);
	}

	function validateAllFields(): boolean {
		const values = { amount, category, description };
		return validateAllFormFields(values, validationRules, touched, fieldErrors);
	}

	async function handleSaveDraft() {
		savingDraft = true;
		try {
			const variables = {
				amount,
				category,
				description: description.trim(),
				submittedDate: new Date().toISOString().split('T')[0]
			};

			const result = await api.saveDraft(
				'expense-approval',
				'Expense Approval',
				variables,
				authStore.user?.username || '',
				draftProcessInstanceId
			);

			draftProcessInstanceId = result.processInstanceId;
			toast = { message: 'Draft saved successfully!', type: 'success' };
		} catch (err) {
			toast = { message: err instanceof Error ? err.message : 'Failed to save draft', type: 'error' };
		} finally {
			savingDraft = false;
		}
	}

	async function handleSubmit(event: Event) {
		event.preventDefault();

		if (!validateAllFields()) {
			toast = { message: 'Please fix the errors below', type: 'error' };
			return;
		}

		submitting = true;
		try {
			await api.startProcess('expense-approval', {
				amount,
				category,
				description: description.trim(),
				submittedDate: new Date().toISOString().split('T')[0]
			});

			toast = { message: 'Expense submitted successfully!', type: 'success' };
			setTimeout(() => goto('/'), 1500);
		} catch (err) {
			toast = { message: err instanceof Error ? err.message : 'Failed to submit expense', type: 'error' };
		} finally {
			submitting = false;
		}
	}
</script>

<svelte:head>
	<title>Submit Expense - BPM Demo</title>
</svelte:head>

{#if toast}
	<Toast message={toast.message} type={toast.type} onClose={() => (toast = null)} />
{/if}

<div class="max-w-2xl mx-auto px-4 py-8">
	<a href="/processes" class="inline-flex items-center text-sm text-gray-600 hover:text-gray-900 mb-6">
		<svg class="w-4 h-4 mr-1" fill="none" viewBox="0 0 24 24" stroke="currentColor">
			<path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 19l-7-7 7-7" />
		</svg>
		Back to Processes
	</a>

	<div class="card">
		<div class="flex items-center space-x-4 mb-6">
			<div class="w-12 h-12 bg-emerald-500 rounded-lg flex items-center justify-center text-white text-2xl">
				$
			</div>
			<div>
				<h1 class="text-xl font-bold text-gray-900">Submit Expense</h1>
				<p class="text-gray-600">Request reimbursement for business expenses</p>
			</div>
		</div>

		<div class="bg-blue-50 border border-blue-200 rounded-lg p-4 mb-6">
			<p class="text-sm text-blue-800">
				<strong>Approval Process:</strong> Expenses up to $500 are approved by supervisors. Amounts
				over $500 require additional executive approval.
			</p>
		</div>

		<form onsubmit={handleSubmit} class="space-y-4" novalidate>
			<div>
				<label for="amount" class="label">Amount ($) *</label>
				<input
					id="amount"
					type="number"
					step="0.01"
					min="0.01"
					max={MAX_AMOUNT}
					bind:value={amount}
					onblur={() => validateFieldOnBlur('amount', amount)}
					class="input {getInputClasses(touched.amount, fieldErrors.amount)}"
					placeholder="0.00"
					aria-describedby={fieldErrors.amount ? 'amount-error' : undefined}
					aria-invalid={touched.amount && !!fieldErrors.amount}
				/>
				{#if touched.amount && fieldErrors.amount}
					<p id="amount-error" class="text-sm text-red-600 mt-1">{fieldErrors.amount}</p>
				{/if}
			</div>

			<div>
				<label for="category" class="label">Category *</label>
				<select
					id="category"
					bind:value={category}
					onblur={() => validateFieldOnBlur('category', category)}
					class="input {getInputClasses(touched.category, fieldErrors.category)}"
					aria-describedby={fieldErrors.category ? 'category-error' : undefined}
					aria-invalid={touched.category && !!fieldErrors.category}
				>
					<option value="">Select a category</option>
					{#each categories as cat}
						<option value={cat}>{cat}</option>
					{/each}
				</select>
				{#if touched.category && fieldErrors.category}
					<p id="category-error" class="text-sm text-red-600 mt-1">{fieldErrors.category}</p>
				{/if}
			</div>

			<div>
				<label for="description" class="label">Description *</label>
				<textarea
					id="description"
					bind:value={description}
					onblur={() => validateFieldOnBlur('description', description)}
					rows="3"
					maxlength={MAX_DESCRIPTION_LENGTH}
					class="input {getInputClasses(touched.description, fieldErrors.description)}"
					placeholder="Describe the expense (minimum 10 characters)..."
					aria-describedby={fieldErrors.description ? 'description-error' : 'description-hint'}
					aria-invalid={touched.description && !!fieldErrors.description}
				></textarea>
				<div class="flex justify-between mt-1">
					{#if touched.description && fieldErrors.description}
						<p id="description-error" class="text-sm text-red-600">{fieldErrors.description}</p>
					{:else}
						<p id="description-hint" class="text-sm text-gray-500">Minimum 10 characters required</p>
					{/if}
					<span class="text-sm text-gray-400">{description.length}/{MAX_DESCRIPTION_LENGTH}</span>
				</div>
			</div>

			<div class="flex justify-end space-x-3 pt-4">
				<a href="/processes" class="btn btn-secondary">Cancel</a>
				<button
					type="button"
					onclick={handleSaveDraft}
					class="btn btn-secondary"
					disabled={savingDraft || submitting}
				>
					{savingDraft ? 'Saving...' : 'Save Draft'}
				</button>
				<button type="submit" class="btn btn-success" disabled={submitting || savingDraft}>
					{submitting ? 'Submitting...' : 'Submit Expense'}
				</button>
			</div>
		</form>
	</div>
</div>
