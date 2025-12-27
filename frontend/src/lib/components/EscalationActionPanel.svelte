<script lang="ts">
	import { api } from '$lib/api/client';
	import type { EscalationOptions, User } from '$lib/types';

	interface Props {
		taskId: string;
		onSuccess?: () => void;
		onError?: (error: string) => void;
	}

	let { taskId, onSuccess, onError }: Props = $props();

	let options = $state<EscalationOptions | null>(null);
	let users = $state<User[]>([]);
	let loading = $state(true);
	let actionLoading = $state(false);
	let error = $state('');

	let showEscalateModal = $state(false);
	let showDeEscalateModal = $state(false);
	let showHandoffModal = $state(false);
	let showApproveModal = $state(false);
	let showRejectModal = $state(false);

	let escalateReason = $state('');
	let escalateLevel = $state('');
	let deEscalateReason = $state('');
	let deEscalateLevel = $state('');
	let handoffUser = $state('');
	let handoffReason = $state('');
	let approvalComments = $state('');
	let rejectionReason = $state('');

	$effect(() => {
		loadOptions();
	});

	async function loadOptions() {
		loading = true;
		try {
			const [optionsData, usersData] = await Promise.all([
				api.getEscalationOptions(taskId),
				api.getUsers()
			]);
			options = optionsData;
			users = usersData;
			if (options.escalateTo.length > 0) {
				escalateLevel = options.escalateTo[0];
			}
			if (options.deEscalateTo.length > 0) {
				deEscalateLevel = options.deEscalateTo[0];
			}
		} catch (err) {
			error = err instanceof Error ? err.message : 'Failed to load options';
		} finally {
			loading = false;
		}
	}

	async function handleEscalate() {
		if (!escalateReason.trim()) {
			onError?.('Escalation reason is required');
			return;
		}
		actionLoading = true;
		try {
			await api.escalateTask(taskId, {
				reason: escalateReason,
				targetLevel: escalateLevel
			});
			showEscalateModal = false;
			escalateReason = '';
			onSuccess?.();
		} catch (err) {
			onError?.(err instanceof Error ? err.message : 'Failed to escalate');
		} finally {
			actionLoading = false;
		}
	}

	async function handleDeEscalate() {
		if (!deEscalateReason.trim()) {
			onError?.('De-escalation reason is required');
			return;
		}
		actionLoading = true;
		try {
			await api.deEscalateTask(taskId, {
				reason: deEscalateReason,
				targetLevel: deEscalateLevel
			});
			showDeEscalateModal = false;
			deEscalateReason = '';
			onSuccess?.();
		} catch (err) {
			onError?.(err instanceof Error ? err.message : 'Failed to de-escalate');
		} finally {
			actionLoading = false;
		}
	}

	async function handleHandoff() {
		if (!handoffUser) {
			onError?.('Please select a user');
			return;
		}
		actionLoading = true;
		try {
			await api.handoffTask(taskId, {
				toUserId: handoffUser,
				reason: handoffReason
			});
			showHandoffModal = false;
			handoffUser = '';
			handoffReason = '';
			onSuccess?.();
		} catch (err) {
			onError?.(err instanceof Error ? err.message : 'Failed to handoff');
		} finally {
			actionLoading = false;
		}
	}

	async function handleApprove() {
		actionLoading = true;
		try {
			await api.approveTask(taskId, approvalComments);
			showApproveModal = false;
			approvalComments = '';
			onSuccess?.();
		} catch (err) {
			onError?.(err instanceof Error ? err.message : 'Failed to approve');
		} finally {
			actionLoading = false;
		}
	}

	async function handleReject() {
		if (!rejectionReason.trim()) {
			onError?.('Rejection reason is required');
			return;
		}
		actionLoading = true;
		try {
			await api.rejectTask(taskId, rejectionReason);
			showRejectModal = false;
			rejectionReason = '';
			onSuccess?.();
		} catch (err) {
			onError?.(err instanceof Error ? err.message : 'Failed to reject');
		} finally {
			actionLoading = false;
		}
	}

	function closeAllModals() {
		showEscalateModal = false;
		showDeEscalateModal = false;
		showHandoffModal = false;
		showApproveModal = false;
		showRejectModal = false;
	}
</script>

<div class="bg-white rounded-lg shadow p-4">
	<h3 class="font-semibold text-gray-800 mb-4">Task Actions</h3>

	{#if loading}
		<div class="text-center py-4">
			<div class="w-6 h-6 border-2 border-blue-600 border-t-transparent rounded-full animate-spin mx-auto"></div>
		</div>
	{:else if error}
		<p class="text-red-600 text-sm">{error}</p>
	{:else}
		<div class="grid grid-cols-2 gap-3">
			<!-- Approve Button -->
			<button
				onclick={() => { showApproveModal = true; }}
				class="flex items-center justify-center gap-2 px-4 py-3 bg-green-600 text-white rounded-lg hover:bg-green-700 transition-colors"
			>
				<svg class="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
					<path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7" />
				</svg>
				Approve
			</button>

			<!-- Reject Button -->
			<button
				onclick={() => { showRejectModal = true; }}
				class="flex items-center justify-center gap-2 px-4 py-3 bg-red-600 text-white rounded-lg hover:bg-red-700 transition-colors"
			>
				<svg class="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
					<path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
				</svg>
				Reject
			</button>

			<!-- Escalate Button -->
			{#if options && options.escalateTo.length > 0}
				<button
					onclick={() => { showEscalateModal = true; }}
					class="flex items-center justify-center gap-2 px-4 py-3 bg-amber-500 text-white rounded-lg hover:bg-amber-600 transition-colors"
				>
					<svg class="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
						<path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 10l7-7m0 0l7 7m-7-7v18" />
					</svg>
					Escalate
				</button>
			{/if}

			<!-- De-escalate Button -->
			{#if options && options.deEscalateTo.length > 0}
				<button
					onclick={() => { showDeEscalateModal = true; }}
					class="flex items-center justify-center gap-2 px-4 py-3 bg-teal-500 text-white rounded-lg hover:bg-teal-600 transition-colors"
				>
					<svg class="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
						<path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 14l-7 7m0 0l-7-7m7 7V3" />
					</svg>
					De-escalate
				</button>
			{/if}

			<!-- Handoff Button -->
			<button
				onclick={() => { showHandoffModal = true; }}
				class="flex items-center justify-center gap-2 px-4 py-3 bg-indigo-500 text-white rounded-lg hover:bg-indigo-600 transition-colors col-span-2"
			>
				<svg class="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
					<path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M17 8l4 4m0 0l-4 4m4-4H3" />
				</svg>
				Hand Off to Another User
			</button>
		</div>
	{/if}
</div>

<!-- Escalate Modal -->
{#if showEscalateModal}
	<div class="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 p-4">
		<div class="bg-white rounded-lg shadow-xl max-w-md w-full p-6">
			<h3 class="text-lg font-bold text-gray-900 mb-4">Escalate Task</h3>
			<div class="space-y-4">
				<div>
					<label class="block text-sm font-medium text-gray-700 mb-1">Escalate To</label>
					<select bind:value={escalateLevel} class="w-full px-3 py-2 border border-gray-300 rounded-lg">
						{#each options?.escalateTo || [] as level}
							<option value={level}>{level}</option>
						{/each}
					</select>
				</div>
				<div>
					<label class="block text-sm font-medium text-gray-700 mb-1">Reason *</label>
					<textarea
						bind:value={escalateReason}
						rows="3"
						class="w-full px-3 py-2 border border-gray-300 rounded-lg resize-none"
						placeholder="Why are you escalating this task?"
					></textarea>
				</div>
			</div>
			<div class="flex gap-3 mt-6">
				<button
					onclick={() => { showEscalateModal = false; }}
					class="flex-1 px-4 py-2 border border-gray-300 rounded-lg hover:bg-gray-50"
				>
					Cancel
				</button>
				<button
					onclick={handleEscalate}
					disabled={actionLoading}
					class="flex-1 px-4 py-2 bg-amber-500 text-white rounded-lg hover:bg-amber-600 disabled:opacity-50"
				>
					{actionLoading ? 'Escalating...' : 'Escalate'}
				</button>
			</div>
		</div>
	</div>
{/if}

<!-- De-escalate Modal -->
{#if showDeEscalateModal}
	<div class="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 p-4">
		<div class="bg-white rounded-lg shadow-xl max-w-md w-full p-6">
			<h3 class="text-lg font-bold text-gray-900 mb-4">De-escalate Task</h3>
			<div class="space-y-4">
				<div>
					<label class="block text-sm font-medium text-gray-700 mb-1">De-escalate To</label>
					<select bind:value={deEscalateLevel} class="w-full px-3 py-2 border border-gray-300 rounded-lg">
						{#each options?.deEscalateTo || [] as level}
							<option value={level}>{level}</option>
						{/each}
					</select>
				</div>
				<div>
					<label class="block text-sm font-medium text-gray-700 mb-1">Reason *</label>
					<textarea
						bind:value={deEscalateReason}
						rows="3"
						class="w-full px-3 py-2 border border-gray-300 rounded-lg resize-none"
						placeholder="Why are you de-escalating this task?"
					></textarea>
				</div>
			</div>
			<div class="flex gap-3 mt-6">
				<button
					onclick={() => { showDeEscalateModal = false; }}
					class="flex-1 px-4 py-2 border border-gray-300 rounded-lg hover:bg-gray-50"
				>
					Cancel
				</button>
				<button
					onclick={handleDeEscalate}
					disabled={actionLoading}
					class="flex-1 px-4 py-2 bg-teal-500 text-white rounded-lg hover:bg-teal-600 disabled:opacity-50"
				>
					{actionLoading ? 'De-escalating...' : 'De-escalate'}
				</button>
			</div>
		</div>
	</div>
{/if}

<!-- Handoff Modal -->
{#if showHandoffModal}
	<div class="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 p-4">
		<div class="bg-white rounded-lg shadow-xl max-w-md w-full p-6">
			<h3 class="text-lg font-bold text-gray-900 mb-4">Hand Off Task</h3>
			<div class="space-y-4">
				<div>
					<label class="block text-sm font-medium text-gray-700 mb-1">Assign To *</label>
					<select bind:value={handoffUser} class="w-full px-3 py-2 border border-gray-300 rounded-lg">
						<option value="">Select a user...</option>
						{#each users as user}
							<option value={user.username}>{user.displayName} ({user.username})</option>
						{/each}
					</select>
				</div>
				<div>
					<label class="block text-sm font-medium text-gray-700 mb-1">Reason</label>
					<textarea
						bind:value={handoffReason}
						rows="3"
						class="w-full px-3 py-2 border border-gray-300 rounded-lg resize-none"
						placeholder="Why are you handing off this task? (optional)"
					></textarea>
				</div>
			</div>
			<div class="flex gap-3 mt-6">
				<button
					onclick={() => { showHandoffModal = false; }}
					class="flex-1 px-4 py-2 border border-gray-300 rounded-lg hover:bg-gray-50"
				>
					Cancel
				</button>
				<button
					onclick={handleHandoff}
					disabled={actionLoading}
					class="flex-1 px-4 py-2 bg-indigo-500 text-white rounded-lg hover:bg-indigo-600 disabled:opacity-50"
				>
					{actionLoading ? 'Handing off...' : 'Hand Off'}
				</button>
			</div>
		</div>
	</div>
{/if}

<!-- Approve Modal -->
{#if showApproveModal}
	<div class="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 p-4">
		<div class="bg-white rounded-lg shadow-xl max-w-md w-full p-6">
			<h3 class="text-lg font-bold text-gray-900 mb-4">Approve Task</h3>
			<div>
				<label class="block text-sm font-medium text-gray-700 mb-1">Comments (optional)</label>
				<textarea
					bind:value={approvalComments}
					rows="3"
					class="w-full px-3 py-2 border border-gray-300 rounded-lg resize-none"
					placeholder="Any comments for approval..."
				></textarea>
			</div>
			<div class="flex gap-3 mt-6">
				<button
					onclick={() => { showApproveModal = false; }}
					class="flex-1 px-4 py-2 border border-gray-300 rounded-lg hover:bg-gray-50"
				>
					Cancel
				</button>
				<button
					onclick={handleApprove}
					disabled={actionLoading}
					class="flex-1 px-4 py-2 bg-green-600 text-white rounded-lg hover:bg-green-700 disabled:opacity-50"
				>
					{actionLoading ? 'Approving...' : 'Approve'}
				</button>
			</div>
		</div>
	</div>
{/if}

<!-- Reject Modal -->
{#if showRejectModal}
	<div class="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 p-4">
		<div class="bg-white rounded-lg shadow-xl max-w-md w-full p-6">
			<h3 class="text-lg font-bold text-gray-900 mb-4">Reject Task</h3>
			<div>
				<label class="block text-sm font-medium text-gray-700 mb-1">Reason *</label>
				<textarea
					bind:value={rejectionReason}
					rows="3"
					class="w-full px-3 py-2 border border-gray-300 rounded-lg resize-none"
					placeholder="Why are you rejecting this task?"
				></textarea>
			</div>
			<div class="flex gap-3 mt-6">
				<button
					onclick={() => { showRejectModal = false; }}
					class="flex-1 px-4 py-2 border border-gray-300 rounded-lg hover:bg-gray-50"
				>
					Cancel
				</button>
				<button
					onclick={handleReject}
					disabled={actionLoading}
					class="flex-1 px-4 py-2 bg-red-600 text-white rounded-lg hover:bg-red-700 disabled:opacity-50"
				>
					{actionLoading ? 'Rejecting...' : 'Reject'}
				</button>
			</div>
		</div>
	</div>
{/if}
