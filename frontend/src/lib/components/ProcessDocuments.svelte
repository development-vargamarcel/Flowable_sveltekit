<script lang="ts">
	/* eslint-disable no-console */
	import { api } from '$lib/api/client';
	import type { DocumentDTO } from '$lib/types';
	import { FileText, Download } from '@lucide/svelte';
	import { toast } from 'svelte-sonner';

	interface Props {
		processInstanceId: string;
	}

	const { processInstanceId }: Props = $props();

	let documents = $state<DocumentDTO[]>([]);
	let loading = $state(false);

	async function loadDocuments() {
		console.log('[ProcessDocuments] Loading documents for process:', processInstanceId);
		loading = true;
		try {
			// Fetch documents - handling pagination by requesting a larger size or just first page for now
			const response = await api.getDocuments(processInstanceId, 0, 50);
			console.log('[ProcessDocuments] Loaded documents:', response.content.length);
			documents = response.content;
		} catch (error) {
			console.error('Failed to load documents:', error);
			toast.error('Failed to load documents');
		} finally {
			loading = false;
		}
	}

	function formatBytes(bytes: number, decimals = 2) {
		if (!+bytes) return '0 Bytes';
		const k = 1024;
		const dm = decimals < 0 ? 0 : decimals;
		const sizes = ['Bytes', 'KB', 'MB', 'GB', 'TB'];
		const i = Math.floor(Math.log(bytes) / Math.log(k));
		return `${parseFloat((bytes / Math.pow(k, i)).toFixed(dm))} ${sizes[i]}`;
	}

	$effect(() => {
		if (processInstanceId) {
			loadDocuments();
		}
	});
</script>

<div class="bg-white rounded-lg shadow-sm border border-gray-200 overflow-hidden">
	<div class="p-4 border-b border-gray-200 bg-gray-50 flex justify-between items-center">
		<h3 class="text-lg font-semibold text-gray-900">Related Documents</h3>
        <span class="text-xs text-gray-500">{documents.length} found</span>
	</div>

	<div class="divide-y divide-gray-100 max-h-[400px] overflow-y-auto">
		{#if loading}
			<div class="p-8 text-center text-gray-500 text-sm">Loading documents...</div>
		{:else if documents.length === 0}
			<div class="p-8 text-center text-gray-500 text-sm">No documents found for this process.</div>
		{:else}
			{#each documents as doc}
				<div class="p-4 flex items-center justify-between hover:bg-gray-50 transition-colors">
					<div class="flex items-center min-w-0 flex-1">
						<div class="flex-shrink-0 h-10 w-10 rounded-lg bg-blue-50 flex items-center justify-center text-blue-600">
							<FileText class="h-5 w-5" />
						</div>
						<div class="ml-4 min-w-0 flex-1">
							<p class="text-sm font-medium text-gray-900 truncate" title={doc.name}>
								{doc.name}
							</p>
							<p class="text-xs text-gray-500">
								{formatBytes(doc.size)} • {new Date(doc.createdAt).toLocaleDateString()} by {doc.createdBy}
                                <span class="ml-2 inline-flex items-center px-2 py-0.5 rounded text-xs font-medium bg-gray-100 text-gray-800">
                                    {doc.type}
                                </span>
							</p>
						</div>
					</div>
					<div class="ml-4 flex items-center space-x-2">
                        <!-- In a real app, this would trigger a download -->
						<button
							class="p-1 rounded-full text-gray-400 hover:text-blue-600 hover:bg-blue-50 transition-colors"
							title="Download"
						>
							<Download class="h-4 w-4" />
						</button>
					</div>
				</div>
			{/each}
		{/if}
	</div>
</div>
