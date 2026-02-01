<script lang="ts">
	import type { GridColumn, ProcessFieldLibrary, FormField, GridDefinition } from '$lib/types';
	import Modal from '../Modal.svelte';
    import CodeEditor from '../CodeEditor.svelte';

	interface Props {
		column: GridColumn | null;
		library: ProcessFieldLibrary;
		onSave: (column: GridColumn) => void;
		onClose: () => void;
		open: boolean;
	}

	const { column, library, onSave, onClose, open }: Props = $props();

	let columnForm = $state({
		id: '',
		name: '',
		label: '',
		type: 'text' as GridColumn['type'],
		required: false,
		placeholder: '',
		options: [] as string[],
		hiddenExpression: '',
		readonlyExpression: '',
		requiredExpression: '',
		calculationExpression: '',
		validation: {
			minLength: undefined as number | undefined,
			maxLength: undefined as number | undefined,
			min: undefined as number | undefined,
			max: undefined as number | undefined,
			pattern: '',
			patternMessage: ''
		}
	});

	const columnTypes = [
		{ value: 'text', label: 'Text' },
		{ value: 'number', label: 'Number' },
		{ value: 'date', label: 'Date' },
		{ value: 'select', label: 'Select' },
		{ value: 'textarea', label: 'Text Area' },
		{ value: 'checkbox', label: 'Checkbox' },
		{ value: 'email', label: 'Email' },
		{ value: 'currency', label: 'Currency' },
		{ value: 'percentage', label: 'Percentage' }
	];

	function generateId(prefix: string): string {
		return `${prefix}_${Date.now()}_${Math.random().toString(36).substr(2, 6)}`;
	}

    function getCodeSuggestions() {
		const suggestions: Array<{ label: string; value: string; type: 'field' | 'grid' | 'column' | 'variable' | 'function'; description?: string }> = [];

		suggestions.push(
			{ label: 'value', value: 'value', type: 'variable', description: 'Current field value' },
			{ label: 'form', value: 'form', type: 'variable', description: 'All form fields' },
			{ label: 'grids', value: 'grids', type: 'variable', description: 'All grids data' },
			{ label: 'row', value: 'row', type: 'variable', description: 'Current grid row' }
		);

		library.fields.forEach((f: FormField) => {
			suggestions.push({ label: `form.${f.name}`, value: `form.${f.name}`, type: 'field', description: f.label });
		});

		library.grids.forEach((g: GridDefinition) => {
			suggestions.push({ label: `grids.${g.name}`, value: `grids.${g.name}`, type: 'grid', description: g.label });
			suggestions.push({ label: `grids.${g.name}.selectedRow`, value: `grids.${g.name}.selectedRow`, type: 'grid', description: 'Selected row' });
			suggestions.push({ label: `grids.${g.name}.selectedRows`, value: `grids.${g.name}.selectedRows`, type: 'grid', description: 'Selected rows (array)' });
			suggestions.push({ label: `grids.${g.name}.selectedRows.length`, value: `grids.${g.name}.selectedRows.length`, type: 'grid', description: 'Count of selected' });
			suggestions.push({ label: `grids.${g.name}.rows`, value: `grids.${g.name}.rows`, type: 'grid', description: 'All rows' });
			suggestions.push({ label: `grids.${g.name}.sum`, value: `grids.${g.name}.sum('')`, type: 'function', description: 'Sum column' });

			g.columns.forEach((c: GridColumn) => {
				suggestions.push({ label: `row.${c.name}`, value: `row.${c.name}`, type: 'column', description: `${g.label} - ${c.label}` });
			});
		});

		return suggestions;
	}

	$effect(() => {
		if (column) {
			columnForm = {
				id: column.id || '',
				name: column.name,
				label: column.label,
				type: column.type as typeof columnForm.type,
				required: column.required || false,
				placeholder: column.placeholder || '',
				options: (column.options || []).map(o => typeof o === 'string' ? o : o.value),
				hiddenExpression: column.hiddenExpression || '',
				readonlyExpression: column.readonlyExpression || '',
				requiredExpression: column.requiredExpression || '',
				calculationExpression: column.calculationExpression || '',
				validation: column.validation ? {
					minLength: column.validation.minLength,
					maxLength: column.validation.maxLength,
					min: column.validation.min,
					max: column.validation.max,
					pattern: column.validation.pattern || '',
					patternMessage: column.validation.patternMessage || ''
				} : {
					minLength: undefined,
					maxLength: undefined,
					min: undefined,
					max: undefined,
					pattern: '',
					patternMessage: ''
				}
			};
		} else {
            columnForm = {
                id: generateId('col'),
                name: '',
                label: '',
                type: 'text',
                required: false,
                placeholder: '',
                options: [],
                hiddenExpression: '',
                readonlyExpression: '',
                requiredExpression: '',
                calculationExpression: '',
                validation: {
                    minLength: undefined,
                    maxLength: undefined,
                    min: undefined,
                    max: undefined,
                    pattern: '',
                    patternMessage: ''
                }
            };
        }
	});

	function handleSave() {

		const newColumn: GridColumn = {
			id: columnForm.id,
			name: columnForm.name || columnForm.label.toLowerCase().replace(/\s+/g, '_'),
			label: columnForm.label,
			type: columnForm.type,
			required: columnForm.required,
			placeholder: columnForm.placeholder,
			options: columnForm.type === 'select' ? columnForm.options : undefined,
			hiddenExpression: columnForm.hiddenExpression,
			readonlyExpression: columnForm.readonlyExpression,
			requiredExpression: columnForm.requiredExpression,
			calculationExpression: columnForm.calculationExpression,
			validation: {
				minLength: columnForm.validation.minLength || undefined,
				maxLength: columnForm.validation.maxLength || undefined,
				min: columnForm.validation.min || undefined,
				max: columnForm.validation.max || undefined,
				pattern: columnForm.validation.pattern || undefined,
				patternMessage: columnForm.validation.patternMessage || undefined
			}
		};
        onSave(newColumn);
	}

	function addColumnOption() {
		columnForm.options = [...columnForm.options, ''];
	}

	function removeColumnOption(index: number) {
		columnForm.options = columnForm.options.filter((_, i) => i !== index);
	}
</script>

<Modal
	open={open}
	title={column ? 'Edit Column' : 'Add Column'}
	onClose={onClose}
	maxWidth="lg"
>
	<div class="space-y-4 max-h-[70vh] overflow-y-auto px-1">
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

		<!-- Validation Rules -->
		<div class="border rounded-md p-4 bg-gray-50">
			<h4 class="text-sm font-medium text-gray-900 mb-3">Validation Rules</h4>
			<div class="grid grid-cols-1 md:grid-cols-2 gap-4">
				<div class="flex items-center">
					<input type="checkbox" id="columnRequired" bind:checked={columnForm.required} class="h-4 w-4 rounded border-gray-300 text-blue-600 focus:ring-blue-500" />
					<label for="columnRequired" class="ml-2 block text-sm text-gray-900">Required</label>
				</div>

				{#if ['text', 'textarea', 'email'].includes(columnForm.type)}
					<div>
						<label for="col_minlen" class="block text-xs font-medium text-gray-500">Min Length</label>
						<input
							id="col_minlen"
							type="number"
							bind:value={columnForm.validation.minLength}
							class="mt-1 block w-full rounded-md border-gray-300 shadow-sm focus:border-blue-500 focus:ring-blue-500 sm:text-sm"
						/>
					</div>
					<div>
						<label for="col_maxlen" class="block text-xs font-medium text-gray-500">Max Length</label>
						<input
							id="col_maxlen"
							type="number"
							bind:value={columnForm.validation.maxLength}
							class="mt-1 block w-full rounded-md border-gray-300 shadow-sm focus:border-blue-500 focus:ring-blue-500 sm:text-sm"
						/>
					</div>
					<div class="col-span-2">
						<label for="col_pattern" class="block text-xs font-medium text-gray-500">Regex Pattern</label>
						<input
							id="col_pattern"
							type="text"
							bind:value={columnForm.validation.pattern}
							placeholder="e.g. ^[A-Z]+$"
							class="mt-1 block w-full rounded-md border-gray-300 shadow-sm focus:border-blue-500 focus:ring-blue-500 sm:text-sm"
						/>
					</div>
					<div class="col-span-2">
						<label for="col_patmsg" class="block text-xs font-medium text-gray-500">Pattern Error Message</label>
						<input
							id="col_patmsg"
							type="text"
							bind:value={columnForm.validation.patternMessage}
							class="mt-1 block w-full rounded-md border-gray-300 shadow-sm focus:border-blue-500 focus:ring-blue-500 sm:text-sm"
						/>
					</div>
				{/if}

				{#if ['number', 'currency', 'percentage', 'date'].includes(columnForm.type)}
					<div>
						<label for="col_min" class="block text-xs font-medium text-gray-500">Minimum Value</label>
						<input
							id="col_min"
							type="number"
							bind:value={columnForm.validation.min}
							class="mt-1 block w-full rounded-md border-gray-300 shadow-sm focus:border-blue-500 focus:ring-blue-500 sm:text-sm"
						/>
					</div>
					<div>
						<label for="col_max" class="block text-xs font-medium text-gray-500">Maximum Value</label>
						<input
							id="col_max"
							type="number"
							bind:value={columnForm.validation.max}
							class="mt-1 block w-full rounded-md border-gray-300 shadow-sm focus:border-blue-500 focus:ring-blue-500 sm:text-sm"
						/>
					</div>
				{/if}
			</div>
		</div>

		<!-- Advanced Behavior (Expressions) -->
		<div class="border rounded-md p-4 bg-purple-50 border-purple-100 mb-4">
			<h4 class="text-sm font-medium text-purple-900 mb-3">Dynamic Behavior (Expressions)</h4>
			<div class="grid grid-cols-1 md:grid-cols-2 gap-4">
				<div>
					<label for="colHiddenExpr" class="block text-xs font-medium text-gray-600">Hidden Expr.</label>
					<input
						id="colHiddenExpr"
						type="text"
						bind:value={columnForm.hiddenExpression}
						placeholder="!row.showCol"
						class="mt-1 w-full px-3 py-2 border border-gray-300 rounded-md focus:ring-2 focus:ring-purple-500 font-mono text-xs"
					/>
				</div>
				<div>
					<label for="colReadonlyExpr" class="block text-xs font-medium text-gray-600">Readonly Expr.</label>
					<input
						id="colReadonlyExpr"
						type="text"
						bind:value={columnForm.readonlyExpression}
						placeholder="row.readonly"
						class="mt-1 w-full px-3 py-2 border border-gray-300 rounded-md focus:ring-2 focus:ring-purple-500 font-mono text-xs"
					/>
				</div>
				<div>
					<label for="colRequiredExpr" class="block text-xs font-medium text-gray-600">Required Expr.</label>
					<input
						id="colRequiredExpr"
						type="text"
						bind:value={columnForm.requiredExpression}
						placeholder="row.required"
						class="mt-1 w-full px-3 py-2 border border-gray-300 rounded-md focus:ring-2 focus:ring-purple-500 font-mono text-xs"
					/>
				</div>
				<div>
					<label for="colCalcExpr" class="block text-xs font-medium text-gray-600">Calculation Expr.</label>
					<input
						id="colCalcExpr"
						type="text"
						bind:value={columnForm.calculationExpression}
						placeholder="row.price * row.qty"
						class="mt-1 w-full px-3 py-2 border border-gray-300 rounded-md focus:ring-2 focus:ring-purple-500 font-mono text-xs"
					/>
				</div>
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
			disabled={!columnForm.label}
		>
			{column ? 'Save' : 'Add Column'}
		</button>
	{/snippet}
</Modal>
