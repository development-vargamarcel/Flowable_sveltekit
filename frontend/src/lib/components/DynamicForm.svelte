<script lang="ts">
	import { untrack } from 'svelte';
	import type { FormField, FormGrid, GridConfig, FieldConditionRule, ComputedFieldState, ComputedGridState } from '$lib/types';
	import type { UserContext, EvaluationContext } from '$lib/utils/expression-evaluator';
	import { ConditionStateComputer } from '$lib/utils/condition-state-computer';
	import DynamicGrid from './DynamicGrid.svelte';

	interface Props {
		fields: FormField[];
		grids: FormGrid[];
		gridConfig: GridConfig;
		values?: Record<string, unknown>;
		errors?: Record<string, string>;
		readonly?: boolean;
		onValuesChange?: (values: Record<string, unknown>) => void;
		// Condition evaluation props
		conditionRules?: FieldConditionRule[];
		taskConditionRules?: FieldConditionRule[];
		processVariables?: Record<string, unknown>;
		userContext?: UserContext;
	}

	const {
		fields,
		grids,
		gridConfig,
		values = {},
		errors = {},
		readonly = false,
		onValuesChange,
		conditionRules = [],
		taskConditionRules = [],
		processVariables = {},
		userContext = { id: '', username: '', roles: [], groups: [] }
	}: Props = $props();

	// Local form values - initialized in $effect
	let formValues = $state<Record<string, unknown>>({});
	let fieldErrors = $state<Record<string, string>>({});
	let gridSelections = $state<Record<string, Record<string, unknown>[]>>({});
	let formInitialized = $state(false);
	let userHasMadeChanges = $state(false);

	// Grid component references for validation
	const gridRefs: Record<string, DynamicGrid> = {};

	// Create grids context for logic execution
	const gridsContext = $derived(
		Object.fromEntries(
			grids.map((g) => [
				g.name,
				{
					rows: tryParseJson(formValues[g.name]),
					selectedRows: gridSelections[g.name] || [],
					selectedRow: (gridSelections[g.name] || [])[0] || null,
					sum: (col: string) => {
						const rows = tryParseJson(formValues[g.name]);
						return rows.reduce((sum, row) => sum + (Number(row[col]) || 0), 0);
					}
				}
			])
		)
	);

	// Computed field and grid states based on condition rules
	let computedFieldStates = $state<Record<string, ComputedFieldState>>({});
	let computedGridStates = $state<Record<string, ComputedGridState>>({});
	
	// Logic Dependency Map: FieldName -> List of Fields that depend on it
	let dependencyMap = $state<Record<string, FormField[]>>({});

	$effect(() => {
		// Build dependency map for auto-calculation
		const map: Record<string, FormField[]> = {};
		for (const field of fields) {
			if (field.logic && field.logic.dependencies) {
				for (const dep of field.logic.dependencies) {
					if (!map[dep]) map[dep] = [];
					map[dep].push(field);
				}
			}
		}
		dependencyMap = map;
	});

	async function executeFieldLogic(field: FormField) {
		if (!field.logic || field.logic.type === 'None' || !field.logic.content) return;

		console.log(`Executing logic for ${field.name} (${field.logic.type})`);

		try {
			if (field.logic.type === 'JS') {
				// prepare safe-ish execution context
				const contextValues = { ...formValues };

				// dynamic function creation
				// signature: (value, form, grids, db, lib) => result
				const func = new Function('value', 'form', 'grids', 'db', 'lib', field.logic.content);

				const dbMock = {
					query: async (sql: string, params: any[]) => {
						console.log('SQL Query:', sql, params);
						return []; // Mock return
					}
				};

				const libMock = {}; // Placeholder for function library

				const result = await func(
					formValues[field.name],
					contextValues,
					gridsContext,
					dbMock,
					libMock
				);

				if (result !== undefined) {
					// Update the field with the result (avoiding direct recursion if possible)
					if (formValues[field.name] !== result) {
						handleFieldChange(field.name, result, true); // true = key for isAutomated
					}
				}
			} else if (field.logic.type === 'SQL') {
				// SQL Placeholder - would replace parameters and call API
				console.log('SQL Logic not fully implemented for execution');
			}
		} catch (e) {
			console.error(`Error executing logic for field ${field.name}:`, e);
			fieldErrors[field.name] = `Logic Error: ${e.message}`;
		}
	}

	// Create evaluation context that updates when form values change
	function createEvaluationContext(): EvaluationContext {
		return {
			form: formValues,
			process: processVariables,
			user: userContext
		};
	}

	// Compute field and grid states whenever form values, rules, or context change
	$effect(() => {
		const allRules = [...conditionRules, ...taskConditionRules];
		if (allRules.length === 0) {
			// No rules, use static field properties
			computedFieldStates = {};
			computedGridStates = {};
			return;
		}

		const context = createEvaluationContext();
		const stateComputer = new ConditionStateComputer(context, { formReadonly: readonly });
		const result = stateComputer.computeFormState(fields, grids, conditionRules, taskConditionRules);

		computedFieldStates = result.fields;
		computedGridStates = result.grids;
	});

	// Helper to get computed field state (with fallback to static properties)
	function getFieldState(field: FormField): ComputedFieldState {
		const computed = computedFieldStates[field.name];
		if (computed) {
			return computed;
		}
		// Fallback to static properties
		return {
			isHidden: field.hidden,
			isReadonly: field.readonly || readonly,
			appliedRules: []
		};
	}

	// Helper to get computed grid state (with fallback to static properties)
	function getGridState(grid: FormGrid): ComputedGridState {
		const computed = computedGridStates[grid.name];
		if (computed) {
			return computed;
		}
		// Fallback: grid is visible and inherits form readonly
		return {
			isHidden: false,
			isReadonly: readonly,
			columnStates: {},
			appliedRules: []
		};
	}

	// Initialize form values from props and defaults - only once
	// This prevents re-initialization from overwriting user changes
	$effect(() => {
		if (formInitialized || userHasMadeChanges) {
			return;
		}

		const newValues = { ...values };

		// Apply default values for fields that don't have a value
		for (const field of fields) {
			if (field.hidden) continue;
			const key = field.name;
			if (newValues[key] === undefined || newValues[key] === null || newValues[key] === '') {
				if (field.defaultValue) {
					newValues[key] = field.defaultValue;
				} else if (field.type === 'checkbox') {
					newValues[key] = false;
				}
			}
		}

		formValues = newValues;
		formInitialized = true;
	});

	// Notify parent of value changes - only after user has made changes
	// This prevents the form from notifying parent during initialization
	// Use untrack for onValuesChange to prevent infinite loop when parent re-renders
	// and creates a new function reference for the callback
	$effect(() => {
		if (userHasMadeChanges) {
			const callback = untrack(() => onValuesChange);
			if (callback) {
				callback(formValues);
			}
		}
	});

	function handleFieldChange(fieldName: string, value: unknown, isAutomated = false) {
		formValues = { ...formValues, [fieldName]: value };
		userHasMadeChanges = true;
		// Clear error when field is modified
		if (fieldErrors[fieldName]) {
			const { [fieldName]: _, ...rest } = fieldErrors;
			fieldErrors = rest;
		}

		// Trigger dependencies
		if (!isAutomated) {
			const dependents = dependencyMap[fieldName] || [];
			for (const dep of dependents) {
				if (dep.logic?.autoCalculate) {
					// Small delay to allow state propagation or use async
					setTimeout(() => executeFieldLogic(dep), 0);
				}
			}
		}
	}

	function handleGridChange(gridName: string, data: Record<string, unknown>[]) {
		formValues = { ...formValues, [gridName]: data };
		userHasMadeChanges = true;
	}

	function handleGridSelectionChange(gridName: string, selectedRows: Record<string, unknown>[]) {
		gridSelections = { ...gridSelections, [gridName]: selectedRows };
	}

	function validateField(field: FormField, value: unknown): string | null {
		// Check required
		if (field.required && (value === undefined || value === null || value === '')) {
			return `${field.label} is required`;
		}

		// Skip validation if empty and not required
		if (value === undefined || value === null || value === '') {
			return null;
		}

		const validation = field.validation;
		if (!validation) return null;

		const strValue = String(value);

		// Min length
		if (validation.minLength && strValue.length < validation.minLength) {
			return `${field.label} must be at least ${validation.minLength} characters`;
		}

		// Max length
		if (validation.maxLength && strValue.length > validation.maxLength) {
			return `${field.label} must not exceed ${validation.maxLength} characters`;
		}

		// Pattern
		if (validation.pattern) {
			try {
				const regex = new RegExp(validation.pattern);
				if (!regex.test(strValue)) {
					return validation.patternMessage || `${field.label} format is invalid`;
				}
			} catch {
				// Invalid regex, skip validation
			}
		}

		// Numeric validations
		if (field.type === 'number' || field.type === 'currency' || field.type === 'percentage') {
			const numValue = Number(value);
			if (!isNaN(numValue)) {
				if (validation.min !== undefined && numValue < validation.min) {
					return `${field.label} must be at least ${validation.min}`;
				}
				if (validation.max !== undefined && numValue > validation.max) {
					return `${field.label} must not exceed ${validation.max}`;
				}
			}
		}

		return null;
	}

	export function validate(): boolean {
		let isValid = true;
		const newErrors: Record<string, string> = {};

		// Validate each visible field (using computed states)
		for (const field of fields) {
			const fieldState = getFieldState(field);
			// Skip validation for hidden fields
			if (fieldState.isHidden) continue;

			const value = formValues[field.name];
			const error = validateField(field, value);

			if (error) {
				newErrors[field.name] = error;
				isValid = false;
			}
		}

		// Validate visible grids
		for (const grid of grids) {
			const gridState = getGridState(grid);
			// Skip validation for hidden grids
			if (gridState.isHidden) continue;

			const gridRef = gridRefs[grid.name];
			if (gridRef && !gridRef.validate()) {
				isValid = false;
			}
		}

		fieldErrors = newErrors;
		return isValid;
	}

	export function getValues(): Record<string, unknown> {
		// Collect grid data
		const result = { ...formValues };
		for (const grid of grids) {
			const gridRef = gridRefs[grid.name];
			if (gridRef) {
				result[grid.name] = gridRef.getData();
			}
		}
		return result;
	}

	export function reset() {
		formValues = { ...values };
		fieldErrors = {};
		userHasMadeChanges = false;
		formInitialized = false;
	}

	// Sort items by grid position for layout, filtering out hidden items
	function getSortedItems(): Array<{ type: 'field' | 'grid'; item: FormField | FormGrid }> {
		const items: Array<{ type: 'field' | 'grid'; item: FormField | FormGrid; row: number; col: number }> = [];

		for (const field of fields) {
			// Use computed state for hidden check
			const fieldState = getFieldState(field);
			if (!fieldState.isHidden) {
				items.push({ type: 'field', item: field, row: field.gridRow, col: field.gridColumn });
			}
		}

		for (const grid of grids) {
			// Use computed state for hidden check
			const gridState = getGridState(grid);
			if (!gridState.isHidden) {
				items.push({ type: 'grid', item: grid, row: grid.gridRow, col: grid.gridColumn });
			}
		}

		// Sort by row first, then column
		items.sort((a, b) => {
			if (a.row !== b.row) return a.row - b.row;
			return a.col - b.col;
		});

		return items.map(({ type, item }) => ({ type, item }));
	}

	function getInputType(fieldType: string): string {
		switch (fieldType) {
			case 'email': return 'email';
			case 'phone': return 'tel';
			case 'number':
			case 'currency':
			case 'percentage': return 'number';
			case 'date': return 'date';
			case 'datetime': return 'datetime-local';
			default: return 'text';
		}
	}

	function formatCurrency(value: unknown): string {
		if (value === null || value === undefined || value === '') return '';
		const num = Number(value);
		return isNaN(num) ? String(value) : num.toFixed(2);
	}

	function formatPercentage(value: unknown): string {
		if (value === null || value === undefined || value === '') return '';
		const num = Number(value);
		return isNaN(num) ? String(value) : num.toString();
	}

	const sortedItems = $derived(getSortedItems());
	const hasFields = $derived(fields.length > 0 || grids.length > 0);

	function tryParseJson(value: unknown): Record<string, unknown>[] {
		if (!value) return [];
		if (Array.isArray(value)) return value;
		if (typeof value === 'string') {
			try {
				const parsed = JSON.parse(value);
				if (Array.isArray(parsed)) return parsed;
			} catch {
				// Not valid JSON
			}
		}
		return [];
	}
</script>

{#if hasFields}
	<div
		class="dynamic-form"
		style="display: grid; grid-template-columns: repeat({gridConfig.columns}, 1fr); gap: {gridConfig.gap}px;"
	>
		{#each sortedItems as { type, item }}
			{#if type === 'field'}
				{@const field = item as FormField}
				{@const value = formValues[field.name]}
				{@const error = fieldErrors[field.name] || errors[field.name]}
				{@const fieldState = getFieldState(field)}
				{@const isReadonly = fieldState.isReadonly}

				<div
					class="form-field {field.cssClass || ''}"
					style="grid-column: span {field.gridWidth};"
				>
					{#if field.type === 'checkbox'}
						<label class="flex items-center space-x-2 cursor-pointer">
							<input
								type="checkbox"
								checked={Boolean(value)}
								onchange={(e) => handleFieldChange(field.name, e.currentTarget.checked)}
								disabled={isReadonly}
								class="h-4 w-4 rounded border-gray-300 text-blue-600 focus:ring-blue-500 disabled:bg-gray-100"
							/>
							<span class="text-sm font-medium text-gray-700">
								{field.label}
								{#if field.required}<span class="text-red-500">*</span>{/if}
							</span>
						</label>
						{#if field.tooltip}
							<p class="text-xs text-gray-500 mt-1">{field.tooltip}</p>
						{/if}
					{:else}
						<label for={`field-${field.name}`} class="block text-sm font-medium text-gray-700 mb-1">
							{field.label}
							{#if field.required}<span class="text-red-500">*</span>{/if}
						</label>

						{#if field.type === 'textarea'}
							<textarea
								id={`field-${field.name}`}
								value={String(value ?? '')}
								oninput={(e) => handleFieldChange(field.name, e.currentTarget.value)}
								placeholder={field.placeholder}
								readonly={isReadonly}
								rows="3"
								class="w-full px-3 py-2 border rounded-md focus:ring-2 focus:ring-blue-500 text-sm
									{error ? 'border-red-500' : 'border-gray-300'}
									{isReadonly ? 'bg-gray-50' : ''}"
							></textarea>
						{:else if field.type === 'select'}
							<select
								id={`field-${field.name}`}
								value={value ?? ''}
								onchange={(e) => handleFieldChange(field.name, e.currentTarget.value)}
								disabled={isReadonly}
								class="w-full px-3 py-2 border rounded-md focus:ring-2 focus:ring-blue-500 text-sm
									{error ? 'border-red-500' : 'border-gray-300'}
									{isReadonly ? 'bg-gray-50' : ''}"
							>
								<option value="">{field.placeholder || 'Select...'}</option>
								{#each field.options || [] as option}
									<option value={option.value}>{option.label}</option>
								{/each}
							</select>
						{:else if field.type === 'multiselect'}
							<select
								id={`field-${field.name}`}
								multiple
								value={Array.isArray(value) ? value : []}
								onchange={(e) => {
									const selected = Array.from(e.currentTarget.selectedOptions).map(o => o.value);
									handleFieldChange(field.name, selected);
								}}
								disabled={isReadonly}
								class="w-full px-3 py-2 border rounded-md focus:ring-2 focus:ring-blue-500 text-sm
									{error ? 'border-red-500' : 'border-gray-300'}
									{isReadonly ? 'bg-gray-50' : ''}"
								size="4"
							>
								{#each field.options || [] as option}
									<option value={option.value}>{option.label}</option>
								{/each}
							</select>
						{:else if field.type === 'radio'}
							<div class="space-y-2">
								{#each field.options || [] as option}
									<label class="flex items-center space-x-2 cursor-pointer">
										<input
											type="radio"
											name={field.name}
											value={option.value}
											checked={value === option.value}
											onchange={() => handleFieldChange(field.name, option.value)}
											disabled={isReadonly}
											class="h-4 w-4 border-gray-300 text-blue-600 focus:ring-blue-500"
										/>
										<span class="text-sm text-gray-700">{option.label}</span>
									</label>
								{/each}
							</div>
						{:else if field.type === 'currency'}
							<div class="relative">
								<span class="absolute left-3 top-1/2 transform -translate-y-1/2 text-gray-500">$</span>
								<input
									id={`field-${field.name}`}
									type="number"
									value={formatCurrency(value)}
									oninput={(e) => handleFieldChange(field.name, e.currentTarget.valueAsNumber)}
									placeholder={field.placeholder}
									readonly={isReadonly}
									step="0.01"
									min={field.validation?.min}
									max={field.validation?.max}
									class="w-full pl-7 pr-3 py-2 border rounded-md focus:ring-2 focus:ring-blue-500 text-sm
										{error ? 'border-red-500' : 'border-gray-300'}
										{isReadonly ? 'bg-gray-50' : ''}"
								/>
							</div>
						{:else if field.type === 'percentage'}
							<div class="relative">
								<input
									id={`field-${field.name}`}
									type="number"
									value={formatPercentage(value)}
									oninput={(e) => handleFieldChange(field.name, e.currentTarget.valueAsNumber)}
									placeholder={field.placeholder}
									readonly={isReadonly}
									min={field.validation?.min ?? 0}
									max={field.validation?.max ?? 100}
									class="w-full pr-8 px-3 py-2 border rounded-md focus:ring-2 focus:ring-blue-500 text-sm
										{error ? 'border-red-500' : 'border-gray-300'}
										{isReadonly ? 'bg-gray-50' : ''}"
								/>
								<span class="absolute right-3 top-1/2 transform -translate-y-1/2 text-gray-500">%</span>
							</div>
						{:else if field.type === 'file'}
							<input
								id={`field-${field.name}`}
								type="file"
								onchange={(e) => {
									const file = e.currentTarget.files?.[0];
									handleFieldChange(field.name, file?.name || '');
								}}
								disabled={isReadonly}
								class="w-full px-3 py-2 border rounded-md focus:ring-2 focus:ring-blue-500 text-sm
									{error ? 'border-red-500' : 'border-gray-300'}
									{isReadonly ? 'bg-gray-50' : ''}"
							/>
						{:else if field.type === 'expression'}
							<div class="px-3 py-2 bg-gray-100 border border-gray-300 rounded-md text-sm text-gray-600">
								{value ?? field.defaultExpression ?? '-'}
							</div>
						{:else}
							<input
								id={`field-${field.name}`}
								type={getInputType(field.type)}
								value={value ?? ''}
								oninput={(e) => {
									const inputType = getInputType(field.type);
									if (inputType === 'number') {
										handleFieldChange(field.name, e.currentTarget.valueAsNumber);
									} else {
										handleFieldChange(field.name, e.currentTarget.value);
									}
								}}
								placeholder={field.placeholder}
								readonly={isReadonly}
								min={field.validation?.min}
								max={field.validation?.max}
								minlength={field.validation?.minLength}
								maxlength={field.validation?.maxLength}
								class="w-full px-3 py-2 border rounded-md focus:ring-2 focus:ring-blue-500 text-sm
									{error ? 'border-red-500' : 'border-gray-300'}
									{isReadonly ? 'bg-gray-50' : ''}"
							/>
						{/if}

						{#if field.tooltip}
							<p class="text-xs text-gray-500 mt-1">{field.tooltip}</p>
						{/if}
					{/if}

					{#if error}
						<p class="text-xs text-red-600 mt-1">{error}</p>
					{/if}
				</div>
			{:else}
				{@const grid = item as FormGrid}
				{@const gridState = getGridState(grid)}
				<div
					class="form-grid {grid.cssClass || ''}"
					style="grid-column: span {grid.gridWidth};"
				>
					<DynamicGrid
						bind:this={gridRefs[grid.name]}
						columns={grid.columns}
						label={grid.label}
						description={grid.description}
						minRows={grid.minRows}
						maxRows={grid.maxRows}
						initialData={tryParseJson(formValues[grid.name])}
						readonly={gridState.isReadonly}
						columnStates={gridState.columnStates}
						enableMultiSelect={true}
						formValues={formValues}
						gridsContext={gridsContext}
						onDataChange={(data) => handleGridChange(grid.name, data)}
						onSelectionChange={(selected) => handleGridSelectionChange(grid.name, selected)}
					/>
				</div>
			{/if}
		{/each}
	</div>
{:else}
	<p class="text-gray-500 text-sm py-4">No form fields defined for this step.</p>
{/if}

<style>
	.dynamic-form {
		width: 100%;
	}

	.form-field {
		margin-bottom: 1rem;
	}

	.form-grid {
		margin-bottom: 1.5rem;
	}
</style>
