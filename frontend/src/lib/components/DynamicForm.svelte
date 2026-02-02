<script lang="ts">
	import { untrack } from 'svelte';
	import type { FormField, FormGrid, GridConfig, FieldConditionRule, ComputedFieldState, ComputedGridState } from '$lib/types';
	import type { UserContext, EvaluationContext, GridContext } from '$lib/utils/expression-evaluator';
	import { ConditionStateComputer } from '$lib/utils/condition-state-computer';
	import { createSafeEvaluator, SafeExpressionEvaluator } from '$lib/utils/expression-evaluator';
	import DynamicGrid from './DynamicGrid.svelte';
    
    // Field Components
    import CheckboxField from './fields/CheckboxField.svelte';
    import SelectField from './fields/SelectField.svelte';
    import RadioField from './fields/RadioField.svelte';
    import InputField from './fields/InputField.svelte';
    import TextAreaField from './fields/TextAreaField.svelte';
    import SignatureField from './fields/SignatureField.svelte';
    import FileField from './fields/FileField.svelte';
    import UserGroupPickerField from './fields/UserGroupPickerField.svelte';
    import HeaderField from './fields/HeaderField.svelte';
    import ExpressionField from './fields/ExpressionField.svelte';

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
        // Task context for expression evaluation
        task?: { id: string; name: string; taskDefinitionKey: string };
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
		userContext = { id: '', username: '', roles: [], groups: [] },
        task
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

	// Computed field and grid states based on condition rules AND visibility expressions
	let computedFieldStates = $state<Record<string, ComputedFieldState>>({});
	let computedGridStates = $state<Record<string, ComputedGridState>>({});
	
	// Logic Dependency Map: FieldName -> List of Fields that depend on it
	// Also includes dependency for visibility expressions
	let dependencyMap = $state<Record<string, (FormField | FormGrid)[]>>({});

	function parseDependencies(expression: string): string[] {
		if (!expression) return [];
		const regex = /form\.([a-zA-Z0-9_]+)|grids\.([a-zA-Z0-9_]+)/g;
		const matches = [...expression.matchAll(regex)];
		const deps = new Set<string>();
		for (const match of matches) {
			if (match[1]) deps.add(match[1]);
			if (match[2]) deps.add(match[2]);
		}
		return Array.from(deps);
	}

	$effect(() => {
		const map: Record<string, (FormField | FormGrid)[]> = {};
		for (const field of fields) {
			const expressions = [
				field.visibilityExpression,
				field.calculationExpression,
				field.validationExpression,
				field.hiddenExpression,
				field.readonlyExpression,
				field.requiredExpression
			].filter(Boolean) as string[];

			for (const expression of expressions) {
				const deps = parseDependencies(expression);
				for (const dep of deps) {
					if (!map[dep]) map[dep] = [];
					map[dep].push(field);
				}
			}
		}
        for (const grid of grids) {
            if (grid.visibilityExpression) {
                const deps = parseDependencies(grid.visibilityExpression);
                for (const dep of deps) {
                    if (!map[dep]) map[dep] = [];
                    map[dep].push(grid);
                }
            }
        }
		dependencyMap = map;
	});

    /**
     * Create a safe expression evaluator with current context
     * This replaces the unsafe new Function() approach
     */
    function createContextEvaluator(): SafeExpressionEvaluator {
        return createSafeEvaluator({
            form: formValues,
            process: processVariables,
            user: userContext,
            grids: gridsContext as unknown as Record<string, GridContext>,
            task: task
        });
    }

 async function executeFieldLogic(field: FormField) {
        // New calculation expression
        if (field.calculationExpression) {
            const evaluator = createContextEvaluator();
            evaluator.updateExtendedContext({ value: formValues[field.name] });
            
            const result = evaluator.evaluateCalculation(field.calculationExpression);

            if (result !== undefined && formValues[field.name] !== result) {
                handleFieldChange(field.name, result, true);
            }
        }
 }

    // Evaluate Visibility Expressions
    function evaluateVisibility(field: FormField): boolean {
        if (!field.visibilityExpression) return !field.hidden;

        const evaluator = createContextEvaluator();
        evaluator.updateExtendedContext({ value: formValues[field.name] });
        
        return evaluator.evaluateVisibility(field.visibilityExpression);
    }

    // Evaluate Grid Visibility
    function evaluateGridVisibility(grid: FormGrid): boolean {
        if (!grid.visibilityExpression) return true;

        const evaluator = createContextEvaluator();
        return evaluator.evaluateVisibility(grid.visibilityExpression);
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
        // This effect runs whenever formValues changes.
        // (Placeholder for future logic merging)
	});

    // We modify the existing effect for ConditionStateComputer to also account for visibilityExpressions if possible,
    // or we wrap `getFieldState`.

	$effect(() => {
		const context = createEvaluationContext();
		const stateComputer = new ConditionStateComputer(context, { formReadonly: readonly });
		const result = stateComputer.computeFormState(fields, grids, conditionRules, taskConditionRules);

		computedFieldStates = result.fields;
		computedGridStates = result.grids;
	});

	// Helper to get computed field state (with fallback to static properties)
	function getFieldState(field: FormField): ComputedFieldState {
		// Start with rule-based state
        let state = computedFieldStates[field.name] || {
			isHidden: !!field.hidden,
			isReadonly: field.readonly || readonly,
			appliedRules: []
		};

        // Apply Visibility Expression
        if (state.appliedRules.length === 0 && field.visibilityExpression) {
            const isVisible = evaluateVisibility(field);
            state = { ...state, isHidden: !isVisible };
        } else if (field.visibilityExpression) {
             const baseVisible = evaluateVisibility(field);
             if (!state.appliedRules.some(r => r.includes('hide') || r.includes('show'))) {
                  state.isHidden = !baseVisible;
             }
        }

		return state;
	}

	// Helper to get computed grid state (with fallback to static properties)
	function getGridState(grid: FormGrid): ComputedGridState {
		const state = computedGridStates[grid.name] || {
			isHidden: false,
			isReadonly: readonly,
			columnStates: {},
			appliedRules: []
		};

        // Apply visibility expression
        if (grid.visibilityExpression) {
            const isVisible = evaluateGridVisibility(grid);
            // If rule didn't hide it, expression can hide it
            if (!state.appliedRules.some(r => r.includes('hide'))) {
                state.isHidden = !isVisible;
            }
        }

        return state;
	}

	// Initialize form values from props and defaults - only once
	$effect(() => {
		if (formInitialized || userHasMadeChanges) {
			return;
		}

		const newValues = { ...values };

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
		if (fieldErrors[fieldName]) {
			const { [fieldName]: _, ...rest } = fieldErrors;
			fieldErrors = rest;
		}

		if (!isAutomated) {
			const dependents = dependencyMap[fieldName] || [];
			for (const dep of dependents) {
                if ('calculationExpression' in dep) {
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
		if (field.required && (value === undefined || value === null || value === '')) {
			return `${field.label} is required`;
		}

		if (value === undefined || value === null || value === '') {
			return null;
		}

        if (field.validationExpression) {
            const evaluator = createContextEvaluator();
            evaluator.updateExtendedContext({ value: value });
            const isValidOrMsg = evaluator.evaluateValidation(field.validationExpression);
            if (isValidOrMsg === false) return field.validationMessage || `${field.label} is invalid`;
            if (typeof isValidOrMsg === 'string') return isValidOrMsg;
        }

		const validation = field.validation;
		if (!validation) return null;

		const strValue = String(value);
		if (validation.minLength && strValue.length < validation.minLength) return `${field.label} must be at least ${validation.minLength} characters`;
		if (validation.maxLength && strValue.length > validation.maxLength) return `${field.label} must not exceed ${validation.maxLength} characters`;

		if (validation.pattern) {
			try {
				const regex = new RegExp(validation.pattern);
				if (!regex.test(strValue)) return validation.patternMessage || `${field.label} format is invalid`;
			} catch { /* ignore */ }
		}

		if (field.type === 'number' || field.type === 'currency' || field.type === 'percentage') {
			const numValue = Number(value);
			if (!isNaN(numValue)) {
				if (validation.min !== undefined && numValue < validation.min) return `${field.label} must be at least ${validation.min}`;
				if (validation.max !== undefined && numValue > validation.max) return `${field.label} must not exceed ${validation.max}`;
			}
		}

		return null;
	}

	export function validate(): boolean {
		let isValid = true;
		const newErrors: Record<string, string> = {};

		for (const field of fields) {
			const fieldState = getFieldState(field);
			if (fieldState.isHidden) continue;
			const value = formValues[field.name];
			const error = validateField(field, value);
			if (error) {
				newErrors[field.name] = error;
				isValid = false;
			}
		}

		for (const grid of grids) {
			const gridState = getGridState(grid);
			if (gridState.isHidden) continue;
			const gridRef = gridRefs[grid.name];
			if (gridRef && !gridRef.validate()) isValid = false;
		}

		fieldErrors = newErrors;
		return isValid;
	}

	export function getValues(): Record<string, unknown> {
		const result = { ...formValues };
		for (const grid of grids) {
			const gridRef = gridRefs[grid.name];
			if (gridRef) result[grid.name] = gridRef.getData();
		}
		return result;
	}

	export function reset() {
		formValues = { ...values };
		fieldErrors = {};
		userHasMadeChanges = false;
		formInitialized = false;
	}

	function getSortedItems(): Array<{ type: 'field' | 'grid'; item: FormField | FormGrid }> {
		const items: Array<{ type: 'field' | 'grid'; item: FormField | FormGrid; row: number; col: number }> = [];

		for (const field of fields) {
			const fieldState = getFieldState(field);
			if (!fieldState.isHidden) items.push({ type: 'field', item: field, row: field.gridRow, col: field.gridColumn });
		}

		for (const grid of grids) {
			const gridState = getGridState(grid);
			if (!gridState.isHidden) items.push({ type: 'grid', item: grid, row: grid.gridRow, col: grid.gridColumn });
		}

		items.sort((a, b) => {
			if (a.row !== b.row) return a.row - b.row;
			return a.col - b.col;
		});

		return items.map(({ type, item }) => ({ type, item }));
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
			} catch { /* ignore */ }
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
						<CheckboxField {field} {value} {isReadonly} onchange={(val) => handleFieldChange(field.name, val)} />
					{:else}
						{#if field.type !== 'header'}
							<label for={`field-${field.name}`} class="block text-sm font-medium text-gray-700 mb-1">
								{field.label}
								{#if field.required}<span class="text-red-500">*</span>{/if}
							</label>
						{/if}

						{#if field.type === 'textarea'}
							<TextAreaField {field} {value} {isReadonly} {error} onchange={(val) => handleFieldChange(field.name, val)} />
						{:else if field.type === 'select' || field.type === 'multiselect'}
							<SelectField {field} {value} {isReadonly} {error} onchange={(val) => handleFieldChange(field.name, val)} />
						{:else if field.type === 'radio'}
							<RadioField {field} {value} {isReadonly} onchange={(val) => handleFieldChange(field.name, val)} />
						{:else if field.type === 'currency' || field.type === 'percentage'}
							<InputField {field} {value} {isReadonly} {error} onchange={(val) => handleFieldChange(field.name, val)} />
						{:else if field.type === 'file'}
							<FileField {field} {isReadonly} {error} onchange={(val) => handleFieldChange(field.name, val)} />
						{:else if field.type === 'signature'}
							<SignatureField {field} {value} {isReadonly} {error} onchange={(val) => handleFieldChange(field.name, val)} />
                        {:else if field.type === 'userPicker' || field.type === 'groupPicker'}
                            <UserGroupPickerField {field} {value} {isReadonly} {error} onchange={(val) => handleFieldChange(field.name, val)} />
						{:else if field.type === 'expression'}
							<ExpressionField {field} {value} />
						{:else if field.type === 'header'}
							<HeaderField {field} />
						{:else}
							<InputField {field} {value} {isReadonly} {error} onchange={(val) => handleFieldChange(field.name, val)} />
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
                        processVariables={processVariables}
                        userContext={userContext}
                        task={task}

                        enablePagination={grid.enablePagination}
                        pageSize={grid.pageSize}
                        enableSorting={grid.enableSorting}
                        enableGrouping={grid.enableGrouping}
                        groupByColumn={grid.groupByColumn}
                        enableRowActions={grid.enableRowActions}
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