<script lang="ts">
	import { Search, X, BookmarkPlus, Trash2 } from '@lucide/svelte';
	import { browser } from '$app/environment';
	import { createEventDispatcher, onMount } from 'svelte';
	import { toast } from 'svelte-sonner';
	import { createLogger } from '$lib/utils/logger';

	interface Filters {
		text: string;
		assignee: string;
		priority: string;
		sortBy: string;
	}

	interface FilterPreset {
		id: string;
		name: string;
		filters: Filters;
		createdAt: string;
	}

	const logger = createLogger('TaskFilters');
	const presetsStorageKey = 'taskFilters:presets';

	let filters = $state<Filters>({
		text: '',
		assignee: '',
		priority: '',
		sortBy: 'created_desc'
	});

	let presetName = $state('');
	let presets = $state<FilterPreset[]>([]);
	// Track the last applied incoming filters to avoid overwriting in-progress edits.
	let lastAppliedSignature = $state('');

	interface Props {
		initialFilters?: Filters | null;
	}

	const { initialFilters = null }: Props = $props();

	const dispatch = createEventDispatcher<{
		change: Filters;
	}>();

	let timer: ReturnType<typeof setTimeout>;

	/**
	 * Debounce filter changes so we avoid spamming the API while the user types.
	 */
	function handleChange() {
		clearTimeout(timer);
		timer = setTimeout(() => {
			dispatch('change', filters);
		}, 300);
	}

	function clearFilters() {
		filters = {
			text: '',
			assignee: '',
			priority: '',
			sortBy: 'created_desc'
		};
		dispatch('change', filters);
	}

	const hasActiveFilters = $derived(
		!!filters.text || !!filters.assignee || !!filters.priority || filters.sortBy !== 'created_desc'
	);

	function readPresetsFromStorage(): FilterPreset[] {
		if (!browser) return [];
		try {
			const stored = localStorage.getItem(presetsStorageKey);
			if (!stored) return [];
			const parsed = JSON.parse(stored) as FilterPreset[];
			if (!Array.isArray(parsed)) return [];
			return parsed;
		} catch (error) {
			logger.warn('Failed to parse stored task filter presets', { error });
			return [];
		}
	}

	function savePresetsToStorage(nextPresets: FilterPreset[]) {
		if (!browser) return;
		localStorage.setItem(presetsStorageKey, JSON.stringify(nextPresets));
	}

	function generatePresetId() {
		if (browser && 'randomUUID' in crypto) {
			return crypto.randomUUID();
		}
		return `${Date.now()}-${Math.random().toString(36).slice(2, 8)}`;
	}

	function handleSavePreset() {
		const trimmedName = presetName.trim();
		if (!trimmedName) {
			toast.error('Name your preset before saving.');
			logger.warn('Preset save skipped due to missing name');
			return;
		}

		if (!hasActiveFilters) {
			toast.error('Apply at least one filter before saving a preset.');
			logger.warn('Preset save skipped due to inactive filters', { filters });
			return;
		}

		const existingIndex = presets.findIndex((preset) => preset.name.toLowerCase() === trimmedName.toLowerCase());
		const nextPreset: FilterPreset = {
			id: existingIndex >= 0 ? presets[existingIndex].id : generatePresetId(),
			name: trimmedName,
			filters: { ...filters },
			createdAt: new Date().toISOString()
		};

		const nextPresets =
			existingIndex >= 0
				? presets.map((preset, index) => (index === existingIndex ? nextPreset : preset))
				: [nextPreset, ...presets];

		presets = nextPresets;
		savePresetsToStorage(nextPresets);
		presetName = '';
		toast.success(`Saved "${trimmedName}" preset.`);
		logger.info('Saved task filter preset', { presetName: trimmedName });
	}

	function applyPreset(preset: FilterPreset) {
		filters = { ...preset.filters };
		dispatch('change', filters);
		logger.info('Applied task filter preset', { presetName: preset.name });
	}

	function deletePreset(preset: FilterPreset) {
		const nextPresets = presets.filter((item) => item.id !== preset.id);
		presets = nextPresets;
		savePresetsToStorage(nextPresets);
		toast.success(`Removed "${preset.name}" preset.`);
		logger.info('Deleted task filter preset', { presetName: preset.name });
	}

	/**
	 * Keep local filter state in sync with parent-provided filters (e.g., when
	 * restoring persisted filters on page load) while avoiding resets during typing.
	 */
	$effect(() => {
		if (!initialFilters) return;
		const signature = JSON.stringify(initialFilters);
		if (signature === lastAppliedSignature) return;
		lastAppliedSignature = signature;
		filters = { ...initialFilters };
	});

	onMount(() => {
		presets = readPresetsFromStorage();
	});
</script>

<div class="bg-white rounded-lg shadow-sm border border-gray-200 p-4 mb-6">
	<div class="flex flex-col md:flex-row gap-4">
		<!-- Search -->
		<div class="flex-grow relative">
			<div class="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
				<Search class="h-4 w-4 text-gray-400" />
			</div>
			<input
				type="text"
				bind:value={filters.text}
				oninput={handleChange}
				placeholder="Search tasks by name or description..."
				class="pl-10 block w-full rounded-md border-gray-300 shadow-sm focus:border-blue-500 focus:ring-blue-500 sm:text-sm border p-2"
			/>
		</div>

		<!-- Filters -->
		<div class="flex flex-wrap gap-4">
			<div class="w-40">
				<select
					bind:value={filters.assignee}
					onchange={handleChange}
					class="block w-full rounded-md border-gray-300 shadow-sm focus:border-blue-500 focus:ring-blue-500 sm:text-sm border p-2"
				>
					<option value="">All Assignees</option>
					<option value="admin">Admin</option>
					<option value="user1">User 1</option>
					<option value="unassigned">Unassigned</option>
				</select>
			</div>

			<div class="w-32">
				<select
					bind:value={filters.priority}
					onchange={handleChange}
					class="block w-full rounded-md border-gray-300 shadow-sm focus:border-blue-500 focus:ring-blue-500 sm:text-sm border p-2"
				>
					<option value="">All Priorities</option>
					<option value="25">Low</option>
					<option value="50">Medium</option>
					<option value="75">High</option>
					<option value="100">Critical</option>
				</select>
			</div>

			<div class="w-40">
				<select
					bind:value={filters.sortBy}
					onchange={handleChange}
					class="block w-full rounded-md border-gray-300 shadow-sm focus:border-blue-500 focus:ring-blue-500 sm:text-sm border p-2"
				>
					<option value="created_desc">Newest First</option>
					<option value="created_asc">Oldest First</option>
					<option value="priority_desc">Highest Priority</option>
					<option value="priority_asc">Lowest Priority</option>
					<option value="due_asc">Due Date (Earliest)</option>
					<option value="due_desc">Due Date (Latest)</option>
				</select>
			</div>

			{#if hasActiveFilters}
				<button
					onclick={clearFilters}
					class="inline-flex items-center px-3 py-2 border border-gray-300 shadow-sm text-sm font-medium rounded-md text-gray-700 bg-white hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500"
					title="Clear Filters"
				>
					<X class="h-4 w-4 text-gray-500" />
				</button>
			{/if}
		</div>
	</div>

	<div class="mt-4 flex flex-col gap-3">
		<div class="flex flex-col sm:flex-row sm:items-center gap-3">
			<div class="flex items-center gap-2 flex-1">
				<input
					type="text"
					bind:value={presetName}
					placeholder="Save current filters as a preset..."
					class="flex-1 rounded-md border-gray-300 shadow-sm focus:border-blue-500 focus:ring-blue-500 sm:text-sm border p-2"
				/>
				<button
					type="button"
					onclick={handleSavePreset}
					class="inline-flex items-center gap-2 px-3 py-2 text-sm font-medium text-white bg-blue-600 rounded-md hover:bg-blue-700 transition-colors"
				>
					<BookmarkPlus class="h-4 w-4" />
					Save Preset
				</button>
			</div>
		</div>

		{#if presets.length > 0}
			<div>
				<p class="text-xs font-semibold uppercase tracking-wide text-gray-500 mb-2">Saved presets</p>
				<div class="flex flex-wrap gap-2">
					{#each presets as preset (preset.id)}
						<div class="flex items-center gap-1 rounded-full border border-gray-200 bg-gray-50 px-2 py-1">
							<button
								type="button"
								onclick={() => applyPreset(preset)}
								class="text-sm text-gray-700 hover:text-blue-600 transition-colors"
							>
								{preset.name}
							</button>
							<button
								type="button"
								onclick={() => deletePreset(preset)}
								class="text-gray-400 hover:text-red-500 transition-colors"
								aria-label={`Delete ${preset.name} preset`}
							>
								<Trash2 class="h-3 w-3" />
							</button>
						</div>
					{/each}
				</div>
			</div>
		{/if}
	</div>
</div>
