<script lang="ts">
	import type { FormField, FormGrid, GridColumn, ProcessFieldLibrary } from '$lib/types';
	import Modal from './Modal.svelte';
	import Sortable from 'sortablejs';
	import { complexDemoLibrary } from '$lib/utils/demo-data';

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

	// Field editor state
	let fieldForm = $state({
		id: '',
		name: '',
		label: '',
		type: 'text' as FormField['type'],
		required: false,
		placeholder: '',
		defaultValue: '',
		tooltip: '',
		readonly: false,
		hidden: false,
		options: [] as { value: string; label: string }[],
		validation: {
			minLength: null as number | null,
			maxLength: null as number | null,
			min: null as number | null,
			max: null as number | null,
			pattern: '',
			patternMessage: ''
		},
		logic: {
			type: 'None' as 'None' | 'JS' | 'SQL',
			content: '',
			dependencies: [] as string[],
			autoCalculate: false
		}
	});

	// Grid editor state
	let gridForm = $state({
		id: '',
		name: '',
		label: '',
		description: '',
		minRows: 0,
		maxRows: 10
	});

	// Column editor state
	let editingColumn = $state<{ gridIndex: number; column: GridColumn } | null>(null);
	let showColumnEditor = $state(false);
	let columnForm = $state({
		id: '',
		name: '',
		label: '',
		type: 'text' as GridColumn['type'],
		required: false,
		placeholder: '',
		options: [] as string[]
	});

	const fieldTypes = [
		{ value: 'text', label: 'Text' },
		{ value: 'number', label: 'Number' },
		{ value: 'email', label: 'Email' },
		{ value: 'phone', label: 'Phone' },
		{ value: 'date', label: 'Date' },
		{ value: 'datetime', label: 'Date & Time' },
		{ value: 'select', label: 'Select' },
		{ value: 'multiselect', label: 'Multi-Select' },
		{ value: 'textarea', label: 'Text Area' },
		{ value: 'checkbox', label: 'Checkbox' },
		{ value: 'radio', label: 'Radio Buttons' },
		{ value: 'currency', label: 'Currency' },
		{ value: 'percentage', label: 'Percentage' },
		{ value: 'currency', label: 'Currency' },
		{ value: 'percentage', label: 'Percentage' },
		{ value: 'file', label: 'File Upload' },
		{ value: 'header', label: 'Section Header' }
	];

	const columnTypes = [
		{ value: 'text', label: 'Text' },
		{ value: 'number', label: 'Number' },
		{ value: 'date', label: 'Date' },
		{ value: 'select', label: 'Select' },
		{ value: 'textarea', label: 'Text Area' }
	];

	function generateId(prefix: string): string {
		return `${prefix}_${Date.now()}_${Math.random().toString(36).substr(2, 6)}`;
	}

	function sortableList(node: HTMLElement) {
		const s = new Sortable(node, {
			animation: 150,
			handle: '.drag-handle',
			ghostClass: 'bg-blue-50',
			onEnd: (evt) => {
				if (evt.oldIndex === undefined || evt.newIndex === undefined) return;
				const newFields = [...library.fields];
				const [moved] = newFields.splice(evt.oldIndex, 1);
				newFields.splice(evt.newIndex, 0, moved);
				onChange({ ...library, fields: newFields });
			}
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
		fieldForm = {
			id: generateId('field'),
			name: '',
			label: '',
			type: 'text',
			required: false,
			placeholder: '',
			defaultValue: '',
			tooltip: '',
			readonly: false,
			hidden: false,
			options: [],
			validation: {
				minLength: null,
				maxLength: null,
				min: null,
				max: null,
				pattern: '',
				patternMessage: ''
			},
			logic: {
				type: 'None',
				content: '',
				dependencies: [],
				autoCalculate: false
			}
		};
		showFieldEditor = true;
	}

	function handleEditField(field: FormField) {
		editingField = field;
		fieldForm = {
			id: field.id,
			name: field.name,
			label: field.label,
			type: field.type as typeof fieldForm.type,
			required: field.required,
			placeholder: field.placeholder || '',
			defaultValue: field.defaultValue || '',
			tooltip: field.tooltip || '',
			readonly: field.readonly,
			hidden: field.hidden,
			options: field.options || [],
			validation: field.validation ? { ...field.validation } : {
				minLength: null,
				maxLength: null,
				min: null,
				max: null,
				pattern: '',
				patternMessage: ''
			},
			logic: field.logic ? {
				type: field.logic.type || 'None',
				content: field.logic.content || '',
				dependencies: field.logic.dependencies ? [...field.logic.dependencies] : [],
				autoCalculate: field.logic.autoCalculate || false
			} : {
				type: 'None',
				content: '',
				dependencies: [],
				autoCalculate: false
			}
		};
		showFieldEditor = true;
	}

	function handleSaveField() {
		const newField: FormField = {
			id: fieldForm.id,
			name: fieldForm.name || fieldForm.label.toLowerCase().replace(/\s+/g, '_'),
			label: fieldForm.label,
			type: fieldForm.type,
			required: fieldForm.required,
			validation: {
				minLength: fieldForm.validation.minLength || undefined,
				maxLength: fieldForm.validation.maxLength || undefined,
				min: fieldForm.validation.min || undefined,
				max: fieldForm.validation.max || undefined,
				pattern: fieldForm.validation.pattern || undefined,
				patternMessage: fieldForm.validation.patternMessage || undefined
			},
			logic: fieldForm.logic.type !== 'None' ? {
				type: fieldForm.logic.type,
				content: fieldForm.logic.content,
				dependencies: fieldForm.logic.dependencies,
				autoCalculate: fieldForm.logic.autoCalculate
			} : undefined,
			options: fieldForm.type === 'select' || fieldForm.type === 'multiselect' || fieldForm.type === 'radio' ? fieldForm.options : null,
			placeholder: fieldForm.placeholder,
			defaultValue: fieldForm.defaultValue,
			defaultExpression: '',
			tooltip: fieldForm.tooltip,
			readonly: fieldForm.readonly,
			hidden: fieldForm.hidden,
			hiddenExpression: '',
			readonlyExpression: '',
			requiredExpression: '',
			gridColumn: 1,
			gridRow: library.fields.length + 1,
			gridWidth: 1,
			cssClass: '',
			onChange: '',
			onBlur: ''
		};

		if (editingField) {
			onChange({
				...library,
				fields: library.fields.map((f) => (f.id === editingField!.id ? newField : f))
			});
		} else {
			onChange({
				...library,
				fields: [...library.fields, newField]
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
		gridForm = {
			id: generateId('grid'),
			name: '',
			label: '',
			description: '',
			minRows: 0,
			maxRows: 10
		};
		showGridEditor = true;
	}

	function handleEditGrid(grid: FormGrid) {
		editingGrid = grid;
		gridForm = {
			id: grid.id,
			name: grid.name,
			label: grid.label,
			description: grid.description || '',
			minRows: grid.minRows,
			maxRows: grid.maxRows
		};
		showGridEditor = true;
	}

	function handleSaveGrid() {
		const newGrid: FormGrid = {
			id: gridForm.id,
			name: gridForm.name || gridForm.label.toLowerCase().replace(/\s+/g, '_'),
			label: gridForm.label,
			description: gridForm.description,
			minRows: gridForm.minRows,
			maxRows: gridForm.maxRows,
			columns: editingGrid?.columns || [],
			gridColumn: 1,
			gridRow: library.fields.length + library.grids.length + 1,
			gridWidth: 2,
			cssClass: ''
		};

		if (editingGrid) {
			onChange({
				...library,
				grids: library.grids.map((g) => (g.id === editingGrid!.id ? newGrid : g))
			});
		} else {
			onChange({
				...library,
				grids: [...library.grids, newGrid]
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
		editingColumn = null;
		columnForm = {
			id: generateId('col'),
			name: '',
			label: '',
			type: 'text',
			required: false,
			placeholder: '',
			options: []
		};
		editingColumn = { gridIndex, column: null as unknown as GridColumn };
		showColumnEditor = true;
	}

	function handleEditColumn(gridIndex: number, column: GridColumn) {
		editingColumn = { gridIndex, column };
		columnForm = {
			id: column.id,
			name: column.name,
			label: column.label,
			type: column.type as typeof columnForm.type,
			required: column.required,
			placeholder: column.placeholder || '',
			options: column.options || []
		};
		showColumnEditor = true;
	}

	function handleSaveColumn() {
		if (!editingColumn) return;

		const newColumn: GridColumn = {
			id: columnForm.id,
			name: columnForm.name || columnForm.label.toLowerCase().replace(/\s+/g, '_'),
			label: columnForm.label,
			type: columnForm.type,
			required: columnForm.required,
			placeholder: columnForm.placeholder,
			options: columnForm.type === 'select' ? columnForm.options : null,
			validation: null
		};

		const grid = library.grids[editingColumn.gridIndex];
		let newColumns: GridColumn[];

		if (editingColumn.column) {
			newColumns = grid.columns.map((c) => (c.id === editingColumn!.column.id ? newColumn : c));
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

	function addFieldOption() {
		fieldForm.options = [...fieldForm.options, { value: '', label: '' }];
	}

	function removeFieldOption(index: number) {
		fieldForm.options = fieldForm.options.filter((_, i) => i !== index);
	}

	function addColumnOption() {
		columnForm.options = [...columnForm.options, ''];
	}

	function removeColumnOption(index: number) {
		columnForm.options = columnForm.options.filter((_, i) => i !== index);
	}

	function getFieldTypeLabel(type: string): string {
		return fieldTypes.find((t) => t.value === type)?.label || type;
	}

	function getColumnTypeLabel(type: string): string {
		return columnTypes.find((t) => t.value === type)?.label || type;
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
      <button
        onclick={loadDemoData}
        class="text-sm text-gray-600 hover:text-blue-600 underline"
      >
        Load Demo Layout
      </button>
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
        <ul class="divide-y divide-gray-200" use:sortableList>
          {#each library.fields as field (field.id)}
            <li class="flex items-center justify-between p-4 hover:bg-gray-50 group">
              <div class="drag-handle mr-3 cursor-move text-gray-400 hover:text-gray-600">
                 <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 8h16M4 16h16"></path></svg>
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
                  {#if field.logic && field.logic.type !== 'None'}
                     <span class="rounded bg-indigo-100 px-2 py-0.5 text-xs font-medium text-indigo-800"
                      >Logic: {field.logic.type}</span
                    >
                  {/if}
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
                  onclick={() => {
                    editingField = field;
                    fieldForm = {
                      ...field,
                      validation: field.validation || {
                        minLength: null,
                        maxLength: null,
                        min: null,
                        max: null,
                        pattern: '',
                        patternMessage: ''
                      },
                      logic: field.logic || {
                        type: 'None',
                        content: '',
                        dependencies: [],
                        autoCalculate: false
                      }
                    };
                    showFieldEditor = true;
                  }}
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
                      handleDeleteField(field.id);
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
                      handleDeleteGrid(grid.id);
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
                  <table class="min-w-full divide-y divide-gray-200">
                    <thead>
                      <tr>
                        <th class="px-3 py-2 text-left text-xs font-medium text-gray-500 uppercase"
                          >Label</th
                        >
                        <th class="px-3 py-2 text-left text-xs font-medium text-gray-500 uppercase"
                          >Type</th
                        >
                        <th class="px-3 py-2 text-left text-xs font-medium text-gray-500 uppercase"
                          >Logic</th
                        >
                        <th class="px-3 py-2 text-right text-xs font-medium text-gray-500 uppercase"
                          >Actions</th
                        >
                      </tr>
                    </thead>
                    <tbody class="divide-y divide-gray-200">
                      {#each grid.columns as col}
                        <tr>
                          <td class="px-3 py-2 text-sm text-gray-900">{col.label}</td>
                          <td class="px-3 py-2 text-sm text-gray-500">{col.type}</td>
                          <td class="px-3 py-2 text-sm text-gray-500">
                            {#if col.logic && col.logic.type !== 'None'}
                              <span class="rounded bg-indigo-50 px-1.5 py-0.5 text-xs text-indigo-700"
                                >{col.logic.type}</span
                              >
                            {/if}
                          </td>
                          <td class="px-3 py-2 text-right text-sm">
                            <button
                              onclick={() => handleEditColumn(index, col)}
                              class="text-blue-600 hover:text-blue-800 mr-2"
                            >
                              Edit
                            </button>
                            <button
                              onclick={() => {
                                if (confirm('Delete this column?')) {
                                  handleDeleteColumn(index, col.id);
                                }
                              }}
                              class="text-red-600 hover:text-red-800"
                            >
                              Delete
                            </button>
                          </td>
                        </tr>
                      {/each}
                    </tbody>
                  </table>
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
<Modal open={showFieldEditor} title={editingField ? 'Edit Field' : 'Add Field'} onClose={() => (showFieldEditor = false)} maxWidth="lg">
  <div class="space-y-6 max-h-[70vh] overflow-y-auto px-1">
    <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
      <div>
        <label for="f_name" class="block text-sm font-medium text-gray-700">Field ID (Name)</label>
        <input
          id="f_name"
          type="text"
          bind:value={fieldForm.name}
          class="mt-1 block w-full rounded-md border-gray-300 shadow-sm focus:border-blue-500 focus:ring-blue-500 sm:text-sm"
          placeholder="e.g. customerName"
        />
      </div>
      <div>
        <label for="f_label" class="block text-sm font-medium text-gray-700">Label</label>
        <input
          id="f_label"
          type="text"
          bind:value={fieldForm.label}
          class="mt-1 block w-full rounded-md border-gray-300 shadow-sm focus:border-blue-500 focus:ring-blue-500 sm:text-sm"
          placeholder="e.g. Customer Name"
        />
      </div>
      <div>
        <label for="f_type" class="block text-sm font-medium text-gray-700">Type</label>
        <select
          id="f_type"
          bind:value={fieldForm.type}
          class="mt-1 block w-full rounded-md border-gray-300 shadow-sm focus:border-blue-500 focus:ring-blue-500 sm:text-sm"
        >
          {#each fieldTypes as type}
            <option value={type.value}>{type.label}</option>
          {/each}
        </select>
      </div>
      <div>
        <label for="f_placeholder" class="block text-sm font-medium text-gray-700"
          >Placeholder</label
        >
        <input
          id="f_placeholder"
          type="text"
          bind:value={fieldForm.placeholder}
          class="mt-1 block w-full rounded-md border-gray-300 shadow-sm focus:border-blue-500 focus:ring-blue-500 sm:text-sm"
        />
      </div>
    </div>

    <!-- Conditional Logic & Visibility -->
    <div class="border rounded-md p-4 bg-indigo-50 border-indigo-100">
      <h4 class="text-sm font-medium text-indigo-900 mb-3">Conditional Logic</h4>
      
      <div class="space-y-4">
        <div>
           <label for="f_logic_type" class="block text-sm font-medium text-indigo-800">Logic Type</label>
           <select
             id="f_logic_type"
             bind:value={fieldForm.logic.type}
             class="mt-1 block w-full rounded-md border-indigo-300 shadow-sm focus:border-indigo-500 focus:ring-indigo-500 sm:text-sm bg-white"
           >
             <option value="None">None</option>
             <option value="Visibility">Visibility Rule (Show/Hide)</option>
             <option value="Dependency">Content Dependency</option>
           </select>
        </div>

        {#if fieldForm.logic.type !== 'None'}
          <div>
            <label for="f_logic_deps" class="block text-sm font-medium text-indigo-800">Dependent Fields (IDs)</label>
            <div class="text-xs text-indigo-600 mb-1">Select fields that trigger this logic</div>
             <select
               multiple
               id="f_logic_deps"
               bind:value={fieldForm.logic.dependencies}
               class="mt-1 block w-full rounded-md border-indigo-300 shadow-sm focus:border-indigo-500 focus:ring-indigo-500 sm:text-sm h-32"
             >
                <!-- Filter out self to prevent circular dependency (basic check) -->
               {#each library.fields.filter(f => f.id !== fieldForm.id) as f}
                 <option value={f.name}>{f.label} ({f.name})</option>
               {/each}
             </select>
             <div class="text-xs mt-1 text-gray-500">Hold Ctrl/Cmd to select multiple</div>
          </div>

          <div>
            <label for="f_logic_content" class="block text-sm font-medium text-indigo-800">Expression (JavaScript)</label>
            <div class="text-xs text-indigo-600 mb-1">
              {#if fieldForm.logic.type === 'Visibility'}
                Return true to show, false to hide. ex: <code class="bg-white px-1 rounded">projectType === 'client'</code>
              {:else}
                Return valid value. ex: <code class="bg-white px-1 rounded">department === 'it' ? 'Tech' : 'Gen'</code>
              {/if}
            </div>
            <textarea
              id="f_logic_content"
              bind:value={fieldForm.logic.content}
              rows="3"
              class="mt-1 block w-full rounded-md border-indigo-300 shadow-sm focus:border-indigo-500 focus:ring-indigo-500 sm:text-sm font-mono"
              placeholder="e.g. projectType === 'client' && amount > 1000"
            ></textarea>
          </div>
        {/if}
      </div>
    </div>

    <!-- Options for Choice Types (Select, Radio, etc.) -->
    {#if ['select', 'multiselect', 'radio'].includes(fieldForm.type)}
      <div class="border rounded-md p-4 bg-gray-50">
        <span class="block text-sm font-medium text-gray-700 mb-2">Options</span>
        {#each fieldForm.options as option, i}
          <div class="flex gap-2 mb-2">
            <input
              type="text"
              bind:value={option.label}
              placeholder="Label"
              aria-label="Option label"
              class="flex-1 rounded-md border-gray-300 shadow-sm focus:border-blue-500 focus:ring-blue-500 sm:text-sm"
            />
            <input
              type="text"
              bind:value={option.value}
              placeholder="Value"
              aria-label="Option value"
              class="flex-1 rounded-md border-gray-300 shadow-sm focus:border-blue-500 focus:ring-blue-500 sm:text-sm"
            />
            <button
              onclick={() => removeFieldOption(i)}
              class="text-red-600 hover:text-red-800 text-sm"
            >
              ×
            </button>
          </div>
        {/each}
        <button
          onclick={addFieldOption}
          class="text-sm text-blue-600 hover:text-blue-800"
        >
          + Add Option
        </button>
      </div>
    {/if}

    <!-- Validation Rules -->
    <div class="border rounded-md p-4 bg-gray-50">
      <h4 class="text-sm font-medium text-gray-900 mb-3">Validation Rules</h4>
      <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
        <div class="flex items-center">
          <input
            id="f_required"
            type="checkbox"
            bind:checked={fieldForm.required}
            class="h-4 w-4 rounded border-gray-300 text-blue-600 focus:ring-blue-500"
          />
          <label for="f_required" class="ml-2 block text-sm text-gray-900">Required Field</label>
        </div>

        {#if ['text', 'textarea', 'email', 'password'].includes(fieldForm.type)}
          <div>
            <label for="f_minlen" class="block text-xs font-medium text-gray-500">Min Length</label>
            <input
              id="f_minlen"
              type="number"
              bind:value={fieldForm.validation.minLength}
              class="mt-1 block w-full rounded-md border-gray-300 shadow-sm focus:border-blue-500 focus:ring-blue-500 sm:text-sm"
            />
          </div>
          <div>
            <label for="f_maxlen" class="block text-xs font-medium text-gray-500">Max Length</label>
            <input
              id="f_maxlen"
              type="number"
              bind:value={fieldForm.validation.maxLength}
              class="mt-1 block w-full rounded-md border-gray-300 shadow-sm focus:border-blue-500 focus:ring-blue-500 sm:text-sm"
            />
          </div>
          <div class="col-span-2">
            <label for="f_pattern" class="block text-xs font-medium text-gray-500"
              >Regex Pattern</label
            >
            <input
              id="f_pattern"
              type="text"
              bind:value={fieldForm.validation.pattern}
              placeholder="e.g. ^[A-Z]+$"
              class="mt-1 block w-full rounded-md border-gray-300 shadow-sm focus:border-blue-500 focus:ring-blue-500 sm:text-sm"
            />
          </div>
          <div class="col-span-2">
            <label for="f_patmsg" class="block text-xs font-medium text-gray-500"
              >Pattern Error Message</label
            >
            <input
              id="f_patmsg"
              type="text"
              bind:value={fieldForm.validation.patternMessage}
              class="mt-1 block w-full rounded-md border-gray-300 shadow-sm focus:border-blue-500 focus:ring-blue-500 sm:text-sm"
            />
          </div>
        {/if}

        {#if ['number', 'currency', 'percentage', 'date', 'datetime'].includes(fieldForm.type)}
          <div>
            <label for="f_min" class="block text-xs font-medium text-gray-500">Minimum Value</label>
            <input
              id="f_min"
              type="number"
              bind:value={fieldForm.validation.min}
              class="mt-1 block w-full rounded-md border-gray-300 shadow-sm focus:border-blue-500 focus:ring-blue-500 sm:text-sm"
            />
          </div>
          <div>
            <label for="f_max" class="block text-xs font-medium text-gray-500">Maximum Value</label>
            <input
              id="f_max"
              type="number"
              bind:value={fieldForm.validation.max}
              class="mt-1 block w-full rounded-md border-gray-300 shadow-sm focus:border-blue-500 focus:ring-blue-500 sm:text-sm"
            />
          </div>
        {/if}
      </div>
    </div>

    <!-- Logic & Dependencies -->
    <div class="border rounded-md p-4 bg-indigo-50 border-indigo-100">
      <h4 class="text-sm font-medium text-indigo-900 mb-3">Logic & Dependencies</h4>
      <div class="space-y-4">
        <div>
          <label for="f_logictype" class="block text-sm font-medium text-gray-700">Logic Type</label
          >
          <select
            id="f_logictype"
            bind:value={fieldForm.logic.type}
            class="mt-1 block w-full rounded-md border-gray-300 shadow-sm focus:border-blue-500 focus:ring-blue-500 sm:text-sm"
          >
            <option value="None">None</option>
            <option value="JS">JavaScript Code</option>
            <option value="SQL">SQL Query</option>
          </select>
        </div>

        {#if fieldForm.logic.type !== 'None'}
          <div>
            <label for="f_deps" class="block text-sm font-medium text-gray-700"
              >Dependencies (Field Names)</label
            >
            <select
              id="f_deps"
              multiple
              bind:value={fieldForm.logic.dependencies}
              class="mt-1 block w-full rounded-md border-gray-300 shadow-sm focus:border-blue-500 focus:ring-blue-500 sm:text-sm"
              size="3"
            >
              {#each library.fields as f}
                {#if f.name !== fieldForm.name}
                  <option value={f.name}>{f.label} ({f.name})</option>
                {/if}
              {/each}
            </select>
            <p class="text-xs text-gray-500 mt-1">Hold Ctrl/Cmd to select multiple.</p>
          </div>

          <div>
            <label for="f_logiccontent" class="block text-sm font-medium text-gray-700">
              {fieldForm.logic.type === 'JS' ? 'JavaScript Code' : 'SQL Query'}
            </label>
            <textarea
              id="f_logiccontent"
              bind:value={fieldForm.logic.content}
              rows="5"
              class="mt-1 block w-full font-mono text-sm rounded-md border-gray-300 shadow-sm focus:border-blue-500 focus:ring-blue-500"
              placeholder={fieldForm.logic.type === 'JS'
                ? "return (Number(form.price) || 0) * (Number(form.qty) || 0);"
                : "SELECT name FROM users WHERE id = ${form.userId}"}
            ></textarea>
            <p class="text-xs text-gray-500 mt-1">
              {fieldForm.logic.type === 'JS'
                ? 'Available variables: value, form (values), db (query), lib (functions)'
                : 'Use ${form.fieldName} to inject values'}
            </p>
          </div>

          <div class="flex items-center">
            <input
              id="f_autocalc"
              type="checkbox"
              bind:checked={fieldForm.logic.autoCalculate}
              class="h-4 w-4 rounded border-gray-300 text-blue-600 focus:ring-blue-500"
            />
            <label for="f_autocalc" class="ml-2 block text-sm text-gray-900"
              >Auto-calculate on dependency change</label
            >
          </div>
        {/if}
      </div>
    </div>
    
    <div class="flex items-center space-x-4">
      <div class="flex items-center">
        <input
          id="f_readonly"
          type="checkbox"
          bind:checked={fieldForm.readonly}
          class="h-4 w-4 rounded border-gray-300 text-blue-600 focus:ring-blue-500"
        />
        <label for="f_readonly" class="ml-2 block text-sm text-gray-900">Read-only</label>
      </div>
      <div class="flex items-center">
        <input
          id="f_hidden"
          type="checkbox"
          bind:checked={fieldForm.hidden}
          class="h-4 w-4 rounded border-gray-300 text-blue-600 focus:ring-blue-500"
        />
        <label for="f_hidden" class="ml-2 block text-sm text-gray-900">Hidden</label>
      </div>
    </div>
  </div>

  <div class="mt-5 sm:mt-6 sm:grid sm:grid-flow-row-dense sm:grid-cols-2 sm:gap-3">
    <button
      onclick={handleSaveField}
      class="inline-flex w-full justify-center rounded-md border border-transparent bg-blue-600 px-4 py-2 text-base font-medium text-white shadow-sm hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:ring-offset-2 sm:col-start-2 sm:text-sm"
    >
      Save Field
    </button>
    <button
      onclick={() => (showFieldEditor = false)}
      class="mt-3 inline-flex w-full justify-center rounded-md border border-gray-300 bg-white px-4 py-2 text-base font-medium text-gray-700 shadow-sm hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:ring-offset-2 sm:col-start-1 sm:mt-0 sm:text-sm"
    >
      Cancel
    </button>
  </div>
</Modal>

<!-- Grid Editor Modal -->
<Modal
	open={showGridEditor}
	title={editingGrid ? 'Edit Grid' : 'Add Grid'}
	onClose={() => (showGridEditor = false)}
	maxWidth="md"
>
	<div class="space-y-4">
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
	</div>

	{#snippet footer()}
		<button
			type="button"
			onclick={() => (showGridEditor = false)}
			class="px-4 py-2 text-gray-700 hover:bg-gray-100 rounded-md"
		>
			Cancel
		</button>
		<button
			type="button"
			onclick={handleSaveGrid}
			class="px-4 py-2 bg-blue-600 text-white rounded-md hover:bg-blue-700"
			disabled={!gridForm.label}
		>
			{editingGrid ? 'Save' : 'Add Grid'}
		</button>
	{/snippet}
</Modal>

<!-- Column Editor Modal -->
<Modal
	open={showColumnEditor}
	title={editingColumn?.column ? 'Edit Column' : 'Add Column'}
	onClose={() => (showColumnEditor = false)}
	maxWidth="md"
>
	<div class="space-y-4">
		<div class="grid grid-cols-2 gap-4">
			<div>
				<label for="columnLabel" class="block text-sm font-medium text-gray-700 mb-1">Label</label>
				<input
					id="columnLabel"
					type="text"
					bind:value={columnForm.label}
					class="w-full px-3 py-2 border border-gray-300 rounded-md focus:ring-2 focus:ring-blue-500"
					placeholder="e.g., Product Name"
				/>
			</div>
			<div>
				<label for="columnName" class="block text-sm font-medium text-gray-700 mb-1">Name</label>
				<input
					id="columnName"
					type="text"
					bind:value={columnForm.name}
					class="w-full px-3 py-2 border border-gray-300 rounded-md focus:ring-2 focus:ring-blue-500"
					placeholder="e.g., productName"
				/>
			</div>
		</div>

		<div class="grid grid-cols-2 gap-4">
			<div>
				<label for="columnType" class="block text-sm font-medium text-gray-700 mb-1">Type</label>
				<select
					id="columnType"
					bind:value={columnForm.type}
					class="w-full px-3 py-2 border border-gray-300 rounded-md bg-white focus:ring-2 focus:ring-blue-500"
				>
					{#each columnTypes as type}
						<option value={type.value}>{type.label}</option>
					{/each}
				</select>
			</div>
			<div>
				<label for="columnPlaceholder" class="block text-sm font-medium text-gray-700 mb-1"
					>Placeholder</label
				>
				<input
					id="columnPlaceholder"
					type="text"
					bind:value={columnForm.placeholder}
					class="w-full px-3 py-2 border border-gray-300 rounded-md focus:ring-2 focus:ring-blue-500"
				/>
			</div>
		</div>

		{#if columnForm.type === 'select'}
			<div>
				<span class="block text-sm font-medium text-gray-700 mb-2">Options</span>
				<div class="space-y-2">
					{#each columnForm.options as _option, index}
						<div class="flex items-center gap-2">
							<input
								type="text"
								bind:value={columnForm.options[index]}
								class="flex-1 px-3 py-2 border border-gray-300 rounded-md focus:ring-2 focus:ring-blue-500"
								placeholder="Option value"
								aria-label="Column option value"
							/>
							<button
								type="button"
								onclick={() => removeColumnOption(index)}
								class="p-2 text-red-500 hover:bg-red-50 rounded"
								title="Remove Option"
							>
								<svg class="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
									<path
										stroke-linecap="round"
										stroke-linejoin="round"
										stroke-width="2"
										d="M6 18L18 6M6 6l12 12"
									/>
								</svg>
							</button>
						</div>
					{/each}
					<button
						type="button"
						onclick={addColumnOption}
						class="text-sm text-blue-600 hover:text-blue-700"
					>
						+ Add Option
					</button>
				</div>
			</div>
		{/if}

		<div class="flex items-center gap-2">
			<input type="checkbox" id="columnRequired" bind:checked={columnForm.required} class="rounded" />
			<label for="columnRequired" class="text-sm text-gray-700">Required</label>
		</div>
	</div>

	{#snippet footer()}
		<button
			type="button"
			onclick={() => (showColumnEditor = false)}
			class="px-4 py-2 text-gray-700 hover:bg-gray-100 rounded-md"
		>
			Cancel
		</button>
		<button
			type="button"
			onclick={handleSaveColumn}
			class="px-4 py-2 bg-blue-600 text-white rounded-md hover:bg-blue-700"
			disabled={!columnForm.label}
		>
			{editingColumn?.column ? 'Save' : 'Add Column'}
		</button>
	{/snippet}
</Modal>
