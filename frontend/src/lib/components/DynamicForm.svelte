<script lang="ts">
	import { untrack } from 'svelte';
	import type { FormField, FormGrid, GridConfig, FieldConditionRule, ComputedFieldState, ComputedGridState } from '$lib/types';
	import type { UserContext, EvaluationContext, ExtendedEvaluationContext, GridContext } from '$lib/utils/expression-evaluator';
	import { ConditionStateComputer } from '$lib/utils/condition-state-computer';
	import { createSafeEvaluator, SafeExpressionEvaluator } from '$lib/utils/expression-evaluator';
	import DynamicGrid from './DynamicGrid.svelte';
    import { Editor } from '@tiptap/core';
    import StarterKit from '@tiptap/starter-kit';
    import SignaturePad from 'signature_pad';

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

    /**
     * Safely evaluate an expression without using new Function()
     * Falls back to the safe expression evaluator
     */
    function safeEvaluate(expression: string, context: { value?: unknown }): unknown {
        const evaluator = createContextEvaluator();
        
        // Update with current field value if provided
        if (context.value !== undefined) {
            evaluator.updateExtendedContext({ value: context.value });
        }
        
        return evaluator.evaluateCalculation(expression);
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
        // We use it to re-evaluate visibility and calculation for ALL fields that might have expressions.

        // 1. Re-evaluate visibility expressions
        const newComputedStates = { ...computedFieldStates };
        let stateChanged = false;

        for (const field of fields) {
            // Priority: 1. Condition Rules (from ConditionStateComputer), 2. visibilityExpression, 3. Static hidden
            // Currently ConditionStateComputer runs in its own effect and updates `computedFieldStates`.
            // We need to merge that logic.
            // Since ConditionStateComputer is derived from rules, let's let it run first (it's in another effect).
            // But wait, the other effect depends on `formValues` too.
            // Let's combine them or apply visibilityExpression on top.

            // Actually, `computedFieldStates` is overwritten by the other effect.
            // So we should probably integrate visibilityExpression logic inside `getFieldState` or update `computedFieldStates` here carefully.
            // The cleanest way is to respect the ConditionRules as overrides, but use visibilityExpression as base visibility.

            // However, the other effect is:
            // $effect(() => { ... result = stateComputer.computeFormState(...) ... })
            // If we write to computedFieldStates here, we might conflict.

            // Let's create a derived state for "expression-based visibility" and merge it.
        }

        // 2. Run calculations
        // We iterate all fields to see if they have calculation expressions that depend on changed values.
        // Since we don't have a dependency graph for expressions, we should probably run all calculation expressions
        // whenever formValues change. This is expensive but correct.
        // To avoid infinite loops, we only update if value is different.

        // We put this in a timeout to avoid synchronous loop with `handleFieldChange`
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
			isHidden: field.hidden,
			isReadonly: field.readonly || readonly,
			appliedRules: []
		};

        // Apply Visibility Expression (Expression > Static, but Rule > Expression usually? Or Rule supplements?)
        // Let's say Rule acts as an override/modifier.
        // If no rule applied, check expression.
        if (state.appliedRules.length === 0 && field.visibilityExpression) {
            const isVisible = evaluateVisibility(field);
            state = { ...state, isHidden: !isVisible };
        } else if (field.visibilityExpression) {
             // If rules applied, they might have set isHidden=true.
             // If rules set isHidden=false (explicitly visible), we respect that.
             // If rules didn't touch visibility (e.g. only readonly), we check expression.
             // This is getting complex.
             // Simple approach: Visibility Expression is the "base" visibility. Rules override it.
             const baseVisible = evaluateVisibility(field);
             if (!state.appliedRules.some(r => r.includes('hide') || r.includes('show'))) {
                  state.isHidden = !baseVisible;
             }
        }

		return state;
	}

	// Helper to get computed grid state (with fallback to static properties)
	function getGridState(grid: FormGrid): ComputedGridState {
		let state = computedGridStates[grid.name] || {
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
		// Check required
		if (field.required && (value === undefined || value === null || value === '')) {
			return `${field.label} is required`;
		}

		// Skip validation if empty and not required
		if (value === undefined || value === null || value === '') {
			return null;
		}

        // Custom Validation Expression - using safe evaluator
        if (field.validationExpression) {
            const evaluator = createContextEvaluator();
            evaluator.updateExtendedContext({ value: value });
            
            const isValidOrMsg = evaluator.evaluateValidation(field.validationExpression);

            if (isValidOrMsg === false) {
                return field.validationMessage || `${field.label} is invalid`;
            }
            if (typeof isValidOrMsg === 'string') {
                return isValidOrMsg;
            }
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

    // Tiptap setup
    function setupTiptap(node: HTMLElement, { content, onUpdate, editable }: any) {
        const editor = new Editor({
            element: node,
            extensions: [StarterKit],
            content: content || '',
            editable: editable,
            onUpdate: ({ editor }) => {
                onUpdate(editor.getHTML());
            },
            editorProps: {
                attributes: {
                    class: 'prose prose-sm focus:outline-none min-h-[100px] max-w-none'
                }
            }
        });

        return {
            destroy() {
                editor.destroy();
            },
            update(newParams: any) {
                if (editor.isEditable !== newParams.editable) {
                    editor.setEditable(newParams.editable);
                }
                // Only update content if it's different to prevent cursor jumps
                if (newParams.content !== editor.getHTML()) {
                    // editor.commands.setContent(newParams.content || '');
                }
            }
        };
    }

    // Signature Pad setup
    function setupSignaturePad(node: HTMLCanvasElement, { value, onUpdate, readonly }: any) {
        const pad = new SignaturePad(node);

        if (value) {
            pad.fromDataURL(value);
        }

        if (readonly) {
            pad.off();
        }

        // Handle resize properly
        const resizeCanvas = () => {
            const ratio = Math.max(window.devicePixelRatio || 1, 1);
            node.width = node.offsetWidth * ratio;
            node.height = node.offsetHeight * ratio;
            node.getContext("2d")?.scale(ratio, ratio);
            if (value) pad.fromDataURL(value); // Reload data after resize
        };

        // Initial resize
        setTimeout(resizeCanvas, 0);
        // window.addEventListener('resize', resizeCanvas); // Removed to avoid listener leak for now

        pad.addEventListener('endStroke', () => {
            if (!readonly) {
                onUpdate(pad.toDataURL());
            }
        });

        return {
            destroy() {
                pad.off();
            },
            update(newParams: any) {
                if (newParams.readonly) pad.off(); else pad.on();
            }
        };
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
						{#if field.type !== 'header'}
							<label for={`field-${field.name}`} class="block text-sm font-medium text-gray-700 mb-1">
								{field.label}
								{#if field.required}<span class="text-red-500">*</span>{/if}
							</label>
						{/if}

						{#if field.type === 'textarea'}
                            {#if field.richText}
                                <div class="border rounded-md {error ? 'border-red-500' : 'border-gray-300'} {isReadonly ? 'bg-gray-50' : 'bg-white'} overflow-hidden">
                                    {#if !isReadonly}
                                        <!-- Toolbar could go here -->
                                        <div class="border-b border-gray-200 bg-gray-50 px-2 py-1 text-xs text-gray-500">
                                            Rich Text Editor
                                        </div>
                                    {/if}
                                    <div
                                        class="p-3 min-h-[100px]"
                                        use:setupTiptap={{
                                            content: String(value ?? ''),
                                            editable: !isReadonly,
                                            onUpdate: (html: string) => handleFieldChange(field.name, html)
                                        }}
                                    ></div>
                                </div>
                            {:else}
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
                            {/if}
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
                        {:else if field.type === 'signature'}
                            <div class="border rounded-md {error ? 'border-red-500' : 'border-gray-300'} bg-white">
                                <canvas
                                    class="w-full h-40 touch-none"
                                    use:setupSignaturePad={{
                                        value: value,
                                        readonly: isReadonly,
                                        onUpdate: (dataUrl: string) => handleFieldChange(field.name, dataUrl)
                                    }}
                                ></canvas>
                                {#if !isReadonly}
                                    <div class="border-t border-gray-200 p-2 text-right bg-gray-50">
                                        <button
                                            class="text-xs text-gray-500 hover:text-red-500"
                                            onclick={() => handleFieldChange(field.name, null)}
                                        >
                                            Clear Signature
                                        </button>
                                    </div>
                                {/if}
                            </div>
                        {:else if field.type === 'userPicker' || field.type === 'groupPicker'}
                            <select
								id={`field-${field.name}`}
								value={value ?? ''}
								onchange={(e) => handleFieldChange(field.name, e.currentTarget.value)}
								disabled={isReadonly}
								class="w-full px-3 py-2 border rounded-md focus:ring-2 focus:ring-blue-500 text-sm
									{error ? 'border-red-500' : 'border-gray-300'}
									{isReadonly ? 'bg-gray-50' : ''}"
							>
								<option value="">Select {field.type === 'userPicker' ? 'User' : 'Group'}...</option>
                                <option value="user1">User 1</option>
                                <option value="user2">User 2</option>
                                <option value="manager">Manager</option>
                                <option value="admin">Admin</option>
							</select>
						{:else if field.type === 'expression'}
							<div class="px-3 py-2 bg-gray-100 border border-gray-300 rounded-md text-sm text-gray-600">
								{value ?? field.defaultExpression ?? '-'}
							</div>
						{:else if field.type === 'header'}
							<h3 class={field.cssClass ? '' : 'text-lg font-semibold text-gray-900 border-b pb-2 mb-2'}>
								{field.label}
							</h3>
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

    :global(.ProseMirror) {
        outline: none;
    }

    :global(.ProseMirror p.is-editor-empty:first-child::before) {
      color: #adb5bd;
      content: attr(data-placeholder);
      float: left;
      height: 0;
      pointer-events: none;
    }
</style>
