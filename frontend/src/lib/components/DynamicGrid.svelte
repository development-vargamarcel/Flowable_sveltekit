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
		onDataChange?: (data: Record<string, unknown>[]) => void;
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
		onDataChange
	}: Props = $props();

	// Helper to get column state (with fallback)
	function getColumnState(columnName: string): ComputedFieldState {
		return columnStates[columnName] || { isHidden: false, isReadonly: readonly, appliedRules: [] };
	}

	// Get visible columns (filter out hidden ones)
	const visibleColumns = $derived(columns.filter(col => !getColumnState(col.name).isHidden));

	// Check if a column is readonly (grid readonly OR column-specific readonly)
	function isColumnReadonly(columnName: string): boolean {
		if (readonly) return true;
		return getColumnState(columnName).isReadonly;
	}

	let rows = $state<GridRow[]>([]);
	let dataLoadedFromProps = $state(false);
	let userHasMadeChanges = $state(false);

	// Initialize rows from initialData when data becomes available
	// This handles both immediate and late-arriving data (e.g., when parent's formValues is populated asynchronously)
	$effect(() => {
		// Only load data from props once, when it first becomes available
		// Also skip if user has already made changes (prevents infinite loop when parent echoes back our changes)
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

	// Helper function to notify parent of data changes
	// Called explicitly after user actions to avoid reactive loops
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
			data: columns.reduce((acc, col) => {
				acc[col.name] = col.type === 'number' ? null : '';
				return acc;
			}, {} as Record<string, unknown>),
			isEditing: true,
			errors: {}
		};

		rows = [...rows, newRow];
		userHasMadeChanges = true;
		notifyDataChange();
	}

	function editRow(rowId: string) {
		rows = rows.map((row) =>
			row.id === rowId ? { ...row, isEditing: true, errors: {} } : row
		);
	}

	function validateColumn(column: GridColumn, value: unknown): string | null {
		// Check required
		if (column.required && (value === undefined || value === null || value === '')) {
			return `${column.label} is required`;
		}

		// Skip validation if empty and not required
		if (value === undefined || value === null || value === '') {
			return null;
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
			const error = validateColumn(col, row.data[col.name]);
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
		row.data[columnName] = value ?? '';
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

	<div class="rounded-md border">
		<Table.Root>
			<Table.Header>
				<Table.Row>
					{#each visibleColumns as column}
						<Table.Head>
							{column.label}
							{#if column.required}
								<span class="text-destructive">*</span>
							{/if}
							{#if isColumnReadonly(column.name) && !readonly}
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
						<Table.Cell colspan={visibleColumns.length + (readonly ? 0 : 1)} class="text-center py-8 text-muted-foreground">
							No rows added yet. {#if !readonly}Click "Add Row" to get started.{/if}
						</Table.Cell>
					</Table.Row>
				{/if}

				{#each rows as row (row.id)}
					<Table.Row>
						{#each visibleColumns as column}
							{@const colReadonly = isColumnReadonly(column.name)}
							<Table.Cell>
								{#if row.isEditing && !readonly && !colReadonly}
									<div class="space-y-1">
										{#if column.type === 'select'}
											<Select.Root
												type="single"
												value={String(row.data[column.name] ?? '')}
												onValueChange={(v) => handleSelectChange(row, column.name, v)}
											>
												<Select.Trigger class={row.errors[column.name] ? 'border-destructive' : ''}>
													<Select.Value placeholder="Select..." />
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
													(v) => (row.data[column.name] = v)
												}
												rows={2}
												placeholder={column.placeholder}
												class={row.errors[column.name] ? 'border-destructive' : ''}
											/>
										{:else if column.type === 'number'}
											<Input
												type="number"
												value={row.data[column.name] !== null && row.data[column.name] !== undefined ? String(row.data[column.name]) : ''}
												oninput={(e) => {
													const val = e.currentTarget.value;
													row.data[column.name] = val === '' ? null : Number(val);
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
												oninput={(e) => (row.data[column.name] = e.currentTarget.value)}
												class={row.errors[column.name] ? 'border-destructive' : ''}
											/>
										{:else}
											<Input
												type="text"
												value={String(row.data[column.name] ?? '')}
												oninput={(e) => (row.data[column.name] = e.currentTarget.value)}
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
										<Button
											variant="ghost"
											size="sm"
											onclick={() => cancelEdit(row.id)}
										>
											Cancel
										</Button>
									{:else}
										<Button
											variant="ghost"
											size="sm"
											onclick={() => editRow(row.id)}
										>
											Edit
										</Button>
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

	{#if !readonly}
		<div class="mt-4">
			<Button
				variant="outline"
				onclick={addRow}
				disabled={maxRows > 0 && rows.length >= maxRows}
			>
				<Plus class="h-4 w-4 mr-2" />
				Add Row
				{#if maxRows > 0}
					<span class="ml-2 text-xs text-muted-foreground">({rows.length}/{maxRows})</span>
				{/if}
			</Button>
		</div>
	{/if}
</div>
