<script lang="ts">
	import type { TaskHistory, Escalation, Approval } from '$lib/types';

	interface Props {
		taskHistory: TaskHistory[];
		escalationHistory: Escalation[];
		approvals: Approval[];
	}

	let { taskHistory, escalationHistory, approvals }: Props = $props();

	interface TimelineEvent {
		id: string;
		type: 'task' | 'escalation' | 'approval';
		timestamp: string;
		title: string;
		subtitle: string;
		status: 'completed' | 'pending' | 'escalated' | 'rejected';
		details?: string;
	}

	function buildTimeline(): TimelineEvent[] {
		const events: TimelineEvent[] = [];

		// Add task history events
		for (const task of taskHistory) {
			events.push({
				id: `task-${task.id}`,
				type: 'task',
				timestamp: task.endTime || task.createTime,
				title: task.name,
				subtitle: task.assignee || 'Unassigned',
				status: task.endTime ? 'completed' : 'pending',
				details: task.deleteReason || undefined
			});
		}

		// Add escalation events
		for (const esc of escalationHistory) {
			events.push({
				id: `esc-${esc.id}`,
				type: 'escalation',
				timestamp: esc.timestamp,
				title: esc.type === 'ESCALATE' ? 'Escalated' : 'De-escalated',
				subtitle: `${esc.fromLevel} → ${esc.toLevel}`,
				status: 'escalated',
				details: esc.reason
			});
		}

		// Add approval events
		for (const approval of approvals) {
			events.push({
				id: `approval-${approval.id}`,
				type: 'approval',
				timestamp: approval.timestamp,
				title: `${approval.decision} by ${approval.approverId}`,
				subtitle: approval.approverLevel,
				status: approval.decision === 'APPROVED' ? 'completed' :
				        approval.decision === 'REJECTED' ? 'rejected' : 'escalated',
				details: approval.comments || undefined
			});
		}

		// Sort by timestamp
		events.sort((a, b) => new Date(a.timestamp).getTime() - new Date(b.timestamp).getTime());

		return events;
	}

	function getStatusColor(status: string): string {
		switch (status) {
			case 'completed':
				return 'bg-green-500';
			case 'pending':
				return 'bg-blue-500';
			case 'escalated':
				return 'bg-amber-500';
			case 'rejected':
				return 'bg-red-500';
			default:
				return 'bg-gray-500';
		}
	}

	function getStatusBgColor(status: string): string {
		switch (status) {
			case 'completed':
				return 'bg-green-50 border-green-200';
			case 'pending':
				return 'bg-blue-50 border-blue-200';
			case 'escalated':
				return 'bg-amber-50 border-amber-200';
			case 'rejected':
				return 'bg-red-50 border-red-200';
			default:
				return 'bg-gray-50 border-gray-200';
		}
	}

	function getTypeIcon(type: string): string {
		switch (type) {
			case 'task':
				return '📋';
			case 'escalation':
				return '⬆️';
			case 'approval':
				return '✓';
			default:
				return '•';
		}
	}

	function formatTime(dateStr: string): string {
		return new Date(dateStr).toLocaleString();
	}

	let timeline = $derived(buildTimeline());
</script>

<div class="relative">
	{#if timeline.length === 0}
		<p class="text-gray-500 text-center py-4">No timeline events yet</p>
	{:else}
		<div class="space-y-4">
			{#each timeline as event, index}
				<div class="flex gap-4">
					<!-- Timeline dot and line -->
					<div class="flex flex-col items-center">
						<div class="w-8 h-8 rounded-full {getStatusColor(event.status)} flex items-center justify-center text-white text-sm">
							{getTypeIcon(event.type)}
						</div>
						{#if index < timeline.length - 1}
							<div class="w-0.5 flex-1 bg-gray-200 mt-2"></div>
						{/if}
					</div>

					<!-- Event content -->
					<div class="flex-1 pb-4">
						<div class="border rounded-lg p-3 {getStatusBgColor(event.status)}">
							<div class="flex items-start justify-between">
								<div>
									<h4 class="font-medium text-gray-900">{event.title}</h4>
									<p class="text-sm text-gray-600">{event.subtitle}</p>
								</div>
								<span class="text-xs text-gray-500">{formatTime(event.timestamp)}</span>
							</div>
							{#if event.details}
								<p class="mt-2 text-sm text-gray-700 bg-white bg-opacity-50 rounded p-2">
									{event.details}
								</p>
							{/if}
						</div>
					</div>
				</div>
			{/each}
		</div>
	{/if}
</div>
