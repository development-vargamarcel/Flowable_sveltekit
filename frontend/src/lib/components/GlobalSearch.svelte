<script lang="ts">
	/* eslint-disable no-console */
	import { Search, Loader2, FileText } from '@lucide/svelte';
	import { api } from '$lib/api/client';
	import type { Task } from '$lib/types';
	import { goto } from '$app/navigation';

	let query = $state('');
	let results = $state<Task[]>([]);
	let loading = $state(false);
	let showResults = $state(false);
	let inputRef: HTMLInputElement;

	let debounceTimer: ReturnType<typeof setTimeout>;

	function handleInput() {
		loading = true;
		showResults = true;
		clearTimeout(debounceTimer);
		
		if (query.trim().length < 2) {
			results = [];
			loading = false;
			return;
		}

		debounceTimer = setTimeout(async () => {
			try {
				const response = await api.getTasks({ text: query });
				results = response.slice(0, 5); // Limit to 5 results
			} catch (error) {
				console.error('Search failed:', error);
				results = [];
			} finally {
				loading = false;
			}
		}, 300);
	}

	function handleSelect(task: Task) {
		goto(`/tasks/${task.id}`);
		showResults = false;
		query = '';
	}

	function handleFocus() {
		if (query.trim().length >= 2) {
			showResults = true;
		}
	}

    // Simple click outside handler
    function handleClickOutside(event: MouseEvent) {
        if (inputRef && !inputRef.contains(event.target as Node) && showResults) {
            showResults = false;
        }
    }
</script>

<svelte:window onclick={handleClickOutside} />

<div class="relative hidden md:block w-64 lg:w-80 mr-4">
	<div class="relative">
		<div class="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
			<Search class="h-4 w-4 text-gray-400" />
		</div>
		<input
			bind:this={inputRef}
			type="text"
			bind:value={query}
			oninput={handleInput}
			onfocus={handleFocus}
			placeholder="Search tasks..."
			class="block w-full pl-10 pr-3 py-2 border border-gray-300 dark:border-gray-600 rounded-md leading-5 bg-gray-50 dark:bg-gray-700 text-gray-900 dark:text-gray-100 placeholder-gray-500 focus:outline-none focus:bg-white dark:focus:bg-gray-600 focus:ring-1 focus:ring-blue-500 focus:border-blue-500 sm:text-sm transition-colors"
		/>
		{#if loading}
			<div class="absolute inset-y-0 right-0 pr-3 flex items-center pointer-events-none">
				<Loader2 class="h-4 w-4 text-blue-500 animate-spin" />
			</div>
		{/if}
	</div>

	{#if showResults && query.trim().length >= 2}
		<div class="absolute mt-1 w-full bg-white dark:bg-gray-800 shadow-lg rounded-md py-1 text-base ring-1 ring-black ring-opacity-5 overflow-auto focus:outline-none sm:text-sm z-50">
			{#if results.length > 0}
				<div class="px-3 py-2 text-xs font-semibold text-gray-500 uppercase tracking-wider bg-gray-50 dark:bg-gray-700/50">
					Tasks
				</div>
				{#each results as task}
					<button
						onclick={() => handleSelect(task)}
						class="w-full text-left px-4 py-2 hover:bg-gray-100 dark:hover:bg-gray-700 flex items-start gap-3 transition-colors"
					>
						<FileText class="h-4 w-4 text-gray-400 mt-0.5" />
						<div class="min-w-0 flex-1">
							<div class="text-sm font-medium text-gray-900 dark:text-gray-100 truncate">
								{task.name}
							</div>
							{#if task.description}
								<div class="text-xs text-gray-500 truncate">
									{task.description}
								</div>
							{/if}
						</div>
					</button>
				{/each}
			{:else if !loading}
				<div class="px-4 py-3 text-sm text-gray-500 text-center">
					No tasks found.
				</div>
			{/if}
		</div>
	{/if}
</div>
