<script lang="ts">
	import { validateField, type ValidationRule } from '$lib/utils/validation';
	import * as Table from '$lib/components/ui/table';
	import { Button } from '$lib/components/ui/button';
	import { Input } from '$lib/components/ui/input';
	import { Textarea } from '$lib/components/ui/textarea';
	import * as Select from '$lib/components/ui/select';
	import { Plus } from '@lucide/svelte';

	export interface GridColumn {
		name: string;
		label: string;
		type: 'text' | 'number' | 'date' | 'select' | 'textarea';
		validation?: ValidationRule[];
		options?: string[];
		placeholder?: string;
		min?: number;
		max?: number;
		step?: number;
	}

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
		onDataChange?: (data: Record<string, unknown>[]) => void;
	}

	const {
		columns,
		label = 'Grid Data',
		description,
		minRows = 0,
		maxRows,
		initialData = [],
		onDataChange
	}: Props = $props();

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
		if (maxRows && rows.length >= maxRows) {
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

	function validateRow(row: GridRow): boolean {
		const errors: Record<string, string | null> = {};
		let isValid = true;

		for (const col of columns) {
			if (col.validation) {
				const error = validateField(row.data[col.name], col.validation);
				if (error) {
					errors[col.name] = error;
					isValid = false;
				}
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

	export function validateAll(): boolean {
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
					{#each columns as column}
						<Table.Head>
							{column.label}
							{#if column.validation?.some((rule) => rule.message.includes('required'))}
								<span class="text-destructive">*</span>
							{/if}
						</Table.Head>
					{/each}
					<Table.Head class="text-right">Actions</Table.Head>
				</Table.Row>
			</Table.Header>
			<Table.Body>
				{#if rows.length === 0}
					<Table.Row>
						<Table.Cell colspan={columns.length + 1} class="text-center py-8 text-muted-foreground">
							No rows added yet. Click "Add Row" to get started.
						</Table.Cell>
					</Table.Row>
				{/if}

				{#each rows as row (row.id)}
					<Table.Row>
						{#each columns as column}
							<Table.Cell>
								{#if row.isEditing}
									<div class="space-y-1">
										{#if column.type === 'select'}
											<Select.Root
												type="single"
												value={String(row.data[column.name] ?? '')}
												onValueChange={(v) => handleSelectChange(row, column.name, v)}
											>
												<Select.Trigger class={row.errors[column.name] ? 'border-destructive' : ''}>
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
									<span class="text-sm">
										{getRowValue(row, column.name) || '-'}
									</span>
								{/if}
							</Table.Cell>
						{/each}
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
					</Table.Row>
				{/each}
			</Table.Body>
		</Table.Root>
	</div>

	<div class="mt-4">
		<Button
			variant="outline"
			onclick={addRow}
			disabled={maxRows !== undefined && rows.length >= maxRows}
		>
			<Plus class="h-4 w-4 mr-2" />
			Add Row
			{#if maxRows}
				<span class="ml-2 text-xs text-muted-foreground">({rows.length}/{maxRows})</span>
			{/if}
		</Button>
	</div>
</div>
