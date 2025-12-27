<script lang="ts">
	import { goto } from '$app/navigation';
	import { api } from '$lib/api/client';
	import Toast from '$lib/components/Toast.svelte';

	let amount = $state<number | null>(null);
	let department = $state('');
	let description = $state('');
	let vendor = $state('');
	let justification = $state('');
	let urgency = $state('normal');

	let loading = $state(false);
	let toast = $state<{ message: string; type: 'success' | 'error' } | null>(null);
	let errors = $state<Record<string, string>>({});

	const departments = [
		'Engineering',
		'Marketing',
		'Sales',
		'Operations',
		'Finance',
		'HR',
		'Legal',
		'IT'
	];

	function validate(): boolean {
		const newErrors: Record<string, string> = {};

		if (!amount || amount <= 0) {
			newErrors.amount = 'Amount must be greater than 0';
		}
		if (!department) {
			newErrors.department = 'Department is required';
		}
		if (!description || description.length < 10) {
			newErrors.description = 'Description must be at least 10 characters';
		}
		if (!vendor) {
			newErrors.vendor = 'Vendor is required';
		}
		if (!justification || justification.length < 20) {
			newErrors.justification = 'Justification must be at least 20 characters';
		}

		errors = newErrors;
		return Object.keys(newErrors).length === 0;
	}

	function getApprovalPathInfo(amt: number): string {
		if (amt <= 1000) {
			return 'Auto-approved (< $1,000)';
		} else if (amt <= 5000) {
			return 'Supervisor approval required';
		} else if (amt <= 20000) {
			return 'Supervisor + Manager approval required';
		} else if (amt <= 50000) {
			return 'Supervisor + Manager + Director approval required';
		} else {
			return 'Full approval chain required (Supervisor → Manager → Director → Executive)';
		}
	}

	async function handleSubmit() {
		if (!validate()) return;

		loading = true;
		try {
			await api.startProcess('purchase-request', {
				amount,
				department,
				description,
				vendor,
				justification,
				urgency
			});

			toast = { message: 'Purchase request submitted successfully!', type: 'success' };
			setTimeout(() => goto('/dashboard'), 2000);
		} catch (err) {
			toast = {
				message: err instanceof Error ? err.message : 'Failed to submit purchase request',
				type: 'error'
			};
		} finally {
			loading = false;
		}
	}
</script>

<svelte:head>
	<title>Submit Purchase Request - BPM Demo</title>
</svelte:head>

{#if toast}
	<Toast message={toast.message} type={toast.type} onClose={() => (toast = null)} />
{/if}

<div class="max-w-2xl mx-auto px-4 py-8">
	<div class="mb-8">
		<a href="/processes" class="text-sm text-blue-600 hover:text-blue-700">← Back to Processes</a>
		<h1 class="text-2xl font-bold text-gray-900 mt-2">Submit Purchase Request</h1>
		<p class="text-gray-600 mt-1">
			Request approval for purchases with hierarchical multi-level approval
		</p>
	</div>

	<form onsubmit={(e) => { e.preventDefault(); handleSubmit(); }} class="space-y-6">
		<div class="card">
			<h2 class="text-lg font-semibold text-gray-800 mb-4">Purchase Details</h2>

			<div class="grid gap-6">
				<!-- Amount -->
				<div>
					<label for="amount" class="block text-sm font-medium text-gray-700">
						Amount ($) *
					</label>
					<input
						type="number"
						id="amount"
						bind:value={amount}
						min="0"
						step="0.01"
						class="mt-1 block w-full px-3 py-2 border border-gray-300 rounded-lg shadow-sm focus:ring-blue-500 focus:border-blue-500"
						placeholder="0.00"
					/>
					{#if errors.amount}
						<p class="mt-1 text-sm text-red-600">{errors.amount}</p>
					{/if}
					{#if amount && amount > 0}
						<p class="mt-2 text-sm text-indigo-600 bg-indigo-50 px-3 py-2 rounded">
							{getApprovalPathInfo(amount)}
						</p>
					{/if}
				</div>

				<!-- Department -->
				<div>
					<label for="department" class="block text-sm font-medium text-gray-700">
						Department *
					</label>
					<select
						id="department"
						bind:value={department}
						class="mt-1 block w-full px-3 py-2 border border-gray-300 rounded-lg shadow-sm focus:ring-blue-500 focus:border-blue-500"
					>
						<option value="">Select department...</option>
						{#each departments as dept}
							<option value={dept}>{dept}</option>
						{/each}
					</select>
					{#if errors.department}
						<p class="mt-1 text-sm text-red-600">{errors.department}</p>
					{/if}
				</div>

				<!-- Vendor -->
				<div>
					<label for="vendor" class="block text-sm font-medium text-gray-700">
						Vendor *
					</label>
					<input
						type="text"
						id="vendor"
						bind:value={vendor}
						class="mt-1 block w-full px-3 py-2 border border-gray-300 rounded-lg shadow-sm focus:ring-blue-500 focus:border-blue-500"
						placeholder="Vendor or supplier name"
					/>
					{#if errors.vendor}
						<p class="mt-1 text-sm text-red-600">{errors.vendor}</p>
					{/if}
				</div>

				<!-- Urgency -->
				<div>
					<label class="block text-sm font-medium text-gray-700 mb-2">Urgency</label>
					<div class="flex gap-4">
						{#each ['low', 'normal', 'high', 'critical'] as level}
							<label class="flex items-center">
								<input
									type="radio"
									bind:group={urgency}
									value={level}
									class="mr-2"
								/>
								<span class="capitalize">{level}</span>
							</label>
						{/each}
					</div>
				</div>

				<!-- Description -->
				<div>
					<label for="description" class="block text-sm font-medium text-gray-700">
						Description *
					</label>
					<textarea
						id="description"
						bind:value={description}
						rows="3"
						class="mt-1 block w-full px-3 py-2 border border-gray-300 rounded-lg shadow-sm focus:ring-blue-500 focus:border-blue-500 resize-none"
						placeholder="Describe what you're purchasing..."
					></textarea>
					{#if errors.description}
						<p class="mt-1 text-sm text-red-600">{errors.description}</p>
					{/if}
				</div>

				<!-- Justification -->
				<div>
					<label for="justification" class="block text-sm font-medium text-gray-700">
						Business Justification *
					</label>
					<textarea
						id="justification"
						bind:value={justification}
						rows="4"
						class="mt-1 block w-full px-3 py-2 border border-gray-300 rounded-lg shadow-sm focus:ring-blue-500 focus:border-blue-500 resize-none"
						placeholder="Explain why this purchase is necessary and its expected benefits..."
					></textarea>
					{#if errors.justification}
						<p class="mt-1 text-sm text-red-600">{errors.justification}</p>
					{/if}
				</div>
			</div>
		</div>

		<!-- Approval Path Info -->
		<div class="bg-indigo-50 border border-indigo-200 rounded-lg p-4">
			<h3 class="font-semibold text-indigo-800 mb-2">Approval Hierarchy</h3>
			<div class="text-sm text-indigo-700 space-y-1">
				<p><strong>Up to $1,000:</strong> Auto-approved</p>
				<p><strong>$1,001 - $5,000:</strong> Supervisor approval</p>
				<p><strong>$5,001 - $20,000:</strong> Supervisor + Manager approval</p>
				<p><strong>$20,001 - $50,000:</strong> Supervisor + Manager + Director approval</p>
				<p><strong>Over $50,000:</strong> Full approval chain including Executive</p>
			</div>
		</div>

		<button
			type="submit"
			disabled={loading}
			class="w-full flex justify-center py-3 px-4 border border-transparent rounded-lg shadow-sm text-sm font-medium text-white bg-blue-600 hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500 disabled:opacity-50"
		>
			{loading ? 'Submitting...' : 'Submit Purchase Request'}
		</button>
	</form>
</div>
