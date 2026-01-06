<script lang="ts">
	import type { FormField, FormGrid, GridColumn, ProcessFieldLibrary } from '$lib/types';
	import Sortable from 'sortablejs';
	import { complexDemoLibrary } from '$lib/utils/demo-data';
    import FieldEditor from './editors/FieldEditor.svelte';
    import GridEditor from './editors/GridEditor.svelte';
    import ColumnEditor from './editors/ColumnEditor.svelte';

	interface Props {
		library: ProcessFieldLibrary;
		onChange: (library: ProcessFieldLibrary) => void;
	}

	const { library, onChange }: Props = $props();

	// UI State
	let activeTab = $state<'fields' | 'grids'>('fields');
	let editingField = $state<FormField | null>(null);
	let editingGrid = $state<FormGrid | null>(null);
	let showFieldEditor = $state(false);
	let showGridEditor = $state(false);
	let expandedGridIndex = $state<number | null>(null);
    let selectedFields = $state<Set<string>>(new Set());
	let editingColumn = $state<{ gridIndex: number; column: GridColumn | null } | null>(null);
	let showColumnEditor = $state(false);


	function generateId(prefix: string): string {
		return `${prefix}_${Date.now()}_${Math.random().toString(36).substr(2, 6)}`;
	}

	function sortableList(node: HTMLElement, options: { group?: string, onEnd: (evt: any) => void }) {
		const s = new Sortable(node, {
			animation: 150,
			handle: '.drag-handle',
			ghostClass: 'bg-blue-50',
            group: options.group,
			onEnd: options.onEnd
		});
		return {
			destroy() {
				s.destroy();
			}
		};
	}

	function loadDemoData() {
		if (confirm('This will overwrite current fields with a complex demo layout. Continue?')) {
			onChange(JSON.parse(JSON.stringify(complexDemoLibrary)));
		}
	}

	// Field Management
	function handleAddField() {
		editingField = null;
		showFieldEditor = true;
	}

	function handleEditField(field: FormField) {
		editingField = field;
		showFieldEditor = true;
	}

	function handleSaveField(field: FormField) {
		if (editingField) {
			onChange({
				...library,
				fields: library.fields.map((f) => (f.id === editingField!.id ? field : f))
			});
		} else {
			onChange({
				...library,
				fields: [...library.fields, field]
			});
		}
		showFieldEditor = false;
		editingField = null;
	}

	function handleDeleteField(fieldId: string) {
		onChange({
			...library,
			fields: library.fields.filter((f) => f.id !== fieldId)
		});
	}

	function handleDuplicateField(field: FormField) {
		const newField: FormField = {
			...field,
			id: generateId('field'),
			name: field.name + '_copy',
			label: field.label + ' (Copy)'
		};
		onChange({
			...library,
			fields: [...library.fields, newField]
		});
	}

	// Grid Management
	function handleAddGrid() {
		editingGrid = null;
		showGridEditor = true;
	}

	function handleEditGrid(grid: FormGrid) {
		editingGrid = grid;
		showGridEditor = true;
	}

	function handleSaveGrid(grid: FormGrid) {
		if (editingGrid) {
			onChange({
				...library,
				grids: library.grids.map((g) => (g.id === editingGrid!.id ? grid : g))
			});
		} else {
			onChange({
				...library,
				grids: [...library.grids, grid]
			});
		}
		showGridEditor = false;
		editingGrid = null;
	}

	function handleDeleteGrid(gridId: string) {
		onChange({
			...library,
			grids: library.grids.filter((g) => g.id !== gridId)
		});
	}

	// Column Management
	function handleAddColumn(gridIndex: number) {
		editingColumn = { gridIndex, column: null };
		showColumnEditor = true;
	}

	function handleEditColumn(gridIndex: number, column: GridColumn) {
		editingColumn = { gridIndex, column };
		showColumnEditor = true;
	}

	function handleSaveColumn(newColumn: GridColumn) {
		if (!editingColumn) return;

		const grid = library.grids[editingColumn.gridIndex];
		let newColumns: GridColumn[];

		if (editingColumn.column) {
			newColumns = grid.columns.map((c) => (c.id === editingColumn!.column!.id ? newColumn : c));
		} else {
			newColumns = [...grid.columns, newColumn];
		}

		onChange({
			...library,
			grids: library.grids.map((g, i) =>
				i === editingColumn!.gridIndex ? { ...g, columns: newColumns } : g
			)
		});

		showColumnEditor = false;
		editingColumn = null;
	}

	function handleDeleteColumn(gridIndex: number, columnId: string) {
		onChange({
			...library,
			grids: library.grids.map((g, i) =>
				i === gridIndex ? { ...g, columns: g.columns.filter((c) => c.id !== columnId) } : g
			)
		});
	}

    function handleColumnReorder(gridIndex: number, oldIndex: number, newIndex: number) {
        const grid = library.grids[gridIndex];
        const newColumns = [...grid.columns];
        const [moved] = newColumns.splice(oldIndex, 1);
        newColumns.splice(newIndex, 0, moved);

        onChange({
            ...library,
            grids: library.grids.map((g, i) => i === gridIndex ? { ...g, columns: newColumns } : g)
        });
    }

    // Toggle field selection for bulk actions
    function toggleFieldSelection(fieldId: string) {
        if (selectedFields.has(fieldId)) {
            selectedFields.delete(fieldId);
        } else {
            selectedFields.add(fieldId);
        }
        selectedFields = new Set(selectedFields); // Trigger reactivity
    }
</script>

<div class="flex flex-col gap-6">
  <!-- Tabs -->
  <div class="border-b border-gray-200">
    <nav class="-mb-px flex space-x-8" aria-label="Tabs">
      <button
        onclick={() => (activeTab = 'fields')}
        class="border-b-2 py-4 px-1 text-sm font-medium {activeTab === 'fields'
          ? 'border-blue-500 text-blue-600'
          : 'border-transparent text-gray-500 hover:border-gray-300 hover:text-gray-700'}"
      >
        Form Fields
      </button>
      <button
        onclick={() => (activeTab = 'grids')}
        class="border-b-2 py-4 px-1 text-sm font-medium {activeTab === 'grids'
          ? 'border-blue-500 text-blue-600'
          : 'border-transparent text-gray-500 hover:border-gray-300 hover:text-gray-700'}"
      >
        Data Grids
      </button>
    </nav>
  </div>

  {#if activeTab === 'fields'}
    <div class="flex justify-between items-center mb-4">
      <div class="flex gap-2">
           <button
            onclick={loadDemoData}
            class="text-sm text-gray-600 hover:text-blue-600 underline"
            >
            Load Demo Layout
            </button>
            {#if selectedFields.size > 0}
                 <span class="text-sm text-gray-600 border-l border-gray-300 pl-2 ml-2">
                    {selectedFields.size} selected
                 </span>
            {/if}
      </div>
      <button
        onclick={handleAddField}
        class="rounded-md bg-blue-600 px-4 py-2 text-sm font-medium text-white hover:bg-blue-700"
      >
        + Add Field
      </button>
    </div>

    <!-- Fields List -->
    {#if library.fields.length === 0}
      <div class="text-center py-12 rounded-lg border-2 border-dashed border-gray-300">
        <p class="text-sm text-gray-500">No fields defined yet.</p>
        <button
          onclick={handleAddField}
          class="mt-2 text-sm font-medium text-blue-600 hover:text-blue-500"
        >
          Add your first field
        </button>
      </div>
    {:else}
      <div class="overflow-hidden rounded-md border border-gray-200 bg-white">
        <ul class="divide-y divide-gray-200" use:sortableList={{
             onEnd: (evt) => {
				if (evt.oldIndex === undefined || evt.newIndex === undefined) return;
				const newFields = [...library.fields];
				const [moved] = newFields.splice(evt.oldIndex, 1);
				newFields.splice(evt.newIndex, 0, moved);
				onChange({ ...library, fields: newFields });
			}
        }}>
          {#each library.fields as field (field.id)}
            <li class="flex items-center justify-between p-4 hover:bg-gray-50 group">
              <div class="drag-handle mr-3 cursor-move text-gray-400 hover:text-gray-600">
                 <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 8h16M4 16h16"></path></svg>
              </div>
              <div class="mr-2">
                 <input type="checkbox" checked={selectedFields.has(field.id || '')} onchange={() => toggleFieldSelection(field.id || '')} class="rounded border-gray-300 text-blue-600 focus:ring-blue-500" />
              </div>
              <div class="min-w-0 flex-1">
                <div class="flex items-center gap-2">
                  <span class="font-medium text-gray-900">{field.label}</span>
                  {#if field.required}
                    <span class="rounded bg-red-100 px-2 py-0.5 text-xs font-medium text-red-800"
                      >Required</span
                    >
                  {/if}
                  <span class="rounded bg-gray-100 px-2 py-0.5 text-xs text-gray-600"
                    >{field.type}</span
                  >
                </div>
                <div class="mt-1 flex gap-4 text-xs text-gray-500">
                  <span>ID: {field.name}</span>
                  {#if field.placeholder}
                    <span>Placeholder: {field.placeholder}</span>
                  {/if}
                </div>
              </div>
              <div class="ml-4 flex gap-2">
                <button
                  onclick={() => handleEditField(field)}
                  class="text-gray-400 hover:text-blue-600"
                >
                  Edit
                </button>
                <button
                  onclick={() => handleDuplicateField(field)}
                  class="text-gray-400 hover:text-green-600"
                  title="Duplicate Field"
                >
                  Copy
                </button>
                <button
                  onclick={() => {
                    if (confirm('Delete this field?')) {
                      handleDeleteField(field.id || '');
                    }
                  }}
                  class="text-gray-400 hover:text-red-600"
                >
                  Delete
                </button>
              </div>
            </li>
          {/each}
        </ul>
      </div>
    {/if}
  {:else}
    <!-- Grids Tab -->
    <div class="flex justify-end">
      <button
        onclick={handleAddGrid}
        class="rounded-md bg-blue-600 px-4 py-2 text-sm font-medium text-white hover:bg-blue-700"
      >
        + Add Grid
      </button>
    </div>

    {#if library.grids.length === 0}
      <div class="text-center py-12 rounded-lg border-2 border-dashed border-gray-300">
        <p class="text-sm text-gray-500">No data grids defined yet.</p>
        <button
          onclick={handleAddGrid}
          class="mt-2 text-sm font-medium text-blue-600 hover:text-blue-500"
        >
          Add your first grid
        </button>
      </div>
    {:else}
      <div class="space-y-4">
        {#each library.grids as grid, index}
          <div class="overflow-hidden rounded-md border border-gray-200 bg-white">
            <div class="flex items-center justify-between bg-gray-50 p-4">
              <div>
                <h4 class="font-medium text-gray-900">{grid.label}</h4>
                <p class="text-xs text-gray-500">{grid.description || 'No description'}</p>
              </div>
              <div class="flex items-center gap-3">
                <button
                  onclick={() => handleEditGrid(grid)}
                  class="text-sm text-blue-600 hover:text-blue-800"
                >
                  Edit Settings
                </button>
                <button
                  onclick={() => {
                    if (confirm('Delete this grid?')) {
                      handleDeleteGrid(grid.id || '');
                    }
                  }}
                  class="text-sm text-red-600 hover:text-red-800"
                >
                  Delete
                </button>
                <button
                  onclick={() => (expandedGridIndex = expandedGridIndex === index ? null : index)}
                  class="ml-2 text-gray-400"
                >
                  {expandedGridIndex === index ? 'Collapse' : 'Expand Columns'}
                </button>
              </div>
            </div>

            {#if expandedGridIndex === index}
              <div class="border-t border-gray-200 p-4">
                <div class="mb-4 flex items-center justify-between">
                  <h5 class="text-sm font-medium text-gray-700">Columns</h5>
                  <button
                    onclick={() => handleAddColumn(index)}
                    class="text-xs font-medium text-blue-600 hover:text-blue-800"
                  >
                    + Add Column
                  </button>
                </div>

                {#if grid.columns.length === 0}
                  <p class="text-sm text-gray-500 italic">No columns defined.</p>
                {:else}
                  <div class="min-w-full divide-y divide-gray-200">
                    <div class="flex bg-gray-50 font-medium text-xs text-gray-500 uppercase">
                        <div class="px-3 py-2 w-8"></div>
                        <div class="px-3 py-2 flex-1">Label</div>
                        <div class="px-3 py-2 flex-1">Type</div>
                        <div class="px-3 py-2 flex-1">Logic</div>
                        <div class="px-3 py-2 w-32 text-right">Actions</div>
                    </div>
                    <div use:sortableList={{
                        group: `grid-${index}-cols`,
                        onEnd: (evt) => {
                            if (evt.oldIndex !== undefined && evt.newIndex !== undefined) {
                                handleColumnReorder(index, evt.oldIndex, evt.newIndex);
                            }
                        }
                    }}>
                      {#each grid.columns as col (col.id)}
                        <div class="flex items-center hover:bg-gray-50 border-b border-gray-100 last:border-0">
                          <div class="drag-handle px-3 py-2 cursor-move text-gray-400 hover:text-gray-600">
                             <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 8h16M4 16h16"></path></svg>
                          </div>
                          <div class="px-3 py-2 flex-1 text-sm text-gray-900">{col.label}</div>
                          <div class="px-3 py-2 flex-1 text-sm text-gray-500">{col.type}</div>
                          <div class="px-3 py-2 flex-1 text-sm text-gray-500">
                          </div>
                          <div class="px-3 py-2 w-32 text-right text-sm">
                            <button
                              onclick={() => handleEditColumn(index, col)}
                              class="text-blue-600 hover:text-blue-800 mr-2"
                            >
                              Edit
                            </button>
                            <button
                              onclick={() => {
                                if (confirm('Delete this column?')) {
                                  handleDeleteColumn(index, col.id || '');
                                }
                              }}
                              class="text-red-600 hover:text-red-800"
                            >
                              Delete
                            </button>
                          </div>
                        </div>
                      {/each}
                    </div>
                  </div>
                {/if}
              </div>
            {/if}
          </div>
        {/each}
      </div>
    {/if}
  {/if}
</div>

<!-- Modal Dialogs -->
<FieldEditor
    open={showFieldEditor}
    field={editingField}
    library={library}
    onSave={handleSaveField}
    onClose={() => showFieldEditor = false}
/>

<GridEditor
    open={showGridEditor}
    grid={editingGrid}
    onSave={handleSaveGrid}
    onClose={() => showGridEditor = false}
/>

{#if editingColumn}
    <ColumnEditor
        open={showColumnEditor}
        column={editingColumn.column}
        library={library}
        onSave={handleSaveColumn}
        onClose={() => showColumnEditor = false}
    />
{/if}
