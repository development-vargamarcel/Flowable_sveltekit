<script lang="ts">
	import { untrack } from 'svelte';
	import type { FormField, FormGrid, GridConfig, FieldConditionRule } from '$lib/types';
	import type { UserContext } from '$lib/utils/expression-evaluator';
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
    
    import { FormController } from './fields/form-controller.svelte';

	interface Props {
		fields: FormField[];
		grids: FormGrid[];
		gridConfig: GridConfig;
		values?: Record<string, unknown>;
		errors?: Record<string, string>;
		readonly?: boolean;
		onValuesChange?: (values: Record<string, unknown>) => void;
		conditionRules?: FieldConditionRule[];
		taskConditionRules?: FieldConditionRule[];
		processVariables?: Record<string, unknown>;
		userContext?: UserContext;
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

    // Controller instance with closure to satisfy Svelte 5 linting
    const controller = new FormController(() => ({
        fields,
        grids,
        initialValues: values,
        readonly,
        conditionRules,
        taskConditionRules,
        processVariables,
        userContext,
        task,
        onValuesChange
    }));

	// Grid component references for validation
	const gridRefs: Record<string, DynamicGrid> = {};

	// Initialize form once
	$effect(() => {
		controller.initialize();
	});

	// Notify parent of changes
	$effect(() => {
		if (controller.userHasMadeChanges) {
			const callback = untrack(() => onValuesChange);
			if (callback) {
				callback(controller.formValues);
			}
		}
	});

	export function validate(): boolean {
        return controller.validate(gridRefs);
	}

	export function getValues(): Record<string, unknown> {
        return controller.getValues(gridRefs);
	}

	export function reset() {
        controller.reset();
	}

	const sortedItems = $derived(controller.getSortedItems());
	const hasFields = $derived(fields.length > 0 || grids.length > 0);
</script>

{#if hasFields}
	<div
		class="dynamic-form"
		style="display: grid; grid-template-columns: repeat({gridConfig.columns}, 1fr); gap: {gridConfig.gap}px;"
	>
		{#each sortedItems as { type, item }}
			{#if type === 'field'}
				{@const field = item as FormField}
				{@const value = controller.formValues[field.name]}
				{@const error = controller.fieldErrors[field.name] || errors[field.name]}
				{@const fieldState = controller.getFieldState(field)}
				{@const isReadonly = fieldState.isReadonly}

				<div
					class="form-field {field.cssClass || ''}"
					style="grid-column: span {field.gridWidth};"
				>
					{#if field.type === 'checkbox'}
						<CheckboxField {field} {value} {isReadonly} onchange={(val) => controller.handleFieldChange(field.name, val)} />
					{:else}
						{#if field.type !== 'header'}
							<label for={`field-${field.name}`} class="block text-sm font-medium text-gray-700 mb-1">
								{field.label}
								{#if field.required}<span class="text-red-500">*</span>{/if}
							</label>
						{/if}

						{#if field.type === 'textarea'}
							<TextAreaField {field} {value} {isReadonly} {error} onchange={(val) => controller.handleFieldChange(field.name, val)} />
						{:else if field.type === 'select' || field.type === 'multiselect'}
							<SelectField {field} {value} {isReadonly} {error} onchange={(val) => controller.handleFieldChange(field.name, val)} />
						{:else if field.type === 'radio'}
							<RadioField {field} {value} {isReadonly} onchange={(val) => controller.handleFieldChange(field.name, val)} />
						{:else if field.type === 'currency' || field.type === 'percentage'}
							<InputField {field} {value} {isReadonly} {error} onchange={(val) => controller.handleFieldChange(field.name, val)} />
						{:else if field.type === 'file'}
							<FileField {field} {isReadonly} {error} onchange={(val) => controller.handleFieldChange(field.name, val)} />
						{:else if field.type === 'signature'}
							<SignatureField {field} value={value as string | null} {isReadonly} {error} onchange={(val) => controller.handleFieldChange(field.name, val)} />
                        {:else if field.type === 'userPicker' || field.type === 'groupPicker'}
                            <UserGroupPickerField {field} {value} {isReadonly} {error} onchange={(val) => controller.handleFieldChange(field.name, val)} />
						{:else if field.type === 'expression'}
							<ExpressionField {field} {value} />
						{:else if field.type === 'header'}
							<HeaderField {field} />
						{:else}
							<InputField {field} {value} {isReadonly} {error} onchange={(val) => controller.handleFieldChange(field.name, val)} />
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
				{@const gridState = controller.getGridState(grid)}
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
						initialData={controller.formValues[grid.name] as Record<string, unknown>[]}
						readonly={gridState.isReadonly}
						columnStates={gridState.columnStates}
						enableMultiSelect={true}
						formValues={controller.formValues}
						gridsContext={controller.gridsContext as Record<string, unknown>}
						onDataChange={(data) => controller.handleGridChange(grid.name, data)}
						onSelectionChange={(selected) => controller.handleGridSelectionChange(grid.name, selected)}
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