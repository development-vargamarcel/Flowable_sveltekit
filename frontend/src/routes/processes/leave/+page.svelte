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
		calculateDays,
		formatISODate,
		type ValidationRule
	} from '$lib/utils/form-helpers';

	// Form values
	let leaveType = $state('');
	let startDate = $state('');
	let endDate = $state('');
	let reason = $state('');

	// Form state
	let submitting = $state(false);
	let savingDraft = $state(false);
	let draftProcessInstanceId = $state<string | undefined>(undefined);
	let toast = $state<{ message: string; type: 'success' | 'error' } | null>(null);
	const touched = $state<Record<string, boolean>>({});
	const fieldErrors = $state<Record<string, string | null>>({});

	// Constants
	const leaveTypes = ['Annual Leave', 'Sick Leave', 'Personal Leave', 'Parental Leave', 'Bereavement', 'Other'];
	const MAX_REASON_LENGTH = 500;
	const MAX_LEAVE_DAYS = 365;
	const today = formatISODate(new Date());

	// Computed
	const days = $derived(calculateDays(startDate, endDate));

	// Validation rules
	const validationRules: Record<string, ValidationRule[]> = {
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
					const diffDays = calculateDays(startDate, value as string);
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
		fieldErrors[field] = validateField(value, validationRules[field] || []);
	}

	function validateAllFields(): boolean {
		const values = { leaveType, startDate, endDate, reason };
		return validateAllFormFields(values, validationRules, touched, fieldErrors);
	}

	async function handleSaveDraft() {
		savingDraft = true;
		try {
			const variables = {
				leaveType,
				startDate,
				endDate,
				days,
				reason: reason.trim()
			};

			const result = await api.saveDraft(
				'leave-request',
				'Leave Request',
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
			await api.startProcess('leave-request', {
				leaveType,
				startDate,
				endDate,
				days,
				reason: reason.trim()
			});

			toast = { message: 'Leave request submitted successfully!', type: 'success' };
			setTimeout(() => goto('/'), 1500);
		} catch (err) {
			toast = { message: err instanceof Error ? err.message : 'Failed to submit leave request', type: 'error' };
		} finally {
			submitting = false;
		}
	}
</script>

<svelte:head>
	<title>Request Leave - BPM Demo</title>
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
			<div class="w-12 h-12 bg-sky-500 rounded-lg flex items-center justify-center text-white text-2xl">
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
					class="input {getInputClasses(touched.leaveType, fieldErrors.leaveType)}"
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
						class="input {getInputClasses(touched.startDate, fieldErrors.startDate)}"
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
						class="input {getInputClasses(touched.endDate, fieldErrors.endDate)}"
						aria-describedby={fieldErrors.endDate ? 'endDate-error' : undefined}
						aria-invalid={touched.endDate && !!fieldErrors.endDate}
					/>
					{#if touched.endDate && fieldErrors.endDate}
						<p id="endDate-error" class="text-sm text-red-600 mt-1">{fieldErrors.endDate}</p>
					{/if}
				</div>
			</div>

			{#if days > 0}
				<div class="bg-gray-50 rounded-lg p-3 text-center">
					<span class="text-2xl font-bold text-gray-900">{days}</span>
					<span class="text-gray-600 ml-1">day{days > 1 ? 's' : ''}</span>
					{#if days > 5}
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
					class="input {getInputClasses(touched.reason, fieldErrors.reason)}"
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
					<span class="text-sm text-gray-400">{reason.length}/{MAX_REASON_LENGTH}</span>
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
					{submitting ? 'Submitting...' : 'Submit Request'}
				</button>
			</div>
		</form>
	</div>
</div>
