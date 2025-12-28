<script lang="ts">
	import { api } from '$lib/api/client';
	import type { EscalationOptions, User } from '$lib/types';
	import Modal from './Modal.svelte';
	import { getActionButtonClasses } from '$lib/utils/theme';

	interface Props {
		taskId: string;
		onSuccess?: () => void;
		onError?: (error: string) => void;
	}

	const { taskId, onSuccess, onError }: Props = $props();

	// Data state
	let options = $state<EscalationOptions | null>(null);
	let users = $state<User[]>([]);
	let loading = $state(true);
	let actionLoading = $state(false);
	let error = $state('');

	// Unified modal state - only one modal can be open at a time
	type ModalType = 'escalate' | 'deescalate' | 'handoff' | 'approve' | 'reject' | null;
	let activeModal = $state<ModalType>(null);

	// Form state for each action
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

	function closeModal() {
		activeModal = null;
	}

	function resetFormState(modal: ModalType) {
		switch (modal) {
			case 'escalate':
				escalateReason = '';
				break;
			case 'deescalate':
				deEscalateReason = '';
				break;
			case 'handoff':
				handoffUser = '';
				handoffReason = '';
				break;
			case 'approve':
				approvalComments = '';
				break;
			case 'reject':
				rejectionReason = '';
				break;
		}
	}

	async function handleAction(
		action: () => Promise<unknown>,
		validation?: () => string | null
	) {
		const validationError = validation?.();
		if (validationError) {
			onError?.(validationError);
			return;
		}

		actionLoading = true;
		try {
			await action();
			const currentModal = activeModal;
			closeModal();
			resetFormState(currentModal);
			onSuccess?.();
		} catch (err) {
			onError?.(err instanceof Error ? err.message : 'Action failed');
		} finally {
			actionLoading = false;
		}
	}

	function handleEscalate() {
		handleAction(
			() => api.escalateTask(taskId, { reason: escalateReason, targetLevel: escalateLevel }),
			() => !escalateReason.trim() ? 'Escalation reason is required' : null
		);
	}

	function handleDeEscalate() {
		handleAction(
			() => api.deEscalateTask(taskId, { reason: deEscalateReason, targetLevel: deEscalateLevel }),
			() => !deEscalateReason.trim() ? 'De-escalation reason is required' : null
		);
	}

	function handleHandoff() {
		handleAction(
			() => api.handoffTask(taskId, { toUserId: handoffUser, reason: handoffReason }),
			() => !handoffUser ? 'Please select a user' : null
		);
	}

	function handleApprove() {
		handleAction(() => api.approveTask(taskId, approvalComments));
	}

	function handleReject() {
		handleAction(
			() => api.rejectTask(taskId, rejectionReason),
			() => !rejectionReason.trim() ? 'Rejection reason is required' : null
		);
	}

	// Button configuration for cleaner rendering
	const actionButtons = $derived([
		{
			key: 'approve' as const,
			label: 'Approve',
			icon: 'M5 13l4 4L19 7',
			variant: 'approve' as const,
			show: true,
			span: false
		},
		{
			key: 'reject' as const,
			label: 'Reject',
			icon: 'M6 18L18 6M6 6l12 12',
			variant: 'reject' as const,
			show: true,
			span: false
		},
		{
			key: 'escalate' as const,
			label: 'Escalate',
			icon: 'M5 10l7-7m0 0l7 7m-7-7v18',
			variant: 'escalate' as const,
			show: options && options.escalateTo.length > 0,
			span: false
		},
		{
			key: 'deescalate' as const,
			label: 'De-escalate',
			icon: 'M19 14l-7 7m0 0l-7-7m7 7V3',
			variant: 'deescalate' as const,
			show: options && options.deEscalateTo.length > 0,
			span: false
		},
		{
			key: 'handoff' as const,
			label: 'Hand Off to Another User',
			icon: 'M17 8l4 4m0 0l-4 4m4-4H3',
			variant: 'handoff' as const,
			show: true,
			span: true
		}
	]);
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
			{#each actionButtons as btn}
				{#if btn.show}
					<button
						onclick={() => { activeModal = btn.key; }}
						class="flex items-center justify-center gap-2 px-4 py-3 rounded-lg transition-colors {getActionButtonClasses(btn.variant)} {btn.span ? 'col-span-2' : ''}"
					>
						<svg class="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
							<path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d={btn.icon} />
						</svg>
						{btn.label}
					</button>
				{/if}
			{/each}
		</div>
	{/if}
</div>

<!-- Escalate Modal -->
<Modal open={activeModal === 'escalate'} title="Escalate Task" onClose={closeModal}>
	{#snippet children()}
		<div class="space-y-4">
			<div>
				<label for="escalate-level" class="block text-sm font-medium text-gray-700 mb-1">Escalate To</label>
				<select id="escalate-level" bind:value={escalateLevel} class="w-full px-3 py-2 border border-gray-300 rounded-lg">
					{#each options?.escalateTo || [] as level}
						<option value={level}>{level}</option>
					{/each}
				</select>
			</div>
			<div>
				<label for="escalate-reason" class="block text-sm font-medium text-gray-700 mb-1">Reason *</label>
				<textarea
					id="escalate-reason"
					bind:value={escalateReason}
					rows="3"
					class="w-full px-3 py-2 border border-gray-300 rounded-lg resize-none"
					placeholder="Why are you escalating this task?"
				></textarea>
			</div>
		</div>
	{/snippet}
	{#snippet footer()}
		<button
			onclick={closeModal}
			class="flex-1 px-4 py-2 border border-gray-300 rounded-lg hover:bg-gray-50"
		>
			Cancel
		</button>
		<button
			onclick={handleEscalate}
			disabled={actionLoading}
			class="flex-1 px-4 py-2 {getActionButtonClasses('escalate')} rounded-lg disabled:opacity-50"
		>
			{actionLoading ? 'Escalating...' : 'Escalate'}
		</button>
	{/snippet}
</Modal>

<!-- De-escalate Modal -->
<Modal open={activeModal === 'deescalate'} title="De-escalate Task" onClose={closeModal}>
	{#snippet children()}
		<div class="space-y-4">
			<div>
				<label for="deescalate-level" class="block text-sm font-medium text-gray-700 mb-1">De-escalate To</label>
				<select id="deescalate-level" bind:value={deEscalateLevel} class="w-full px-3 py-2 border border-gray-300 rounded-lg">
					{#each options?.deEscalateTo || [] as level}
						<option value={level}>{level}</option>
					{/each}
				</select>
			</div>
			<div>
				<label for="deescalate-reason" class="block text-sm font-medium text-gray-700 mb-1">Reason *</label>
				<textarea
					id="deescalate-reason"
					bind:value={deEscalateReason}
					rows="3"
					class="w-full px-3 py-2 border border-gray-300 rounded-lg resize-none"
					placeholder="Why are you de-escalating this task?"
				></textarea>
			</div>
		</div>
	{/snippet}
	{#snippet footer()}
		<button
			onclick={closeModal}
			class="flex-1 px-4 py-2 border border-gray-300 rounded-lg hover:bg-gray-50"
		>
			Cancel
		</button>
		<button
			onclick={handleDeEscalate}
			disabled={actionLoading}
			class="flex-1 px-4 py-2 {getActionButtonClasses('deescalate')} rounded-lg disabled:opacity-50"
		>
			{actionLoading ? 'De-escalating...' : 'De-escalate'}
		</button>
	{/snippet}
</Modal>

<!-- Handoff Modal -->
<Modal open={activeModal === 'handoff'} title="Hand Off Task" onClose={closeModal}>
	{#snippet children()}
		<div class="space-y-4">
			<div>
				<label for="handoff-user" class="block text-sm font-medium text-gray-700 mb-1">Assign To *</label>
				<select id="handoff-user" bind:value={handoffUser} class="w-full px-3 py-2 border border-gray-300 rounded-lg">
					<option value="">Select a user...</option>
					{#each users as user}
						<option value={user.username}>{user.displayName} ({user.username})</option>
					{/each}
				</select>
			</div>
			<div>
				<label for="handoff-reason" class="block text-sm font-medium text-gray-700 mb-1">Reason</label>
				<textarea
					id="handoff-reason"
					bind:value={handoffReason}
					rows="3"
					class="w-full px-3 py-2 border border-gray-300 rounded-lg resize-none"
					placeholder="Why are you handing off this task? (optional)"
				></textarea>
			</div>
		</div>
	{/snippet}
	{#snippet footer()}
		<button
			onclick={closeModal}
			class="flex-1 px-4 py-2 border border-gray-300 rounded-lg hover:bg-gray-50"
		>
			Cancel
		</button>
		<button
			onclick={handleHandoff}
			disabled={actionLoading}
			class="flex-1 px-4 py-2 {getActionButtonClasses('handoff')} rounded-lg disabled:opacity-50"
		>
			{actionLoading ? 'Handing off...' : 'Hand Off'}
		</button>
	{/snippet}
</Modal>

<!-- Approve Modal -->
<Modal open={activeModal === 'approve'} title="Approve Task" onClose={closeModal}>
	{#snippet children()}
		<div>
			<label for="approval-comments" class="block text-sm font-medium text-gray-700 mb-1">Comments (optional)</label>
			<textarea
				id="approval-comments"
				bind:value={approvalComments}
				rows="3"
				class="w-full px-3 py-2 border border-gray-300 rounded-lg resize-none"
				placeholder="Any comments for approval..."
			></textarea>
		</div>
	{/snippet}
	{#snippet footer()}
		<button
			onclick={closeModal}
			class="flex-1 px-4 py-2 border border-gray-300 rounded-lg hover:bg-gray-50"
		>
			Cancel
		</button>
		<button
			onclick={handleApprove}
			disabled={actionLoading}
			class="flex-1 px-4 py-2 {getActionButtonClasses('approve')} rounded-lg disabled:opacity-50"
		>
			{actionLoading ? 'Approving...' : 'Approve'}
		</button>
	{/snippet}
</Modal>

<!-- Reject Modal -->
<Modal open={activeModal === 'reject'} title="Reject Task" onClose={closeModal}>
	{#snippet children()}
		<div>
			<label for="rejection-reason" class="block text-sm font-medium text-gray-700 mb-1">Reason *</label>
			<textarea
				id="rejection-reason"
				bind:value={rejectionReason}
				rows="3"
				class="w-full px-3 py-2 border border-gray-300 rounded-lg resize-none"
				placeholder="Why are you rejecting this task?"
			></textarea>
		</div>
	{/snippet}
	{#snippet footer()}
		<button
			onclick={closeModal}
			class="flex-1 px-4 py-2 border border-gray-300 rounded-lg hover:bg-gray-50"
		>
			Cancel
		</button>
		<button
			onclick={handleReject}
			disabled={actionLoading}
			class="flex-1 px-4 py-2 {getActionButtonClasses('reject')} rounded-lg disabled:opacity-50"
		>
			{actionLoading ? 'Rejecting...' : 'Reject'}
		</button>
	{/snippet}
</Modal>
