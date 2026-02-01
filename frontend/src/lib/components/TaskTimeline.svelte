<script lang="ts">
	import { onMount } from 'svelte';
	import { api } from '$lib/api/client';
	import type { TaskHistoryEvent } from '$lib/types';
	import { formatDistanceToNow } from 'date-fns';
	import { 
		Circle, 
		UserPlus, 
		MessageSquare, 
		FileText, 
		CheckCircle, 
		AlertCircle,
        Clock,
        Edit
	} from '@lucide/svelte';

	interface Props {
		taskId: string;
	}

	const { taskId }: Props = $props();

	let history = $state<TaskHistoryEvent[]>([]);
	let loading = $state(false);

	async function loadHistory() {
		loading = true;
		try {
			history = await api.getTaskHistory(taskId);
            // Sort by timestamp descending (newest first)
            history.sort((a, b) => new Date(b.timestamp).getTime() - new Date(a.timestamp).getTime());
		} catch (error) {
			console.error('Failed to load task history:', error);
		} finally {
			loading = false;
		}
	}

	function getIcon(type: string) {
		switch (type) {
			case 'CREATED': return Circle;
			case 'ASSIGNED': return UserPlus;
            case 'CLAIMED': return UserPlus;
            case 'UNCLAIMED': return UserPlus;
			case 'COMMENT': return MessageSquare;
			case 'DOCUMENT_UPLOAD': return FileText;
            case 'DOCUMENT_DELETE': return FileText;
			case 'COMPLETED': return CheckCircle;
            case 'STATUS_CHANGE': return Clock;
            case 'UPDATE': return Edit;
			default: return AlertCircle;
		}
	}

    function getColor(type: string) {
        switch (type) {
            case 'CREATED': return 'text-gray-500 bg-gray-100';
            case 'ASSIGNED':
            case 'CLAIMED': return 'text-blue-500 bg-blue-100';
            case 'COMMENT': return 'text-yellow-500 bg-yellow-100';
            case 'DOCUMENT_UPLOAD': return 'text-purple-500 bg-purple-100';
            case 'COMPLETED': return 'text-green-500 bg-green-100';
            default: return 'text-gray-500 bg-gray-100';
        }
    }

    function getLabel(event: TaskHistoryEvent) {
        if (event.details) return event.details;
        switch (event.type) {
            case 'CREATED': return 'Task created';
            case 'ASSIGNED': return `Assigned to ${event.userId}`;
            case 'CLAIMED': return `Claimed by ${event.userId}`;
            case 'COMMENT': return 'Comment added';
            case 'DOCUMENT_UPLOAD': return 'Document uploaded';
            case 'COMPLETED': return 'Task completed';
            default: return event.type;
        }
    }

	onMount(() => {
		loadHistory();
	});
</script>

<div class="bg-white rounded-lg shadow-sm border border-gray-200 overflow-hidden h-full">
	<div class="p-4 border-b border-gray-200 bg-gray-50">
		<h3 class="text-lg font-semibold text-gray-900">Timeline</h3>
	</div>

	<div class="p-4">
		{#if loading}
			<div class="text-center text-gray-500 text-sm py-4">Loading history...</div>
		{:else if history.length === 0}
			<div class="text-center text-gray-500 text-sm py-4">No history available.</div>
		{:else}
            <div class="flow-root">
                <ul role="list" class="-mb-8">
                    {#each history as event, i}
                        {@const Icon = getIcon(event.type)}
                        <li>
                            <div class="relative pb-8">
                                {#if i !== history.length - 1}
                                    <span class="absolute top-4 left-4 -ml-px h-full w-0.5 bg-gray-200" aria-hidden="true"></span>
                                {/if}
                                <div class="relative flex space-x-3">
                                    <div>
                                        <span class={`h-8 w-8 rounded-full flex items-center justify-center ring-8 ring-white ${getColor(event.type)}`}>
                                            <Icon class="h-4 w-4" aria-hidden="true" />
                                        </span>
                                    </div>
                                    <div class="min-w-0 flex-1 pt-1.5 flex justify-between space-x-4">
                                        <div>
                                            <p class="text-sm text-gray-900">
                                                {getLabel(event)} 
                                                <span class="font-medium text-gray-500">by {event.userName || event.userId}</span>
                                            </p>
                                        </div>
                                        <div class="text-right text-sm whitespace-nowrap text-gray-500">
                                            <time datetime={event.timestamp}>{formatDistanceToNow(new Date(event.timestamp), { addSuffix: true })}</time>
                                        </div>
                                    </div>
                                </div>
                            </div>
                        </li>
                    {/each}
                </ul>
            </div>
		{/if}
	</div>
</div>
