<script lang="ts">
  import type { GridColumn, ComputedFieldState } from '$lib/types';
  import * as Table from '$lib/components/ui/table';
  import { Button } from '$lib/components/ui/button';
  import { Input } from '$lib/components/ui/input';
  import { Textarea } from '$lib/components/ui/textarea';
  import * as Select from '$lib/components/ui/select';
  import { Plus, ChevronLeft, ChevronRight, ArrowUp, ArrowDown, ChevronsUpDown, MoreHorizontal } from '@lucide/svelte';

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

    // New Props
    enablePagination?: boolean;
    pageSize?: number;
    enableSorting?: boolean;
    enableGrouping?: boolean;
    groupByColumn?: string;
    enableRowActions?: boolean;
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
    onSelectionChange,

    // Defaults
    enablePagination = false,
    pageSize = 10,
    enableSorting = false,
    enableGrouping = false,
    groupByColumn = '',
    enableRowActions = false
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

    if (column.hiddenExpression) {
        const isHidden = safeEvaluate(column.hiddenExpression, {
            value: null,
            row: {},
            form: formValues,
            grids: gridsContext,
            process: processVariables,
            task: task,
            user: userContext
        });

        if (isHidden === true) {
            state = { ...state, isHidden: true };
        }
    }

    if (column.readonlyExpression) {
        const isReadonly = safeEvaluate(column.readonlyExpression, {
            value: rowData ? rowData[column.name] : null,
            row: rowData || {},
            form: formValues,
            grids: gridsContext,
            process: processVariables,
            task: task,
            user: userContext
        });

        if (isReadonly === true) {
            state = { ...state, isReadonly: true };
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

  // Pagination State
  let currentPage = $state(1);
  const currentPageSize = $derived(pageSize);

  // Sorting State
  let sortColumn = $state<string | null>(null);
  let sortDirection = $state<'asc' | 'desc'>('asc');

  // Toggle sorting
  function toggleSort(columnName: string) {
      if (!enableSorting) return;

      if (sortColumn === columnName) {
          if (sortDirection === 'asc') {
              sortDirection = 'desc';
          } else {
              sortColumn = null; // Clear sort
              sortDirection = 'asc';
          }
      } else {
          sortColumn = columnName;
          sortDirection = 'asc';
      }
  }

  // Derived: Processed Rows (Sorted & Grouped logic preparation)
  // Note: We don't implement full grouping rendering in the table body yet (complex layout change),
  // but we can sort by group column first to simulate grouping structure.
  const processedRows = $derived.by(() => {
      const result = [...rows];

      // 1. Grouping (Implicit Sort)
      if (enableGrouping && groupByColumn) {
          // If sorting is active, we let explicit sort take precedence or secondary?
          // Usually grouping forces primary sort on group column.
          result.sort((a, b) => {
              const valA = String(a.data[groupByColumn] || '');
              const valB = String(b.data[groupByColumn] || '');
              return valA.localeCompare(valB);
          });
      }

      // 2. Sorting (Explicit)
      if (sortColumn) {
          result.sort((a, b) => {
              const valA = a.data[sortColumn!] as any;
              const valB = b.data[sortColumn!] as any;

              if (valA === valB) return 0;

              // Handle numbers
              if (typeof valA === 'number' && typeof valB === 'number') {
                  return sortDirection === 'asc' ? valA - valB : valB - valA;
              }

              // Handle strings
              const strA = String(valA || '');
              const strB = String(valB || '');
              return sortDirection === 'asc' ? strA.localeCompare(strB) : strB.localeCompare(strA);
          });
      }

      return result;
  });

  // Derived: Paginated Rows
  const paginatedRows = $derived.by(() => {
      if (!enablePagination) return processedRows;

      const startIndex = (currentPage - 1) * currentPageSize;
      return processedRows.slice(startIndex, startIndex + currentPageSize);
  });

  const totalPages = $derived(enablePagination ? Math.ceil(processedRows.length / currentPageSize) : 1);

  // Check if all rows (on current page) are selected
  const allPageSelected = $derived.by(() => {
      if (paginatedRows.length === 0) return false;
      return paginatedRows.every(r => selectedRowIds.has(r.id));
  });

  const somePageSelected = $derived.by(() => {
      if (paginatedRows.length === 0) return false;
      return paginatedRows.some(r => selectedRowIds.has(r.id)) && !allPageSelected;
  });

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

  // Toggle all rows on current page selection
  function toggleAllPageSelection() {
    const newSelected = new Set(selectedRowIds);

    if (allPageSelected) {
        // Deselect all on this page
        paginatedRows.forEach(r => newSelected.delete(r.id));
    } else {
        // Select all on this page
        paginatedRows.forEach(r => newSelected.add(r.id));
    }

    selectedRowIds = newSelected;
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

    // Jump to last page if pagination enabled
    if (enablePagination) {
        currentPage = Math.ceil(rows.length / currentPageSize) || 1;
    }

    userHasMadeChanges = true;
    notifyDataChange();
  }

  function editRow(rowId: string) {
    rows = rows.map((row) => (row.id === rowId ? { ...row, isEditing: true, errors: {} } : row));
  }

  function validateColumn(column: GridColumn, value: unknown, row: GridRow): string | null {
    // Check required (static or dynamic)
    let isRequired = column.required;

    if (column.requiredExpression) {
        const requiredResult = safeEvaluate(column.requiredExpression, {
            value: value,
            row: row.data,
            form: formValues,
            grids: gridsContext,
            process: processVariables,
            task: task,
            user: userContext
        });
        if (requiredResult === true) {
            isRequired = true;
        }
    }

    if (isRequired && (value === undefined || value === null || value === '')) {
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

  function _handleImport(event: Event) {
      // Mock import for now - would normally parse CSV
      const input = event.target as HTMLInputElement;
      if (input.files?.length) {
          alert(`Import simulated: ${input.files[0].name}`);
      }
  }

  function _handleExport() {
      // Mock export
      const csvContent = "data:text/csv;charset=utf-8,"
          + columns.map(c => c.label).join(",") + "\n"
          + rows.map(r => columns.map(c => r.data[c.name]).join(",")).join("\n");
      const encodedUri = encodeURI(csvContent);
      const link = document.createElement("a");
      link.setAttribute("href", encodedUri);
      link.setAttribute("download", "grid_data.csv");
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
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
    <div class="mb-4 flex items-center justify-between">
      <div>
          <h3 class="text-lg font-semibold">{label}</h3>
          {#if description}
            <p class="text-sm text-muted-foreground mt-1">{description}</p>
          {/if}
      </div>
      {#if gridsContext?.enableImportExport || true /* Assuming prop for now, logic below */}
          <div class="flex gap-2">
              <!-- Import/Export Buttons Placeholder if enabled -->
          </div>
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
                  checked={allPageSelected}
                  indeterminate={somePageSelected}
                  onchange={toggleAllPageSelection}
                  class="h-4 w-4 rounded border-gray-300 text-blue-600 focus:ring-blue-500"
                  aria-label="Select all rows on page"
                />
              </Table.Head>
            {/if}
            {#each visibleColumns as column}
              <Table.Head>
                <div class="flex items-center gap-1">
                    {#if enableSorting}
                        <button
                            onclick={() => toggleSort(column.name)}
                            class="flex items-center hover:text-blue-600 focus:outline-none"
                        >
                            {column.label}
                            <span class="ml-1">
                                {#if sortColumn === column.name}
                                    {#if sortDirection === 'asc'}
                                        <ArrowUp class="w-3 h-3" />
                                    {:else}
                                        <ArrowDown class="w-3 h-3" />
                                    {/if}
                                {:else}
                                    <ChevronsUpDown class="w-3 h-3 text-gray-300" />
                                {/if}
                            </span>
                        </button>
                    {:else}
                        {column.label}
                    {/if}

                    {#if column.required}
                      <span class="text-destructive">*</span>
                    {/if}
                    {#if isColumnReadonly(column) && !readonly}
                      <span class="text-muted-foreground text-xs ml-1">(read-only)</span>
                    {/if}
                </div>
              </Table.Head>
            {/each}
            {#if !readonly || enableRowActions}
              <Table.Head class="text-right">Actions</Table.Head>
            {/if}
          </Table.Row>
        </Table.Header>
        <Table.Body>
          {#if paginatedRows.length === 0}
            <Table.Row>
              <Table.Cell
                colspan={visibleColumns.length + (readonly && !enableRowActions ? 0 : 1) + (enableMultiSelect ? 1 : 0)}
                class="text-center py-8 text-muted-foreground"
              >
                {#if rows.length === 0}
                    No rows added yet. {#if !readonly}Click "Add Row" to get started.{/if}
                {:else}
                    No matches found on this page.
                {/if}
              </Table.Cell>
            </Table.Row>
          {/if}

          {#each paginatedRows as row, i (row.id)}
            {#if enableGrouping && groupByColumn && (i === 0 || processedRows[processedRows.indexOf(row) - 1]?.data[groupByColumn] !== row.data[groupByColumn])}
                <!-- Group Header -->
                <Table.Row class="bg-gray-100">
                    <Table.Cell colspan={visibleColumns.length + (readonly && !enableRowActions ? 0 : 1) + (enableMultiSelect ? 1 : 0)} class="font-bold text-gray-700 py-1">
                        {row.data[groupByColumn] || 'Uncategorized'}
                    </Table.Cell>
                </Table.Row>
            {/if}

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
                              <Select.Item value={option.value} label={option.label} />
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
              {#if !readonly || enableRowActions}
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
                      {#if !readonly}
                          <Button variant="ghost" size="sm" onclick={() => editRow(row.id)}>Edit</Button>
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
                      {#if enableRowActions}
                          <Button variant="ghost" size="icon" title="More Actions">
                              <MoreHorizontal class="h-4 w-4" />
                          </Button>
                      {/if}
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

  <div class="mt-4 flex items-center justify-between">
      <div>
          {#if !readonly}
            <Button variant="outline" onclick={addRow} disabled={maxRows > 0 && rows.length >= maxRows}>
                <Plus class="h-4 w-4 mr-2" />
                Add Row
                {#if maxRows > 0}
                <span class="ml-2 text-xs text-muted-foreground">({rows.length}/{maxRows})</span>
                {/if}
            </Button>
          {/if}
      </div>

      {#if enablePagination && totalPages > 1}
          <div class="flex items-center gap-2">
              <span class="text-sm text-gray-600">Page {currentPage} of {totalPages}</span>
              <div class="flex gap-1">
                  <Button
                    variant="outline"
                    size="icon"
                    disabled={currentPage === 1}
                    onclick={() => currentPage--}
                  >
                      <ChevronLeft class="h-4 w-4" />
                  </Button>
                  <Button
                    variant="outline"
                    size="icon"
                    disabled={currentPage === totalPages}
                    onclick={() => currentPage++}
                  >
                      <ChevronRight class="h-4 w-4" />
                  </Button>
              </div>
          </div>
      {/if}
  </div>
</div>
