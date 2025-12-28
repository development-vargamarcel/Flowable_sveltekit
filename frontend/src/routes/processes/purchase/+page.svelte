<script lang="ts">
	import { goto } from '$app/navigation';
	import { api } from '$lib/api/client';
	import Toast from '$lib/components/Toast.svelte';
	import GridForm, { type GridColumn } from '$lib/components/GridForm.svelte';
	import { rules } from '$lib/utils/validation';

	let department = $state('');
	let vendor = $state('');
	let justification = $state('');
	let urgency = $state('normal');
	let lineItems = $state<Record<string, unknown>[]>([]);

	let loading = $state(false);
	let toast = $state<{ message: string; type: 'success' | 'error' } | null>(null);
	let errors = $state<Record<string, string>>({});

	let gridFormRef: GridForm;

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

	const lineItemColumns: GridColumn[] = [
		{
			name: 'item',
			label: 'Item/Service',
			type: 'text',
			placeholder: 'Enter item name',
			validation: [
				rules.required('Item name is required'),
				rules.minLength(3, 'Item name must be at least 3 characters')
			]
		},
		{
			name: 'description',
			label: 'Description',
			type: 'textarea',
			placeholder: 'Brief description',
			validation: [rules.minLength(5, 'Description must be at least 5 characters')]
		},
		{
			name: 'quantity',
			label: 'Quantity',
			type: 'number',
			min: 1,
			step: 1,
			placeholder: '1',
			validation: [
				rules.required('Quantity is required'),
				rules.positive('Quantity must be greater than 0')
			]
		},
		{
			name: 'unitPrice',
			label: 'Unit Price ($)',
			type: 'number',
			min: 0.01,
			step: 0.01,
			placeholder: '0.00',
			validation: [
				rules.required('Unit price is required'),
				rules.positive('Unit price must be greater than 0')
			]
		}
	];

	const totalAmount = $derived(() => {
		return lineItems.reduce((sum, item) => {
			const quantity = Number(item.quantity) || 0;
			const unitPrice = Number(item.unitPrice) || 0;
			return sum + quantity * unitPrice;
		}, 0);
	});

	function validate(): boolean {
		const newErrors: Record<string, string> = {};

		if (!department) {
			newErrors.department = 'Department is required';
		}
		if (!vendor || vendor.length < 2) {
			newErrors.vendor = 'Vendor name is required (minimum 2 characters)';
		}
		if (!justification || justification.length < 20) {
			newErrors.justification = 'Justification must be at least 20 characters';
		}
		if (lineItems.length === 0) {
			newErrors.lineItems = 'At least one line item is required';
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
		if (!validate()) {
			toast = { message: 'Please fix the errors below', type: 'error' };
			return;
		}

		// Validate grid data
		if (gridFormRef && gridFormRef.hasEditingRows()) {
			toast = { message: 'Please save or cancel all editing rows', type: 'error' };
			return;
		}

		if (gridFormRef && !gridFormRef.validateAll()) {
			toast = { message: 'Please fix errors in line items', type: 'error' };
			return;
		}

		loading = true;
		try {
			const amount = totalAmount();

			await api.startProcess('purchase-request', {
				amount,
				department,
				vendor,
				justification,
				urgency,
				lineItems: lineItems.map((item) => ({
					item: item.item,
					description: item.description || '',
					quantity: item.quantity,
					unitPrice: item.unitPrice,
					total: (Number(item.quantity) || 0) * (Number(item.unitPrice) || 0)
				}))
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

	function handleLineItemsChange(data: Record<string, unknown>[]) {
		lineItems = data;
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
		<!-- Vendor and Department -->
		<div class="card">
			<h2 class="text-lg font-semibold text-gray-800 mb-4">Purchase Details</h2>

			<div class="grid gap-6">
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
					<span class="block text-sm font-medium text-gray-700 mb-2">Urgency</span>
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

		<!-- Line Items Grid -->
		<div class="card">
			<GridForm
				bind:this={gridFormRef}
				columns={lineItemColumns}
				label="Line Items"
				description="Add items or services to be purchased. You can add, edit, and remove rows as needed."
				minRows={0}
				onDataChange={handleLineItemsChange}
			/>
			{#if errors.lineItems}
				<p class="mt-2 text-sm text-red-600">{errors.lineItems}</p>
			{/if}

			<!-- Total Amount Display -->
			{#if lineItems.length > 0}
				<div class="mt-6 pt-4 border-t border-gray-200">
					<div class="flex justify-between items-center">
						<span class="text-lg font-semibold text-gray-900">Total Amount:</span>
						<span class="text-2xl font-bold text-blue-600">
							${totalAmount().toFixed(2)}
						</span>
					</div>
					{#if totalAmount() > 0}
						<p class="mt-2 text-sm text-indigo-600 bg-indigo-50 px-3 py-2 rounded">
							{getApprovalPathInfo(totalAmount())}
						</p>
					{/if}
				</div>
			{/if}
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
