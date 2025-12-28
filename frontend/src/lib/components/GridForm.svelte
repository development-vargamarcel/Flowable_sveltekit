<script lang="ts">
	import { validateField, type ValidationRule } from '$lib/utils/validation';

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

	// Initialize rows from initialData once
	$effect(() => {
		if (rows.length === 0 && initialData.length > 0) {
			rows = initialData.map((data) => ({
				id: crypto.randomUUID(),
				data: { ...data },
				isEditing: false,
				errors: {}
			}));
		}
	});

	$effect(() => {
		if (onDataChange) {
			onDataChange(rows.map((row) => row.data));
		}
	});

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
		} else {
			rows = rows.map((r) => (r.id === rowId ? { ...r, isEditing: false, errors: {} } : r));
		}
	}

	function deleteRow(rowId: string) {
		if (minRows && rows.length <= minRows) {
			return;
		}

		rows = rows.filter((row) => row.id !== rowId);
	}

	function getRowValue(row: GridRow, columnName: string): string {
		const value = row.data[columnName];
		if (value === null || value === undefined) return '';
		return String(value);
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

<div class="grid-form">
	{#if label}
		<div class="mb-4">
			<h3 class="text-lg font-semibold text-gray-900">{label}</h3>
			{#if description}
				<p class="text-sm text-gray-600 mt-1">{description}</p>
			{/if}
		</div>
	{/if}

	<div class="overflow-x-auto">
		<table class="min-w-full divide-y divide-gray-200 border border-gray-200 rounded-lg">
			<thead class="bg-gray-50">
				<tr>
					{#each columns as column}
						<th
							class="px-4 py-3 text-left text-xs font-medium text-gray-700 uppercase tracking-wider"
						>
							{column.label}
							{#if column.validation?.some((rule) => rule.message.includes('required'))}
								<span class="text-red-500">*</span>
							{/if}
						</th>
					{/each}
					<th class="px-4 py-3 text-right text-xs font-medium text-gray-700 uppercase tracking-wider">
						Actions
					</th>
				</tr>
			</thead>
			<tbody class="bg-white divide-y divide-gray-200">
				{#if rows.length === 0}
					<tr>
						<td colspan={columns.length + 1} class="px-4 py-8 text-center text-gray-500">
							No rows added yet. Click "Add Row" to get started.
						</td>
					</tr>
				{/if}

				{#each rows as row (row.id)}
					<tr class="hover:bg-gray-50">
						{#each columns as column}
							<td class="px-4 py-3">
								{#if row.isEditing}
									<div>
										{#if column.type === 'select'}
											<select
												bind:value={row.data[column.name]}
												class="w-full px-3 py-2 border rounded-md focus:ring-2 focus:ring-blue-500 text-sm {row
													.errors[column.name]
													? 'border-red-500'
													: 'border-gray-300'}"
											>
												<option value="">Select...</option>
												{#each column.options || [] as option}
													<option value={option}>{option}</option>
												{/each}
											</select>
										{:else if column.type === 'textarea'}
											<textarea
												bind:value={row.data[column.name]}
												rows="2"
												placeholder={column.placeholder}
												class="w-full px-3 py-2 border rounded-md focus:ring-2 focus:ring-blue-500 text-sm {row
													.errors[column.name]
													? 'border-red-500'
													: 'border-gray-300'}"
											></textarea>
										{:else if column.type === 'number'}
											<input
												type="number"
												bind:value={row.data[column.name]}
												min={column.min}
												max={column.max}
												step={column.step}
												placeholder={column.placeholder}
												class="w-full px-3 py-2 border rounded-md focus:ring-2 focus:ring-blue-500 text-sm {row
													.errors[column.name]
													? 'border-red-500'
													: 'border-gray-300'}"
											/>
										{:else if column.type === 'date'}
											<input
												type="date"
												bind:value={row.data[column.name]}
												class="w-full px-3 py-2 border rounded-md focus:ring-2 focus:ring-blue-500 text-sm {row
													.errors[column.name]
													? 'border-red-500'
													: 'border-gray-300'}"
											/>
										{:else}
											<input
												type="text"
												bind:value={row.data[column.name]}
												placeholder={column.placeholder}
												class="w-full px-3 py-2 border rounded-md focus:ring-2 focus:ring-blue-500 text-sm {row
													.errors[column.name]
													? 'border-red-500'
													: 'border-gray-300'}"
											/>
										{/if}
										{#if row.errors[column.name]}
											<p class="text-xs text-red-600 mt-1">{row.errors[column.name]}</p>
										{/if}
									</div>
								{:else}
									<div class="text-sm text-gray-900">
										{getRowValue(row, column.name) || '-'}
									</div>
								{/if}
							</td>
						{/each}
						<td class="px-4 py-3">
							<div class="flex justify-end space-x-2">
								{#if row.isEditing}
									<button
										type="button"
										onclick={() => saveRow(row.id)}
										class="text-green-600 hover:text-green-800 text-sm font-medium"
										title="Save"
									>
										Save
									</button>
									<button
										type="button"
										onclick={() => cancelEdit(row.id)}
										class="text-gray-600 hover:text-gray-800 text-sm font-medium"
										title="Cancel"
									>
										Cancel
									</button>
								{:else}
									<button
										type="button"
										onclick={() => editRow(row.id)}
										class="text-blue-600 hover:text-blue-800 text-sm font-medium"
										title="Edit"
									>
										Edit
									</button>
									<button
										type="button"
										onclick={() => deleteRow(row.id)}
										disabled={minRows > 0 && rows.length <= minRows}
										class="text-red-600 hover:text-red-800 text-sm font-medium disabled:text-gray-400 disabled:cursor-not-allowed"
										title="Delete"
									>
										Delete
									</button>
								{/if}
							</div>
						</td>
					</tr>
				{/each}
			</tbody>
		</table>
	</div>

	<div class="mt-4">
		<button
			type="button"
			onclick={addRow}
			disabled={maxRows !== undefined && rows.length >= maxRows}
			class="inline-flex items-center px-4 py-2 border border-gray-300 rounded-md shadow-sm text-sm font-medium text-gray-700 bg-white hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500 disabled:bg-gray-100 disabled:text-gray-400 disabled:cursor-not-allowed"
		>
			<svg class="w-4 h-4 mr-2" fill="none" viewBox="0 0 24 24" stroke="currentColor">
				<path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4" />
			</svg>
			Add Row
			{#if maxRows}
				<span class="ml-2 text-xs text-gray-500">({rows.length}/{maxRows})</span>
			{/if}
		</button>
	</div>
</div>

<style>
	.grid-form {
		@apply w-full;
	}
</style>
