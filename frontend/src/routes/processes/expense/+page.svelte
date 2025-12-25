<script lang="ts">
	import { goto } from '$app/navigation';
	import { api } from '$lib/api/client';
	import Toast from '$lib/components/Toast.svelte';

	let amount = $state<number | null>(null);
	let category = $state('');
	let description = $state('');
	let submitting = $state(false);
	let toast = $state<{ message: string; type: 'success' | 'error' } | null>(null);

	const categories = ['Travel', 'Office Supplies', 'Meals', 'Equipment', 'Software', 'Training', 'Other'];

	async function handleSubmit(event: Event) {
		event.preventDefault();

		if (!amount || amount <= 0) {
			toast = { message: 'Please enter a valid amount', type: 'error' };
			return;
		}

		if (!category) {
			toast = { message: 'Please select a category', type: 'error' };
			return;
		}

		submitting = true;
		try {
			const response = await api.startProcess('expense-approval', {
				amount,
				category,
				description,
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
	<Toast message={toast.message} type={toast.type} onClose={() => toast = null} />
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
				<strong>Approval Process:</strong> Expenses up to $500 are approved by supervisors.
				Amounts over $500 require additional executive approval.
			</p>
		</div>

		<form onsubmit={handleSubmit} class="space-y-4">
			<div>
				<label for="amount" class="label">Amount ($) *</label>
				<input
					id="amount"
					type="number"
					step="0.01"
					min="0.01"
					bind:value={amount}
					class="input"
					placeholder="0.00"
					required
				/>
			</div>

			<div>
				<label for="category" class="label">Category *</label>
				<select id="category" bind:value={category} class="input" required>
					<option value="">Select a category</option>
					{#each categories as cat}
						<option value={cat}>{cat}</option>
					{/each}
				</select>
			</div>

			<div>
				<label for="description" class="label">Description *</label>
				<textarea
					id="description"
					bind:value={description}
					rows="3"
					class="input"
					placeholder="Describe the expense..."
					required
				></textarea>
			</div>

			<div class="flex justify-end space-x-3 pt-4">
				<a href="/processes" class="btn btn-secondary">Cancel</a>
				<button
					type="submit"
					class="btn btn-success"
					disabled={submitting}
				>
					{submitting ? 'Submitting...' : 'Submit Expense'}
				</button>
			</div>
		</form>
	</div>
</div>
