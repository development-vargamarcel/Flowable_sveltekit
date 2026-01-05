<script lang="ts">
  import type { GridColumn, ComputedFieldState } from '$lib/types';
  import * as Table from '$lib/components/ui/table';
  import { Button } from '$lib/components/ui/button';
  import { Input } from '$lib/components/ui/input';
  import { Textarea } from '$lib/components/ui/textarea';
  import * as Select from '$lib/components/ui/select';
  import { Plus } from '@lucide/svelte';

  interface GridRow {
    id: string;
    data: Record<string, unknown>;
    isEditing: boolean;
    errors: Record<string, string | null>;
  }

  interface Props {
    columns: GridColumn[];
    label?: string;
    description?: string;
    minRows?: number;
    maxRows?: number;
    initialData?: Record<string, unknown>[];
    readonly?: boolean;
    columnStates?: Record<string, ComputedFieldState>;
    enableMultiSelect?: boolean;
    formValues?: Record<string, unknown>;
    gridsContext?: Record<string, any>;
    processVariables?: Record<string, unknown>;
    userContext?: any;
    task?: any;
    onDataChange?: (data: Record<string, unknown>[]) => void;
    onSelectionChange?: (selectedRows: Record<string, unknown>[]) => void;
  }

  const {
    columns,
    label = 'Grid Data',
    description,
    minRows = 0,
    maxRows = 0,
    initialData = [],
    readonly = false,
    columnStates = {},
    enableMultiSelect = false,
    formValues = {},
    gridsContext = {},
    processVariables = {},
    userContext = {},
    task = null,
    onDataChange,
    onSelectionChange
  }: Props = $props();

  // Helper to safely evaluate expressions
  function safeEvaluate(expression: string, context: any): any {
     try {
         const func = new Function('value', 'row', 'form', 'grids', 'process', 'task', 'user', expression);
         return func(
             context.value,
             context.row,
             context.form,
             context.grids,
             context.process,
             context.task,
             context.user
         );
     } catch (e) {
         console.warn('Grid expression evaluation failed:', expression, e);
         return undefined;
     }
  }

  // Helper to get column state (with fallback)
  function getColumnState(column: GridColumn, rowData?: Record<string, unknown>): ComputedFieldState {
    let state = columnStates[column.name] || { isHidden: false, isReadonly: readonly, appliedRules: [] };

    if (column.visibilityExpression) {
        // We try to evaluate without row first for column-level visibility
        const isVisible = safeEvaluate(column.visibilityExpression, {
            value: null,
            row: {}, // Empty row context
            form: formValues,
            grids: gridsContext,
            process: processVariables,
            task: task,
            user: userContext
        });

        if (isVisible === false) {
            state = { ...state, isHidden: true };
        }
    }

    return state;
  }

  // Get visible columns (filter out hidden ones)
  const visibleColumns = $derived(columns.filter((col) => !getColumnState(col).isHidden));

  // Check if a column is readonly (grid readonly OR column-specific readonly)
  function isColumnReadonly(column: GridColumn): boolean {
    if (readonly) return true;
    return getColumnState(column).isReadonly;
  }

  let rows = $state<GridRow[]>([]);
  let dataLoadedFromProps = $state(false);
  let userHasMadeChanges = $state(false);
  let selectedRowIds = $state<Set<string>>(new Set());

  // Check if all rows are selected
  const allSelected = $derived(rows.length > 0 && selectedRowIds.size === rows.length);
  const someSelected = $derived(selectedRowIds.size > 0 && selectedRowIds.size < rows.length);

  // Toggle row selection
  function toggleRowSelection(rowId: string) {
    const newSelected = new Set(selectedRowIds);
    if (newSelected.has(rowId)) {
      newSelected.delete(rowId);
    } else {
      newSelected.add(rowId);
    }
    selectedRowIds = newSelected;
    notifySelectionChange();
  }

  // Toggle all rows selection
  function toggleAllSelection() {
    if (allSelected) {
      selectedRowIds = new Set();
    } else {
      selectedRowIds = new Set(rows.map(r => r.id));
    }
    notifySelectionChange();
  }

  // Notify parent of selection changes
  function notifySelectionChange() {
    if (onSelectionChange) {
      const selectedData = rows.filter(r => selectedRowIds.has(r.id)).map(r => r.data);
      onSelectionChange(selectedData);
    }
  }

  // Column Dependency Map
  let dependencyMap = $state<Record<string, GridColumn[]>>({});

  $effect(() => {
    const map: Record<string, GridColumn[]> = {};
    for (const col of columns) {
      if (col.logic && col.logic.dependencies) {
        for (const dep of col.logic.dependencies) {
          if (!map[dep]) map[dep] = [];
          map[dep].push(col);
        }
      }
    }
    dependencyMap = map;
  });

  async function executeColumnLogic(row: GridRow, column: GridColumn) {
    // New Calculation Expression
    if (column.calculationExpression) {
        const result = safeEvaluate(column.calculationExpression, {
            value: row.data[column.name],
            row: row.data,
            form: formValues,
            grids: gridsContext,
            process: processVariables,
            task: task,
            user: userContext
        });

        if (result !== undefined && row.data[column.name] !== result) {
            handleCellChange(row, column.name, result, true);
        }
    }
    // Legacy Logic
    else if (column.logic && column.logic.type !== 'None' && column.logic.content) {
      try {
        if (column.logic.type === 'JS') {
          const func = new Function('value', 'row', 'form', 'grids', 'db', 'lib', column.logic.content);

          const rowData = { ...row.data };
          const result = await func(
            row.data[column.name],
            rowData,
            formValues,
            gridsContext,
            {
              query: async (sql: string, params: any[]) => {
                console.log('SQL:', sql, params);
                return [];
              }
            },
            {}
          );

          if (result !== undefined && row.data[column.name] !== result) {
            handleCellChange(row, column.name, result, true);
          }
        }
      } catch (e) {
        console.error(`Error executing logic for column ${column.name}:`, e);
        row.errors[column.name] = `Logic Error: ${e.message}`;
      }
    }
  }

  // Re-run calculations when dependencies change (formValues or other columns)
  $effect(() => {
     if (formValues && rows.length > 0) {
         setTimeout(() => {
             rows.forEach(row => {
                 columns.forEach(col => {
                     if (col.calculationExpression) {
                         executeColumnLogic(row, col);
                     }
                 });
             });
         }, 0);
     }
  });

  function handleCellChange(row: GridRow, columnName: string, value: unknown, isAutomated = false) {
    // Avoid infinite loops
    if (row.data[columnName] === value) return;

    row.data[columnName] = value;

    if (!isAutomated) {
      userHasMadeChanges = true;
      // Trigger dependencies (row-level)
      // 1. Legacy deps
      const dependents = dependencyMap[columnName] || [];
      for (const dep of dependents) {
        if (dep.logic?.autoCalculate) {
          setTimeout(() => executeColumnLogic(row, dep), 0);
        }
      }
      // 2. New calc expressions - we check all columns in the row that have calc expressions
      columns.forEach(col => {
          if (col.calculationExpression && col.name !== columnName) {
              setTimeout(() => executeColumnLogic(row, col), 0);
          }
      });
    }
  }

  // Initialize rows from initialData when data becomes available
  $effect(() => {
    if (!dataLoadedFromProps && !userHasMadeChanges && initialData.length > 0) {
      rows = initialData.map((data) => ({
        id: crypto.randomUUID(),
        data: { ...data },
        isEditing: false,
        errors: {}
      }));
      dataLoadedFromProps = true;
    }
  });

  function notifyDataChange() {
    if (onDataChange) {
      onDataChange(rows.map((row) => row.data));
    }
  }

  function addRow() {
    if (maxRows > 0 && rows.length >= maxRows) {
      return;
    }

    const newRow: GridRow = {
      id: crypto.randomUUID(),
      data: columns.reduce(
        (acc, col) => {
          acc[col.name] = col.type === 'number' ? null : '';
          return acc;
        },
        {} as Record<string, unknown>
      ),
      isEditing: true,
      errors: {}
    };

    rows = [...rows, newRow];
    userHasMadeChanges = true;
    notifyDataChange();
  }

  function editRow(rowId: string) {
    rows = rows.map((row) => (row.id === rowId ? { ...row, isEditing: true, errors: {} } : row));
  }

  function validateColumn(column: GridColumn, value: unknown, row: GridRow): string | null {
    // Check required
    if (column.required && (value === undefined || value === null || value === '')) {
      return `${column.label} is required`;
    }

    // Skip validation if empty and not required
    if (value === undefined || value === null || value === '') {
      return null;
    }

    // Custom Validation
    if (column.validationExpression) {
        const isValidOrMsg = safeEvaluate(column.validationExpression, {
            value: value,
            row: row.data,
            form: formValues,
            grids: gridsContext,
            process: processVariables,
            task: task,
            user: userContext
        });

        if (isValidOrMsg === false) {
            return column.validationMessage || `${column.label} is invalid`;
        }
        if (typeof isValidOrMsg === 'string') {
            return isValidOrMsg;
        }
    }

    const validation = column.validation;
    if (!validation) return null;

    const strValue = String(value);

    // Min length
    if (validation.minLength && strValue.length < validation.minLength) {
      return `Min ${validation.minLength} characters`;
    }

    // Max length
    if (validation.maxLength && strValue.length > validation.maxLength) {
      return `Max ${validation.maxLength} characters`;
    }

    // Pattern
    if (validation.pattern) {
      try {
        const regex = new RegExp(validation.pattern);
        if (!regex.test(strValue)) {
          return validation.patternMessage || 'Invalid format';
        }
      } catch {
        // Invalid regex, skip
      }
    }

    // Numeric validations
    if (column.type === 'number') {
      const numValue = Number(value);
      if (!isNaN(numValue)) {
        if (column.min !== undefined && numValue < column.min) {
          return `Min ${column.min}`;
        }
        if (column.max !== undefined && numValue > column.max) {
          return `Max ${column.max}`;
        }
      }
    }

    return null;
  }

  function validateRow(row: GridRow): boolean {
    const errors: Record<string, string | null> = {};
    let isValid = true;

    // Only validate visible columns
    for (const col of visibleColumns) {
      const error = validateColumn(col, row.data[col.name], row);
      if (error) {
        errors[col.name] = error;
        isValid = false;
      }
    }

    row.errors = errors;
    return isValid;
  }

  function saveRow(rowId: string) {
    const row = rows.find((r) => r.id === rowId);
    if (!row) return;

    if (!validateRow(row)) {
      rows = [...rows];
      return;
    }

    rows = rows.map((r) => (r.id === rowId ? { ...r, isEditing: false, errors: {} } : r));
    userHasMadeChanges = true;
    notifyDataChange();
  }

  function cancelEdit(rowId: string) {
    const rowIndex = rows.findIndex((r) => r.id === rowId);
    if (rowIndex === -1) return;

    const row = rows[rowIndex];

    // If this is a new row (all empty values), remove it
    const isEmpty = Object.values(row.data).every(
      (val) => val === '' || val === null || val === undefined
    );

    if (isEmpty) {
      rows = rows.filter((r) => r.id !== rowId);
      userHasMadeChanges = true;
      notifyDataChange();
    } else {
      rows = rows.map((r) => (r.id === rowId ? { ...r, isEditing: false, errors: {} } : r));
    }
  }

  function deleteRow(rowId: string) {
    if (minRows && rows.length <= minRows) {
      return;
    }

    rows = rows.filter((row) => row.id !== rowId);
    userHasMadeChanges = true;
    notifyDataChange();
  }

  function getRowValue(row: GridRow, columnName: string): string {
    const value = row.data[columnName];
    if (value === null || value === undefined) return '';
    return String(value);
  }

  function handleSelectChange(row: GridRow, columnName: string, value: string | undefined) {
    handleCellChange(row, columnName, value ?? '');
  }

  export function getData(): Record<string, unknown>[] {
    return rows.map((row) => row.data);
  }

  export function validate(): boolean {
    let isValid = true;
    for (const row of rows) {
      if (!validateRow(row)) {
        isValid = false;
      }
    }
    rows = [...rows];
    return isValid;
  }

  export function hasEditingRows(): boolean {
    return rows.some((row) => row.isEditing);
  }

  export function getSelectedRows(): Record<string, unknown>[] {
    return rows.filter(r => selectedRowIds.has(r.id)).map(r => r.data);
  }

  export function clearSelection(): void {
    selectedRowIds = new Set();
  }
</script>

<div class="w-full">
  {#if label}
    <div class="mb-4">
      <h3 class="text-lg font-semibold">{label}</h3>
      {#if description}
        <p class="text-sm text-muted-foreground mt-1">{description}</p>
      {/if}
    </div>
  {/if}

  <div class="rounded-md border overflow-x-auto">
    <div class="min-w-[800px]">
      <Table.Root>
        <Table.Header>
          <Table.Row>
            {#if enableMultiSelect}
              <Table.Head class="w-10">
                <input
                  type="checkbox"
                  checked={allSelected}
                  indeterminate={someSelected}
                  onchange={toggleAllSelection}
                  class="h-4 w-4 rounded border-gray-300 text-blue-600 focus:ring-blue-500"
                  aria-label="Select all rows"
                />
              </Table.Head>
            {/if}
            {#each visibleColumns as column}
              <Table.Head>
                {column.label}
                {#if column.required}
                  <span class="text-destructive">*</span>
                {/if}
                {#if isColumnReadonly(column) && !readonly}
                  <span class="text-muted-foreground text-xs ml-1">(read-only)</span>
                {/if}
              </Table.Head>
            {/each}
            {#if !readonly}
              <Table.Head class="text-right">Actions</Table.Head>
            {/if}
          </Table.Row>
        </Table.Header>
        <Table.Body>
          {#if rows.length === 0}
            <Table.Row>
              <Table.Cell
                colspan={visibleColumns.length + (readonly ? 0 : 1)}
                class="text-center py-8 text-muted-foreground"
              >
                No rows added yet. {#if !readonly}Click "Add Row" to get started.{/if}
              </Table.Cell>
            </Table.Row>
          {/if}

          {#each rows as row (row.id)}
            <Table.Row class={selectedRowIds.has(row.id) ? 'bg-blue-50' : ''}>
              {#if enableMultiSelect}
                <Table.Cell class="w-10">
                  <input
                    type="checkbox"
                    checked={selectedRowIds.has(row.id)}
                    onchange={() => toggleRowSelection(row.id)}
                    class="h-4 w-4 rounded border-gray-300 text-blue-600 focus:ring-blue-500"
                    aria-label="Select row"
                  />
                </Table.Cell>
              {/if}
              {#each visibleColumns as column}
                {@const colReadonly = isColumnReadonly(column)}
                <Table.Cell>
                  {#if row.isEditing && !readonly && !colReadonly}
                    <div class="space-y-1">
                      {#if column.type === 'select'}
                        <Select.Root
                          type="single"
                          value={String(row.data[column.name] ?? '')}
                          onValueChange={(v) => handleSelectChange(row, column.name, v)}
                        >
                          <Select.Trigger
                            class={row.errors[column.name] ? 'border-destructive' : ''}
                          >
                            <span class="block truncate">
                              {row.data[column.name] || 'Select...'}
                            </span>
                          </Select.Trigger>
                          <Select.Content>
                            {#each column.options || [] as option}
                              <Select.Item value={option} label={option} />
                            {/each}
                          </Select.Content>
                        </Select.Root>
                      {:else if column.type === 'textarea'}
                        <Textarea
                          bind:value={
                            () => String(row.data[column.name] ?? ''),
                            (v) => handleCellChange(row, column.name, v)
                          }
                          rows={2}
                          placeholder={column.placeholder}
                          class={row.errors[column.name] ? 'border-destructive' : ''}
                        />
                      {:else if column.type === 'number'}
                        <Input
                          type="number"
                          value={row.data[column.name] !== null &&
                          row.data[column.name] !== undefined
                            ? String(row.data[column.name])
                            : ''}
                          oninput={(e) => {
                            const val = e.currentTarget.value;
                            handleCellChange(row, column.name, val === '' ? null : Number(val));
                          }}
                          min={column.min}
                          max={column.max}
                          step={column.step}
                          placeholder={column.placeholder}
                          class={row.errors[column.name] ? 'border-destructive' : ''}
                        />
                      {:else if column.type === 'date'}
                        <Input
                          type="date"
                          value={String(row.data[column.name] ?? '')}
                          oninput={(e) => handleCellChange(row, column.name, e.currentTarget.value)}
                          class={row.errors[column.name] ? 'border-destructive' : ''}
                        />
                      {:else}
                        <Input
                          type="text"
                          value={String(row.data[column.name] ?? '')}
                          oninput={(e) => handleCellChange(row, column.name, e.currentTarget.value)}
                          placeholder={column.placeholder}
                          class={row.errors[column.name] ? 'border-destructive' : ''}
                        />
                      {/if}
                      {#if row.errors[column.name]}
                        <p class="text-xs text-destructive">{row.errors[column.name]}</p>
                      {/if}
                    </div>
                  {:else}
                    <span class="text-sm {colReadonly ? 'text-muted-foreground' : ''}">
                      {getRowValue(row, column.name) || '-'}
                    </span>
                  {/if}
                </Table.Cell>
              {/each}
              {#if !readonly}
                <Table.Cell class="text-right">
                  <div class="flex justify-end gap-2">
                    {#if row.isEditing}
                      <Button
                        variant="ghost"
                        size="sm"
                        onclick={() => saveRow(row.id)}
                        class="text-green-600 hover:text-green-700 hover:bg-green-50"
                      >
                        Save
                      </Button>
                      <Button variant="ghost" size="sm" onclick={() => cancelEdit(row.id)}>
                        Cancel
                      </Button>
                    {:else}
                      <Button variant="ghost" size="sm" onclick={() => editRow(row.id)}>Edit</Button
                      >
                      <Button
                        variant="ghost"
                        size="sm"
                        onclick={() => deleteRow(row.id)}
                        disabled={minRows > 0 && rows.length <= minRows}
                        class="text-destructive hover:text-destructive hover:bg-destructive/10"
                      >
                        Delete
                      </Button>
                    {/if}
                  </div>
                </Table.Cell>
              {/if}
            </Table.Row>
          {/each}
        </Table.Body>
      </Table.Root>
    </div>
  </div>

  {#if !readonly}
    <div class="mt-4">
      <Button variant="outline" onclick={addRow} disabled={maxRows > 0 && rows.length >= maxRows}>
        <Plus class="h-4 w-4 mr-2" />
        Add Row
        {#if maxRows > 0}
          <span class="ml-2 text-xs text-muted-foreground">({rows.length}/{maxRows})</span>
        {/if}
      </Button>
    </div>
  {/if}
</div>
