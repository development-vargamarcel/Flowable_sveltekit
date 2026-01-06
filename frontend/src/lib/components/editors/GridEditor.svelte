<script lang="ts">
	import type { FormGrid } from '$lib/types';
	import Modal from '../Modal.svelte';

	interface Props {
		grid: FormGrid | null;
		onSave: (grid: FormGrid) => void;
		onClose: () => void;
		open: boolean;
	}

	const { grid, onSave, onClose, open }: Props = $props();

	let gridForm = $state({
		id: '',
		name: '',
		label: '',
		description: '',
		minRows: 0,
		maxRows: 10,
		visibilityExpression: '',
        enablePagination: false,
        pageSize: 10,
        enableSorting: false,
        enableRowActions: false,
        enableImportExport: false,
        enableGrouping: false,
        groupByColumn: ''
	});

	function generateId(prefix: string): string {
		return `${prefix}_${Date.now()}_${Math.random().toString(36).substr(2, 6)}`;
	}

	$effect(() => {
		if (grid) {
			gridForm = {
				id: grid.id || '',
				name: grid.name,
				label: grid.label,
				description: grid.description || '',
				minRows: grid.minRows || 0,
				maxRows: grid.maxRows || 10,
				visibilityExpression: grid.visibilityExpression || '',
                enablePagination: grid.enablePagination || false,
                pageSize: grid.pageSize || 10,
                enableSorting: grid.enableSorting || false,
                enableRowActions: grid.enableRowActions || false,
                enableImportExport: grid.enableImportExport || false,
                enableGrouping: grid.enableGrouping || false,
                groupByColumn: grid.groupByColumn || ''
			};
		} else {
            gridForm = {
                id: generateId('grid'),
                name: '',
                label: '',
                description: '',
                minRows: 0,
                maxRows: 10,
                visibilityExpression: '',
                enablePagination: false,
                pageSize: 10,
                enableSorting: false,
                enableRowActions: false,
                enableImportExport: false,
                enableGrouping: false,
                groupByColumn: ''
            };
        }
	});

	function handleSave() {
		const newGrid: FormGrid = {
			id: gridForm.id,
			name: gridForm.name || gridForm.label.toLowerCase().replace(/\s+/g, '_'),
			label: gridForm.label,
			description: gridForm.description,
			minRows: gridForm.minRows,
			maxRows: gridForm.maxRows,
			visibilityExpression: gridForm.visibilityExpression,
            enablePagination: gridForm.enablePagination,
            pageSize: gridForm.pageSize,
            enableSorting: gridForm.enableSorting,
            enableRowActions: gridForm.enableRowActions,
            enableImportExport: gridForm.enableImportExport,
            enableGrouping: gridForm.enableGrouping,
            groupByColumn: gridForm.groupByColumn,
			columns: grid?.columns || [],
			gridColumn: 1,
			gridRow: 1,
			gridWidth: 2,
			cssClass: ''
		};
		onSave(newGrid);
	}
</script>

<Modal
	open={open}
	title={grid ? 'Edit Grid' : 'Add Grid'}
	onClose={onClose}
	maxWidth="lg"
>
	<div class="space-y-4 max-h-[70vh] overflow-y-auto px-1">
		<div class="grid grid-cols-2 gap-4">
			<div>
				<label for="gridLabel" class="block text-sm font-medium text-gray-700 mb-1">Label</label>
				<input
					id="gridLabel"
					type="text"
					bind:value={gridForm.label}
					class="w-full px-3 py-2 border border-gray-300 rounded-md focus:ring-2 focus:ring-blue-500"
					placeholder="e.g., Products"
				/>
			</div>
			<div>
				<label for="gridName" class="block text-sm font-medium text-gray-700 mb-1">Name</label>
				<input
					id="gridName"
					type="text"
					bind:value={gridForm.name}
					class="w-full px-3 py-2 border border-gray-300 rounded-md focus:ring-2 focus:ring-blue-500"
					placeholder="e.g., products"
				/>
			</div>
		</div>

		<div>
			<label for="gridDescription" class="block text-sm font-medium text-gray-700 mb-1"
				>Description</label
			>
			<input
				id="gridDescription"
				type="text"
				bind:value={gridForm.description}
				class="w-full px-3 py-2 border border-gray-300 rounded-md focus:ring-2 focus:ring-blue-500"
				placeholder="Optional description"
			/>
		</div>

		<div class="grid grid-cols-2 gap-4">
			<div>
				<label for="minRows" class="block text-sm font-medium text-gray-700 mb-1">Min Rows</label>
				<input
					id="minRows"
					type="number"
					min="0"
					bind:value={gridForm.minRows}
					class="w-full px-3 py-2 border border-gray-300 rounded-md focus:ring-2 focus:ring-blue-500"
				/>
			</div>
			<div>
				<label for="maxRows" class="block text-sm font-medium text-gray-700 mb-1">Max Rows</label>
				<input
					id="maxRows"
					type="number"
					min="1"
					bind:value={gridForm.maxRows}
					class="w-full px-3 py-2 border border-gray-300 rounded-md focus:ring-2 focus:ring-blue-500"
				/>
			</div>
		</div>

        <!-- Grid Features -->
        <div class="border rounded-md p-4 bg-gray-50 border-gray-200">
             <h4 class="text-sm font-medium text-gray-900 mb-3">Features</h4>
             <div class="grid grid-cols-2 gap-4">
                 <div class="flex items-center">
                    <input
                      id="g_pagination"
                      type="checkbox"
                      bind:checked={gridForm.enablePagination}
                      class="h-4 w-4 rounded border-gray-300 text-blue-600 focus:ring-blue-500"
                    />
                    <label for="g_pagination" class="ml-2 block text-sm text-gray-900">Pagination</label>
                 </div>
                 {#if gridForm.enablePagination}
                    <div>
                        <label for="g_pageSize" class="block text-xs font-medium text-gray-500">Page Size</label>
                        <input
                            id="g_pageSize"
                            type="number"
                            bind:value={gridForm.pageSize}
                            class="mt-1 block w-full rounded-md border-gray-300 shadow-sm focus:border-blue-500 focus:ring-blue-500 sm:text-sm"
                        />
                    </div>
                 {/if}
                 <div class="flex items-center">
                    <input
                      id="g_sorting"
                      type="checkbox"
                      bind:checked={gridForm.enableSorting}
                      class="h-4 w-4 rounded border-gray-300 text-blue-600 focus:ring-blue-500"
                    />
                    <label for="g_sorting" class="ml-2 block text-sm text-gray-900">Sorting</label>
                 </div>
                 <div class="flex items-center">
                    <input
                      id="g_actions"
                      type="checkbox"
                      bind:checked={gridForm.enableRowActions}
                      class="h-4 w-4 rounded border-gray-300 text-blue-600 focus:ring-blue-500"
                    />
                    <label for="g_actions" class="ml-2 block text-sm text-gray-900">Row Actions</label>
                 </div>
                 <div class="flex items-center">
                    <input
                      id="g_import"
                      type="checkbox"
                      bind:checked={gridForm.enableImportExport}
                      class="h-4 w-4 rounded border-gray-300 text-blue-600 focus:ring-blue-500"
                    />
                    <label for="g_import" class="ml-2 block text-sm text-gray-900">Import/Export</label>
                 </div>
                 <div class="flex items-center">
                    <input
                      id="g_grouping"
                      type="checkbox"
                      bind:checked={gridForm.enableGrouping}
                      class="h-4 w-4 rounded border-gray-300 text-blue-600 focus:ring-blue-500"
                    />
                    <label for="g_grouping" class="ml-2 block text-sm text-gray-900">Grouping</label>
                 </div>
                 {#if gridForm.enableGrouping}
                    <div>
                        <label for="g_groupBy" class="block text-xs font-medium text-gray-500">Group By Column (Name)</label>
                         <select
                            id="g_groupBy"
                            bind:value={gridForm.groupByColumn}
                            class="mt-1 block w-full rounded-md border-gray-300 shadow-sm focus:border-blue-500 focus:ring-blue-500 sm:text-sm"
                        >
                            <option value="">-- Select Column --</option>
                            {#if grid}
                                {#each grid.columns as col}
                                    <option value={col.name}>{col.label}</option>
                                {/each}
                            {/if}
                        </select>
                    </div>
                 {/if}
             </div>
        </div>

		<!-- Advanced Behavior -->
		<div class="border rounded-md p-4 bg-purple-50 border-purple-100 mt-4">
			<h4 class="text-sm font-medium text-purple-900 mb-3">Dynamic Behavior</h4>
			<div>
				<label for="gridVisExpr" class="block text-xs font-medium text-gray-600">Visibility Expression</label>
				<input
					id="gridVisExpr"
					type="text"
					bind:value={gridForm.visibilityExpression}
					placeholder="form.showGrid"
					class="mt-1 w-full px-3 py-2 border border-gray-300 rounded-md focus:ring-2 focus:ring-purple-500 font-mono text-xs"
				/>
			</div>
		</div>
	</div>

	{#snippet footer()}
		<button
			type="button"
			onclick={onClose}
			class="px-4 py-2 text-gray-700 hover:bg-gray-100 rounded-md"
		>
			Cancel
		</button>
		<button
			type="button"
			onclick={handleSave}
			class="px-4 py-2 bg-blue-600 text-white rounded-md hover:bg-blue-700"
			disabled={!gridForm.label}
		>
			{grid ? 'Save' : 'Add Grid'}
		</button>
	{/snippet}
</Modal>
