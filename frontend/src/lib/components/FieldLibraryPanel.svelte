<script lang="ts">
	import type { FormField, FormGrid, GridColumn, ProcessFieldLibrary } from '$lib/types';
	import Modal from './Modal.svelte';

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
		options: [] as { value: string; label: string }[]
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
		{ value: 'file', label: 'File Upload' }
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
			options: []
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
			options: field.options || []
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
			validation: null,
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
		const grid = library.grids[gridIndex];
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

<div class="border rounded-lg bg-white">
	<div class="p-4 border-b bg-gray-50">
		<h3 class="text-lg font-semibold text-gray-900">Field Library</h3>
		<p class="text-sm text-gray-500 mt-1">
			Define fields and grids once, use them across all tasks in this process.
		</p>
	</div>

	<!-- Tabs -->
	<div class="border-b">
		<div class="flex">
			<button
				type="button"
				onclick={() => (activeTab = 'fields')}
				class="px-6 py-3 text-sm font-medium border-b-2 -mb-px {activeTab === 'fields'
					? 'border-blue-500 text-blue-600'
					: 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'}"
			>
				Fields ({library.fields.length})
			</button>
			<button
				type="button"
				onclick={() => (activeTab = 'grids')}
				class="px-6 py-3 text-sm font-medium border-b-2 -mb-px {activeTab === 'grids'
					? 'border-blue-500 text-blue-600'
					: 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'}"
			>
				Grids ({library.grids.length})
			</button>
		</div>
	</div>

	<!-- Fields Tab -->
	{#if activeTab === 'fields'}
		<div class="p-4">
			<div class="flex justify-end mb-4">
				<button
					type="button"
					onclick={handleAddField}
					class="flex items-center gap-2 px-4 py-2 bg-blue-600 text-white rounded-md hover:bg-blue-700"
				>
					<svg class="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
						<path
							stroke-linecap="round"
							stroke-linejoin="round"
							stroke-width="2"
							d="M12 4v16m8-8H4"
						/>
					</svg>
					Add Field
				</button>
			</div>

			{#if library.fields.length === 0}
				<div class="text-center py-8 text-gray-500">
					<svg
						class="w-12 h-12 mx-auto mb-4 text-gray-300"
						fill="none"
						viewBox="0 0 24 24"
						stroke="currentColor"
					>
						<path
							stroke-linecap="round"
							stroke-linejoin="round"
							stroke-width="2"
							d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"
						/>
					</svg>
					<p>No fields defined yet.</p>
					<p class="text-sm mt-1">Add fields to build your form library.</p>
				</div>
			{:else}
				<div class="space-y-2">
					{#each library.fields as field}
						<div
							class="flex items-center justify-between p-3 border rounded-lg hover:bg-gray-50"
						>
							<div class="flex-1 min-w-0">
								<div class="flex items-center gap-3">
									<span class="font-medium text-gray-900">{field.label || field.name}</span>
									<span class="px-2 py-0.5 text-xs rounded-full bg-gray-100 text-gray-600">
										{getFieldTypeLabel(field.type)}
									</span>
									{#if field.required}
										<span class="px-2 py-0.5 text-xs rounded-full bg-red-100 text-red-600">
											Required
										</span>
									{/if}
									{#if field.readonly}
										<span class="px-2 py-0.5 text-xs rounded-full bg-yellow-100 text-yellow-600">
											Read-only
										</span>
									{/if}
								</div>
								<p class="text-sm text-gray-500 mt-0.5">
									Name: <code class="bg-gray-100 px-1 rounded">{field.name}</code>
								</p>
							</div>
							<div class="flex items-center gap-2">
								<button
									type="button"
									onclick={() => handleDuplicateField(field)}
									class="p-2 text-gray-400 hover:text-gray-600 rounded"
									title="Duplicate"
								>
									<svg class="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
										<path
											stroke-linecap="round"
											stroke-linejoin="round"
											stroke-width="2"
											d="M8 16H6a2 2 0 01-2-2V6a2 2 0 012-2h8a2 2 0 012 2v2m-6 12h8a2 2 0 002-2v-8a2 2 0 00-2-2h-8a2 2 0 00-2 2v8a2 2 0 002 2z"
										/>
									</svg>
								</button>
								<button
									type="button"
									onclick={() => handleEditField(field)}
									class="p-2 text-gray-400 hover:text-blue-600 rounded"
									title="Edit"
								>
									<svg class="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
										<path
											stroke-linecap="round"
											stroke-linejoin="round"
											stroke-width="2"
											d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z"
										/>
									</svg>
								</button>
								<button
									type="button"
									onclick={() => handleDeleteField(field.id)}
									class="p-2 text-gray-400 hover:text-red-600 rounded"
									title="Delete"
								>
									<svg class="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
										<path
											stroke-linecap="round"
											stroke-linejoin="round"
											stroke-width="2"
											d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16"
										/>
									</svg>
								</button>
							</div>
						</div>
					{/each}
				</div>
			{/if}
		</div>
	{/if}

	<!-- Grids Tab -->
	{#if activeTab === 'grids'}
		<div class="p-4">
			<div class="flex justify-end mb-4">
				<button
					type="button"
					onclick={handleAddGrid}
					class="flex items-center gap-2 px-4 py-2 bg-blue-600 text-white rounded-md hover:bg-blue-700"
				>
					<svg class="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
						<path
							stroke-linecap="round"
							stroke-linejoin="round"
							stroke-width="2"
							d="M12 4v16m8-8H4"
						/>
					</svg>
					Add Grid
				</button>
			</div>

			{#if library.grids.length === 0}
				<div class="text-center py-8 text-gray-500">
					<svg
						class="w-12 h-12 mx-auto mb-4 text-gray-300"
						fill="none"
						viewBox="0 0 24 24"
						stroke="currentColor"
					>
						<path
							stroke-linecap="round"
							stroke-linejoin="round"
							stroke-width="2"
							d="M3 10h18M3 14h18m-9-4v8m-7 0h14a2 2 0 002-2V8a2 2 0 00-2-2H5a2 2 0 00-2 2v8a2 2 0 002 2z"
						/>
					</svg>
					<p>No grids defined yet.</p>
					<p class="text-sm mt-1">Add grids for tabular data entry.</p>
				</div>
			{:else}
				<div class="space-y-3">
					{#each library.grids as grid, gridIndex}
						<div class="border rounded-lg">
							<div
								class="flex items-center justify-between p-3 bg-gray-50 rounded-t-lg cursor-pointer"
								onclick={() =>
									(expandedGridIndex = expandedGridIndex === gridIndex ? null : gridIndex)}
							>
								<div class="flex items-center gap-3">
									<svg
										class="w-5 h-5 text-gray-400 transition-transform {expandedGridIndex ===
										gridIndex
											? 'rotate-90'
											: ''}"
										fill="none"
										viewBox="0 0 24 24"
										stroke="currentColor"
									>
										<path
											stroke-linecap="round"
											stroke-linejoin="round"
											stroke-width="2"
											d="M9 5l7 7-7 7"
										/>
									</svg>
									<span class="font-medium text-gray-900">{grid.label || grid.name}</span>
									<span class="px-2 py-0.5 text-xs rounded-full bg-blue-100 text-blue-600">
										{grid.columns.length} columns
									</span>
								</div>
								<div class="flex items-center gap-2">
									<button
										type="button"
										onclick|stopPropagation={() => handleEditGrid(grid)}
										class="p-2 text-gray-400 hover:text-blue-600 rounded"
										title="Edit Grid"
									>
										<svg class="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
											<path
												stroke-linecap="round"
												stroke-linejoin="round"
												stroke-width="2"
												d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z"
											/>
										</svg>
									</button>
									<button
										type="button"
										onclick|stopPropagation={() => handleDeleteGrid(grid.id)}
										class="p-2 text-gray-400 hover:text-red-600 rounded"
										title="Delete Grid"
									>
										<svg class="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
											<path
												stroke-linecap="round"
												stroke-linejoin="round"
												stroke-width="2"
												d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16"
											/>
										</svg>
									</button>
								</div>
							</div>

							{#if expandedGridIndex === gridIndex}
								<div class="p-3 border-t">
									<div class="flex justify-between items-center mb-3">
										<span class="text-sm font-medium text-gray-700">Columns</span>
										<button
											type="button"
											onclick={() => handleAddColumn(gridIndex)}
											class="text-sm text-blue-600 hover:text-blue-700"
										>
											+ Add Column
										</button>
									</div>

									{#if grid.columns.length === 0}
										<p class="text-sm text-gray-500 italic">No columns defined</p>
									{:else}
										<div class="space-y-2">
											{#each grid.columns as column}
												<div
													class="flex items-center justify-between p-2 bg-gray-50 rounded"
												>
													<div class="flex items-center gap-2">
														<span class="text-sm font-medium">{column.label}</span>
														<span
															class="px-2 py-0.5 text-xs rounded bg-gray-200 text-gray-600"
														>
															{getColumnTypeLabel(column.type)}
														</span>
														{#if column.required}
															<span class="text-xs text-red-500">*</span>
														{/if}
													</div>
													<div class="flex items-center gap-1">
														<button
															type="button"
															onclick={() => handleEditColumn(gridIndex, column)}
															class="p-1 text-gray-400 hover:text-blue-600"
														>
															<svg
																class="w-4 h-4"
																fill="none"
																viewBox="0 0 24 24"
																stroke="currentColor"
															>
																<path
																	stroke-linecap="round"
																	stroke-linejoin="round"
																	stroke-width="2"
																	d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z"
																/>
															</svg>
														</button>
														<button
															type="button"
															onclick={() => handleDeleteColumn(gridIndex, column.id)}
															class="p-1 text-gray-400 hover:text-red-600"
														>
															<svg
																class="w-4 h-4"
																fill="none"
																viewBox="0 0 24 24"
																stroke="currentColor"
															>
																<path
																	stroke-linecap="round"
																	stroke-linejoin="round"
																	stroke-width="2"
																	d="M6 18L18 6M6 6l12 12"
																/>
															</svg>
														</button>
													</div>
												</div>
											{/each}
										</div>
									{/if}
								</div>
							{/if}
						</div>
					{/each}
				</div>
			{/if}
		</div>
	{/if}
</div>

<!-- Field Editor Modal -->
<Modal
	open={showFieldEditor}
	title={editingField ? 'Edit Field' : 'Add Field'}
	onClose={() => (showFieldEditor = false)}
	maxWidth="lg"
>
	<div class="space-y-4">
		<div class="grid grid-cols-2 gap-4">
			<div>
				<label for="fieldLabel" class="block text-sm font-medium text-gray-700 mb-1">Label</label>
				<input
					id="fieldLabel"
					type="text"
					bind:value={fieldForm.label}
					class="w-full px-3 py-2 border border-gray-300 rounded-md focus:ring-2 focus:ring-blue-500"
					placeholder="e.g., Customer Name"
				/>
			</div>
			<div>
				<label for="fieldName" class="block text-sm font-medium text-gray-700 mb-1">Name</label>
				<input
					id="fieldName"
					type="text"
					bind:value={fieldForm.name}
					class="w-full px-3 py-2 border border-gray-300 rounded-md focus:ring-2 focus:ring-blue-500"
					placeholder="e.g., customerName"
				/>
				<p class="text-xs text-gray-500 mt-1">Variable name (auto-generated if empty)</p>
			</div>
		</div>

		<div class="grid grid-cols-2 gap-4">
			<div>
				<label for="fieldType" class="block text-sm font-medium text-gray-700 mb-1">Type</label>
				<select
					id="fieldType"
					bind:value={fieldForm.type}
					class="w-full px-3 py-2 border border-gray-300 rounded-md bg-white focus:ring-2 focus:ring-blue-500"
				>
					{#each fieldTypes as type}
						<option value={type.value}>{type.label}</option>
					{/each}
				</select>
			</div>
			<div>
				<label for="fieldPlaceholder" class="block text-sm font-medium text-gray-700 mb-1"
					>Placeholder</label
				>
				<input
					id="fieldPlaceholder"
					type="text"
					bind:value={fieldForm.placeholder}
					class="w-full px-3 py-2 border border-gray-300 rounded-md focus:ring-2 focus:ring-blue-500"
					placeholder="e.g., Enter name..."
				/>
			</div>
		</div>

		{#if fieldForm.type === 'select' || fieldForm.type === 'multiselect' || fieldForm.type === 'radio'}
			<div>
				<label class="block text-sm font-medium text-gray-700 mb-2">Options</label>
				<div class="space-y-2">
					{#each fieldForm.options as option, index}
						<div class="flex items-center gap-2">
							<input
								type="text"
								bind:value={fieldForm.options[index].value}
								class="flex-1 px-3 py-2 border border-gray-300 rounded-md focus:ring-2 focus:ring-blue-500"
								placeholder="Value"
							/>
							<input
								type="text"
								bind:value={fieldForm.options[index].label}
								class="flex-1 px-3 py-2 border border-gray-300 rounded-md focus:ring-2 focus:ring-blue-500"
								placeholder="Label"
							/>
							<button
								type="button"
								onclick={() => removeFieldOption(index)}
								class="p-2 text-red-500 hover:bg-red-50 rounded"
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
						onclick={addFieldOption}
						class="text-sm text-blue-600 hover:text-blue-700"
					>
						+ Add Option
					</button>
				</div>
			</div>
		{/if}

		<div class="flex items-center gap-6">
			<label class="flex items-center gap-2">
				<input type="checkbox" bind:checked={fieldForm.required} class="rounded" />
				<span class="text-sm text-gray-700">Required</span>
			</label>
			<label class="flex items-center gap-2">
				<input type="checkbox" bind:checked={fieldForm.readonly} class="rounded" />
				<span class="text-sm text-gray-700">Read-only</span>
			</label>
			<label class="flex items-center gap-2">
				<input type="checkbox" bind:checked={fieldForm.hidden} class="rounded" />
				<span class="text-sm text-gray-700">Hidden</span>
			</label>
		</div>

		<div>
			<label for="fieldTooltip" class="block text-sm font-medium text-gray-700 mb-1"
				>Tooltip (optional)</label
			>
			<input
				id="fieldTooltip"
				type="text"
				bind:value={fieldForm.tooltip}
				class="w-full px-3 py-2 border border-gray-300 rounded-md focus:ring-2 focus:ring-blue-500"
				placeholder="Help text for users"
			/>
		</div>
	</div>

	{#snippet footer()}
		<button
			type="button"
			onclick={() => (showFieldEditor = false)}
			class="px-4 py-2 text-gray-700 hover:bg-gray-100 rounded-md"
		>
			Cancel
		</button>
		<button
			type="button"
			onclick={handleSaveField}
			class="px-4 py-2 bg-blue-600 text-white rounded-md hover:bg-blue-700"
			disabled={!fieldForm.label}
		>
			{editingField ? 'Save' : 'Add Field'}
		</button>
	{/snippet}
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
				<label class="block text-sm font-medium text-gray-700 mb-2">Options</label>
				<div class="space-y-2">
					{#each columnForm.options as option, index}
						<div class="flex items-center gap-2">
							<input
								type="text"
								bind:value={columnForm.options[index]}
								class="flex-1 px-3 py-2 border border-gray-300 rounded-md focus:ring-2 focus:ring-blue-500"
								placeholder="Option value"
							/>
							<button
								type="button"
								onclick={() => removeColumnOption(index)}
								class="p-2 text-red-500 hover:bg-red-50 rounded"
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
