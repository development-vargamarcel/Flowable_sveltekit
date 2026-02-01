<script lang="ts">
	import type { WorkflowHistory, Comment } from '$lib/types';
	import ProcessTimeline from './ProcessTimeline.svelte';
	import ProcessDiagram from './ProcessDiagram.svelte';
	import Modal from './Modal.svelte';
	import { formatDate, formatDuration } from '$lib/utils';
	import { Badge } from '$lib/components/ui/badge';
	import { api } from '$lib/api/client';
	import { toast } from 'svelte-sonner';

	interface Props {
		process: WorkflowHistory | null;
		onClose: () => void;
	}

	const { process, onClose }: Props = $props();

	let comments = $state<Comment[]>([]);
	let newComment = $state('');
	let isSubmittingComment = $state(false);

	let activeTab: 'details' | 'diagram' = $state('details');
	let bpmnXml: string | null = $state(null);
	let activeActivityIds: string[] = $state([]);

	$effect(() => {
		if (process?.comments) {
			comments = [...process.comments];
		} else {
			comments = [];
		}
	});

	$effect(() => {
		if (activeTab === 'diagram' && !bpmnXml && process) {
			loadDiagram();
		}
	});

	async function loadDiagram() {
		if (!process) return;
		try {
			const result = await api.getProcessBpmn(process.processDefinitionId);
			bpmnXml = result.bpmn;

			// Calculate active activity IDs
			if (process.activities) {
				activeActivityIds = process.activities
					.filter(a => !a.endTime)
					.map(a => a.activityId);
			} else if (process.tasks) {
				// Fallback if activities not populated
				activeActivityIds = process.tasks
					.map(t => t.taskDefinitionKey);
			}
		} catch (e) {
			console.error(e);
			toast.error('Failed to load diagram');
		}
	}

	async function handleAddComment() {
		if (!process || !newComment.trim()) return;

		isSubmittingComment = true;
		try {
			const response = await api.addComment(process.processInstanceId, newComment);
			comments = [...comments, response.comment];
			newComment = '';
			toast.success('Comment added');
		} catch (e) {
			console.error(e);
			toast.error('Failed to add comment');
		} finally {
			isSubmittingComment = false;
		}
	}

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

	function getStatusClass(status: string | undefined): string {
		if (!status) return 'bg-gray-100 text-gray-800 hover:bg-gray-200';
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
			<div class="flex justify-between items-center">
				<p class="text-sm text-gray-500">Business Key: {process.businessKey}</p>

				<div class="bg-gray-100 p-1 rounded-lg inline-flex">
					<button
						class="px-3 py-1 text-sm font-medium rounded-md transition-all {activeTab === 'details' ? 'bg-white shadow text-gray-900' : 'text-gray-500 hover:text-gray-900'}"
						onclick={() => activeTab = 'details'}
					>
						Details
					</button>
					<button
						class="px-3 py-1 text-sm font-medium rounded-md transition-all {activeTab === 'diagram' ? 'bg-white shadow text-gray-900' : 'text-gray-500 hover:text-gray-900'}"
						onclick={() => activeTab = 'diagram'}
					>
						Diagram
					</button>
				</div>
			</div>
		</div>

		{#if activeTab === 'details'}
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
					<div class="mt-1 font-medium">{formatDuration(process.durationInMillis || null)}</div>
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

			<!-- Comments -->
			<div class="mb-6">
				<h3 class="font-semibold text-gray-800 mb-3">Comments</h3>
				{#if comments.length > 0}
					<div class="space-y-3 mb-4">
						{#each comments as comment}
							<div class="bg-gray-50 rounded-lg p-3">
								<div class="flex justify-between items-start mb-1">
									<span class="font-medium text-sm text-gray-900">{comment.authorId}</span>
									<span class="text-xs text-gray-500">{formatDate(comment.timestamp)}</span>
								</div>
								<p class="text-sm text-gray-700 whitespace-pre-wrap">{comment.message}</p>
							</div>
						{/each}
					</div>
				{/if}
				{#if process.status === 'ACTIVE'}
					<div class="flex gap-2">
						<textarea
							bind:value={newComment}
							class="flex-1 min-h-[60px] p-2 text-sm border border-gray-300 rounded-md focus:ring-blue-500 focus:border-blue-500"
							placeholder="Add a comment..."
						></textarea>
						<button
							onclick={handleAddComment}
							disabled={!newComment.trim() || isSubmittingComment}
							class="px-4 py-2 bg-blue-600 text-white text-sm font-medium rounded-md hover:bg-blue-700 disabled:opacity-50 disabled:cursor-not-allowed h-fit"
						>
							{isSubmittingComment ? 'Adding...' : 'Add'}
						</button>
					</div>
				{/if}
			</div>

			<!-- Variables -->
			<div>
				<h3 class="font-semibold text-gray-800 mb-3">Process Variables</h3>
				<div class="bg-gray-50 rounded-lg p-4 overflow-x-auto">
					<pre class="text-sm text-gray-700">{JSON.stringify(process.variables, null, 2)}</pre>
				</div>
			</div>
		{:else}
			{#if bpmnXml}
				<div class="h-[600px] bg-white border rounded">
					<ProcessDiagram {bpmnXml} {activeActivityIds} height="100%" />
				</div>
			{:else}
				<div class="h-64 flex items-center justify-center text-gray-500">
					<div class="flex flex-col items-center gap-2">
						<div class="animate-spin rounded-full h-8 w-8 border-b-2 border-gray-900"></div>
						<span>Loading diagram...</span>
					</div>
				</div>
			{/if}
		{/if}
	{/if}
</Modal>
