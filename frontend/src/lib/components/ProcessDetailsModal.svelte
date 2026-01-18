<script lang="ts">
	import type { WorkflowHistory } from '$lib/types';
	import ProcessTimeline from './ProcessTimeline.svelte';
	import Modal from './Modal.svelte';

	interface Props {
		process: WorkflowHistory | null;
		onClose: () => void;
	}

	const { process, onClose }: Props = $props();

	function getStatusColor(status: string): string {
		switch (status) {
			case 'ACTIVE':
				return 'bg-blue-100 text-blue-800';
			case 'COMPLETED':
				return 'bg-green-100 text-green-800';
			case 'SUSPENDED':
				return 'bg-yellow-100 text-yellow-800';
			case 'TERMINATED':
				return 'bg-red-100 text-red-800';
			default:
				return 'bg-gray-100 text-gray-800';
		}
	}

	function formatDate(dateStr: string): string {
		return new Date(dateStr).toLocaleString();
	}

	function formatDuration(millis: number | null): string {
		if (!millis) return 'N/A';
		const hours = Math.floor(millis / 3600000);
		const minutes = Math.floor((millis % 3600000) / 60000);
		if (hours > 24) {
			const days = Math.floor(hours / 24);
			return `${days}d ${hours % 24}h`;
		}
		return `${hours}h ${minutes}m`;
	}
</script>

<Modal
	open={process !== null}
	title={process?.processDefinitionName || process?.processDefinitionKey || 'Process Details'}
	onClose={onClose}
	maxWidth="4xl"
>
	{#if process}
		<div class="mb-4">
			<p class="text-sm text-gray-500">Business Key: {process.businessKey}</p>
		</div>

		<!-- Status and Info -->
		<div class="grid grid-cols-2 md:grid-cols-4 gap-4 mb-6">
			<div class="bg-gray-50 rounded-lg p-3">
				<div class="text-xs text-gray-500 uppercase">Status</div>
				<div class="mt-1">
					<span
						class="px-2 py-1 rounded-full text-sm font-medium {getStatusColor(process.status)}"
					>
						{process.status}
					</span>
				</div>
			</div>
			<div class="bg-gray-50 rounded-lg p-3">
				<div class="text-xs text-gray-500 uppercase">Current Level</div>
				<div class="mt-1 font-medium text-indigo-700">{process.currentLevel}</div>
			</div>
			<div class="bg-gray-50 rounded-lg p-3">
				<div class="text-xs text-gray-500 uppercase">Escalations</div>
				<div class="mt-1 font-medium text-amber-700">{process.escalationCount}</div>
			</div>
			<div class="bg-gray-50 rounded-lg p-3">
				<div class="text-xs text-gray-500 uppercase">Duration</div>
				<div class="mt-1 font-medium">{formatDuration(process.durationInMillis)}</div>
			</div>
		</div>

		<!-- Timeline -->
		<div class="mb-6">
			<h3 class="font-semibold text-gray-800 mb-3">Process Timeline</h3>
			<ProcessTimeline
				taskHistory={process.taskHistory}
				escalationHistory={process.escalationHistory}
				approvals={process.approvals}
			/>
		</div>

		<!-- Approvals -->
		{#if process.approvals.length > 0}
			<div class="mb-6">
				<h3 class="font-semibold text-gray-800 mb-3">Approval History</h3>
				<div class="space-y-2">
					{#each process.approvals as approval}
						<div class="flex items-center justify-between bg-gray-50 rounded-lg p-3">
							<div class="flex items-center gap-3">
								<span
									class="w-8 h-8 flex items-center justify-center rounded-full
                                    {approval.decision === 'APPROVED'
										? 'bg-green-100 text-green-600'
										: approval.decision === 'REJECTED'
											? 'bg-red-100 text-red-600'
											: 'bg-amber-100 text-amber-600'}"
								>
									{approval.stepOrder}
								</span>
								<div>
									<div class="font-medium">{approval.taskName}</div>
									<div class="text-sm text-gray-500">
										{approval.approverId} ({approval.approverLevel})
									</div>
								</div>
							</div>
							<div class="text-right">
								<span
									class="px-2 py-1 rounded text-xs font-medium
                                    {approval.decision === 'APPROVED'
										? 'bg-green-100 text-green-700'
										: approval.decision === 'REJECTED'
											? 'bg-red-100 text-red-700'
											: 'bg-amber-100 text-amber-700'}"
								>
									{approval.decision}
								</span>
								<div class="text-xs text-gray-500 mt-1">
									{formatDate(approval.timestamp)}
								</div>
							</div>
						</div>
					{/each}
				</div>
			</div>
		{/if}

		<!-- Escalation History -->
		{#if process.escalationHistory.length > 0}
			<div class="mb-6">
				<h3 class="font-semibold text-gray-800 mb-3">Escalation History</h3>
				<div class="space-y-2">
					{#each process.escalationHistory as escalation}
						<div class="flex items-center justify-between bg-amber-50 rounded-lg p-3">
							<div class="flex items-center gap-3">
								<span class="text-lg">
									{escalation.type === 'ESCALATE' ? '⬆️' : '⬇️'}
								</span>
								<div>
									<div class="font-medium">
										{escalation.fromLevel} → {escalation.toLevel}
									</div>
									<div class="text-sm text-gray-600">{escalation.reason}</div>
								</div>
							</div>
							<div class="text-right text-sm text-gray-500">
								<div>{escalation.fromUserId}</div>
								<div>{formatDate(escalation.timestamp)}</div>
							</div>
						</div>
					{/each}
				</div>
			</div>
		{/if}

		<!-- Variables -->
		<div>
			<h3 class="font-semibold text-gray-800 mb-3">Process Variables</h3>
			<div class="bg-gray-50 rounded-lg p-4 overflow-x-auto">
				<pre class="text-sm text-gray-700">{JSON.stringify(process.variables, null, 2)}</pre>
			</div>
		</div>
	{/if}
</Modal>
