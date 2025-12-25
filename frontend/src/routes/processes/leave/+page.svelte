<script lang="ts">
	import { goto } from '$app/navigation';
	import { api } from '$lib/api/client';
	import Toast from '$lib/components/Toast.svelte';

	let leaveType = $state('');
	let startDate = $state('');
	let endDate = $state('');
	let reason = $state('');
	let submitting = $state(false);
	let toast = $state<{ message: string; type: 'success' | 'error' } | null>(null);

	const leaveTypes = ['Annual Leave', 'Sick Leave', 'Personal Leave', 'Parental Leave', 'Bereavement', 'Other'];

	let days = $derived(() => {
		if (!startDate || !endDate) return 0;
		const start = new Date(startDate);
		const end = new Date(endDate);
		const diffTime = Math.abs(end.getTime() - start.getTime());
		return Math.ceil(diffTime / (1000 * 60 * 60 * 24)) + 1;
	});

	async function handleSubmit(event: Event) {
		event.preventDefault();

		if (!leaveType) {
			toast = { message: 'Please select a leave type', type: 'error' };
			return;
		}

		if (!startDate || !endDate) {
			toast = { message: 'Please select start and end dates', type: 'error' };
			return;
		}

		if (new Date(endDate) < new Date(startDate)) {
			toast = { message: 'End date must be after start date', type: 'error' };
			return;
		}

		submitting = true;
		try {
			const response = await api.startProcess('leave-request', {
				leaveType,
				startDate,
				endDate,
				days: days(),
				reason
			});

			toast = { message: 'Leave request submitted successfully!', type: 'success' };
			setTimeout(() => goto('/'), 1500);
		} catch (err) {
			toast = { message: err instanceof Error ? err.message : 'Failed to submit leave request', type: 'error' };
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

		<form onsubmit={handleSubmit} class="space-y-4">
			<div>
				<label for="leaveType" class="label">Leave Type *</label>
				<select id="leaveType" bind:value={leaveType} class="input" required>
					<option value="">Select leave type</option>
					{#each leaveTypes as type}
						<option value={type}>{type}</option>
					{/each}
				</select>
			</div>

			<div class="grid grid-cols-2 gap-4">
				<div>
					<label for="startDate" class="label">Start Date *</label>
					<input
						id="startDate"
						type="date"
						bind:value={startDate}
						min={today}
						class="input"
						required
					/>
				</div>
				<div>
					<label for="endDate" class="label">End Date *</label>
					<input
						id="endDate"
						type="date"
						bind:value={endDate}
						min={startDate || today}
						class="input"
						required
					/>
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
				<label for="reason" class="label">Reason</label>
				<textarea
					id="reason"
					bind:value={reason}
					rows="3"
					class="input"
					placeholder="Optional: Provide reason for leave..."
				></textarea>
			</div>

			<div class="flex justify-end space-x-3 pt-4">
				<a href="/processes" class="btn btn-secondary">Cancel</a>
				<button
					type="submit"
					class="btn btn-success"
					disabled={submitting}
				>
					{submitting ? 'Submitting...' : 'Submit Request'}
				</button>
			</div>
		</form>
	</div>
</div>
