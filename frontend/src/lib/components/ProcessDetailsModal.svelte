<script lang="ts">
	import type { WorkflowHistory } from '$lib/types';
	import ProcessTimeline from './ProcessTimeline.svelte';
	import Modal from './Modal.svelte';
	import { formatDate, formatDuration } from '$lib/utils';
	import { Badge } from '$lib/components/ui/badge';

	interface Props {
		process: WorkflowHistory | null;
		onClose: () => void;
	}

	const { process, onClose }: Props = $props();

	function getStatusVariant(status: string): "default" | "secondary" | "destructive" | "outline" {
		switch (status) {
			case 'ACTIVE':
				return 'default'; // Blue-ish in many themes or use a custom one
			case 'COMPLETED':
				return 'secondary'; // Green-ish often
			case 'TERMINATED':
				return 'destructive';
			default:
				return 'outline';
		}
	}

	function getStatusClass(status: string): string {
		switch (status) {
			case 'ACTIVE':
				return 'bg-blue-100 text-blue-800 hover:bg-blue-200';
			case 'COMPLETED':
				return 'bg-green-100 text-green-800 hover:bg-green-200';
			case 'SUSPENDED':
				return 'bg-yellow-100 text-yellow-800 hover:bg-yellow-200';
			case 'TERMINATED':
				return 'bg-red-100 text-red-800 hover:bg-red-200';
			default:
				return 'bg-gray-100 text-gray-800 hover:bg-gray-200';
		}
	}

	function getApprovalColor(decision: string): string {
		if (decision === 'APPROVED') return 'bg-green-100 text-green-600';
		if (decision === 'REJECTED') return 'bg-red-100 text-red-600';
		return 'bg-amber-100 text-amber-600';
	}

	function getApprovalBadgeClass(decision: string): string {
		if (decision === 'APPROVED') return 'bg-green-100 text-green-700';
		if (decision === 'REJECTED') return 'bg-red-100 text-red-700';
		return 'bg-amber-100 text-amber-700';
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
					<Badge class={getStatusClass(process.status)}>
						{process.status}
					</Badge>
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
                                    {getApprovalColor(approval.decision)}"
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
                                    {getApprovalBadgeClass(approval.decision)}"
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
