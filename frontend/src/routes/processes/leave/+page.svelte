<script lang="ts">
	import { goto } from '$app/navigation';
	import { api } from '$lib/api/client';
	import Toast from '$lib/components/Toast.svelte';
	import { rules, validateField } from '$lib/utils/validation';

	let leaveType = $state('');
	let startDate = $state('');
	let endDate = $state('');
	let reason = $state('');
	let submitting = $state(false);
	let toast = $state<{ message: string; type: 'success' | 'error' } | null>(null);
	const touched = $state<Record<string, boolean>>({});
	const fieldErrors = $state<Record<string, string | null>>({});

	const leaveTypes = [
		'Annual Leave',
		'Sick Leave',
		'Personal Leave',
		'Parental Leave',
		'Bereavement',
		'Other'
	];

	const MAX_REASON_LENGTH = 500;
	const MAX_LEAVE_DAYS = 365;

	const days = $derived(() => {
		if (!startDate || !endDate) return 0;
		const start = new Date(startDate);
		const end = new Date(endDate);
		const diffTime = Math.abs(end.getTime() - start.getTime());
		return Math.ceil(diffTime / (1000 * 60 * 60 * 24)) + 1;
	});

	// Validation rules
	const validationRules = {
		leaveType: [rules.required('Please select a leave type')],
		startDate: [rules.required('Please select a start date')],
		endDate: [
			rules.required('Please select an end date'),
			{
				validate: (value: unknown) => {
					if (!value || !startDate) return true;
					return new Date(value as string) >= new Date(startDate);
				},
				message: 'End date must be on or after start date'
			},
			{
				validate: (value: unknown) => {
					if (!value || !startDate) return true;
					const start = new Date(startDate);
					const end = new Date(value as string);
					const diffDays =
						Math.ceil((end.getTime() - start.getTime()) / (1000 * 60 * 60 * 24)) + 1;
					return diffDays <= MAX_LEAVE_DAYS;
				},
				message: `Leave cannot exceed ${MAX_LEAVE_DAYS} days`
			}
		],
		reason: [
			rules.maxLength(MAX_REASON_LENGTH, `Reason cannot exceed ${MAX_REASON_LENGTH} characters`)
		]
	};

	function validateFieldOnBlur(field: string, value: unknown) {
		touched[field] = true;
		const fieldRules = validationRules[field as keyof typeof validationRules];
		if (fieldRules) {
			fieldErrors[field] = validateField(value, fieldRules);
		}
	}

	function validateAllFields(): boolean {
		const fields = { leaveType, startDate, endDate, reason };
		let isValid = true;

		for (const [field, value] of Object.entries(fields)) {
			touched[field] = true;
			const fieldRules = validationRules[field as keyof typeof validationRules];
			if (fieldRules) {
				const error = validateField(value, fieldRules);
				fieldErrors[field] = error;
				if (error) isValid = false;
			}
		}

		return isValid;
	}

	async function handleSubmit(event: Event) {
		event.preventDefault();

		if (!validateAllFields()) {
			toast = { message: 'Please fix the errors below', type: 'error' };
			return;
		}

		submitting = true;
		try {
			await api.startProcess('leave-request', {
				leaveType,
				startDate,
				endDate,
				days: days(),
				reason: reason.trim()
			});

			toast = { message: 'Leave request submitted successfully!', type: 'success' };
			setTimeout(() => goto('/'), 1500);
		} catch (err) {
			toast = {
				message: err instanceof Error ? err.message : 'Failed to submit leave request',
				type: 'error'
			};
		} finally {
			submitting = false;
		}
	}

	// Set minimum date to today
	const today = new Date().toISOString().split('T')[0];
</script>

<svelte:head>
	<title>Request Leave - BPM Demo</title>
</svelte:head>

{#if toast}
	<Toast message={toast.message} type={toast.type} onClose={() => (toast = null)} />
{/if}

<div class="max-w-2xl mx-auto px-4 py-8">
	<a
		href="/processes"
		class="inline-flex items-center text-sm text-gray-600 hover:text-gray-900 mb-6"
	>
		<svg class="w-4 h-4 mr-1" fill="none" viewBox="0 0 24 24" stroke="currentColor">
			<path
				stroke-linecap="round"
				stroke-linejoin="round"
				stroke-width="2"
				d="M15 19l-7-7 7-7"
			/>
		</svg>
		Back to Processes
	</a>

	<div class="card">
		<div class="flex items-center space-x-4 mb-6">
			<div
				class="w-12 h-12 bg-sky-500 rounded-lg flex items-center justify-center text-white text-2xl"
			>
				📅
			</div>
			<div>
				<h1 class="text-xl font-bold text-gray-900">Request Leave</h1>
				<p class="text-gray-600">Submit a time-off request</p>
			</div>
		</div>

		<div class="bg-blue-50 border border-blue-200 rounded-lg p-4 mb-6">
			<p class="text-sm text-blue-800">
				<strong>Approval Process:</strong> Leave requests up to 5 days are approved by supervisors.
				Longer leaves require additional executive approval.
			</p>
		</div>

		<form onsubmit={handleSubmit} class="space-y-4" novalidate>
			<div>
				<label for="leaveType" class="label">Leave Type *</label>
				<select
					id="leaveType"
					bind:value={leaveType}
					onblur={() => validateFieldOnBlur('leaveType', leaveType)}
					class="input {touched.leaveType && fieldErrors.leaveType ? 'border-red-500 focus:ring-red-500' : ''}"
					aria-describedby={fieldErrors.leaveType ? 'leaveType-error' : undefined}
					aria-invalid={touched.leaveType && !!fieldErrors.leaveType}
				>
					<option value="">Select leave type</option>
					{#each leaveTypes as type}
						<option value={type}>{type}</option>
					{/each}
				</select>
				{#if touched.leaveType && fieldErrors.leaveType}
					<p id="leaveType-error" class="text-sm text-red-600 mt-1">{fieldErrors.leaveType}</p>
				{/if}
			</div>

			<div class="grid grid-cols-2 gap-4">
				<div>
					<label for="startDate" class="label">Start Date *</label>
					<input
						id="startDate"
						type="date"
						bind:value={startDate}
						onblur={() => validateFieldOnBlur('startDate', startDate)}
						min={today}
						class="input {touched.startDate && fieldErrors.startDate ? 'border-red-500 focus:ring-red-500' : ''}"
						aria-describedby={fieldErrors.startDate ? 'startDate-error' : undefined}
						aria-invalid={touched.startDate && !!fieldErrors.startDate}
					/>
					{#if touched.startDate && fieldErrors.startDate}
						<p id="startDate-error" class="text-sm text-red-600 mt-1">{fieldErrors.startDate}</p>
					{/if}
				</div>
				<div>
					<label for="endDate" class="label">End Date *</label>
					<input
						id="endDate"
						type="date"
						bind:value={endDate}
						onblur={() => validateFieldOnBlur('endDate', endDate)}
						min={startDate || today}
						class="input {touched.endDate && fieldErrors.endDate ? 'border-red-500 focus:ring-red-500' : ''}"
						aria-describedby={fieldErrors.endDate ? 'endDate-error' : undefined}
						aria-invalid={touched.endDate && !!fieldErrors.endDate}
					/>
					{#if touched.endDate && fieldErrors.endDate}
						<p id="endDate-error" class="text-sm text-red-600 mt-1">{fieldErrors.endDate}</p>
					{/if}
				</div>
			</div>

			{#if days() > 0}
				<div class="bg-gray-50 rounded-lg p-3 text-center">
					<span class="text-2xl font-bold text-gray-900">{days()}</span>
					<span class="text-gray-600 ml-1">day{days() > 1 ? 's' : ''}</span>
					{#if days() > 5}
						<p class="text-sm text-orange-600 mt-1">* Requires executive approval</p>
					{/if}
				</div>
			{/if}

			<div>
				<label for="reason" class="label">Reason (Optional)</label>
				<textarea
					id="reason"
					bind:value={reason}
					onblur={() => validateFieldOnBlur('reason', reason)}
					rows="3"
					maxlength={MAX_REASON_LENGTH}
					class="input {touched.reason && fieldErrors.reason ? 'border-red-500 focus:ring-red-500' : ''}"
					placeholder="Optional: Provide reason for leave..."
					aria-describedby={fieldErrors.reason ? 'reason-error' : 'reason-hint'}
					aria-invalid={touched.reason && !!fieldErrors.reason}
				></textarea>
				<div class="flex justify-between mt-1">
					{#if touched.reason && fieldErrors.reason}
						<p id="reason-error" class="text-sm text-red-600">{fieldErrors.reason}</p>
					{:else}
						<p id="reason-hint" class="text-sm text-gray-500">Optional</p>
					{/if}
					<span class="text-sm text-gray-400">
						{reason.length}/{MAX_REASON_LENGTH}
					</span>
				</div>
			</div>

			<div class="flex justify-end space-x-3 pt-4">
				<a href="/processes" class="btn btn-secondary">Cancel</a>
				<button type="submit" class="btn btn-success" disabled={submitting}>
					{submitting ? 'Submitting...' : 'Submit Request'}
				</button>
			</div>
		</form>
	</div>
</div>
