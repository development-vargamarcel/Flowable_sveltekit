<script lang="ts">
	import type {
		FieldConditionRule,
		ConditionEffect,
		ConditionTargetType,
		ConditionTarget,
		FormField,
		FormGrid
	} from '$lib/types';
	import { generateRuleId } from '$lib/utils/condition-state-computer';

	interface Props {
		rule: FieldConditionRule | null;
		availableFields: FormField[];
		availableGrids: FormGrid[];
		onSave: (rule: FieldConditionRule) => void;
		onCancel: () => void;
		onDelete?: (ruleId: string) => void;
	}

	const { rule, availableFields, availableGrids, onSave, onCancel, onDelete }: Props = $props();

	// Form state
	let name = $state(rule?.name ?? 'New Rule');
	let description = $state(rule?.description ?? '');
	let enabled = $state(rule?.enabled ?? true);
	let priority = $state(rule?.priority ?? 0);
	let ruleEffect = $state(rule?.effect ?? 'readonly');
	let targetType = $state(rule?.target.type ?? 'all');
	let selectedFields = $state(rule?.target.type === 'field' ? rule.target.fieldNames : []);
	let selectedGrids = $state(rule?.target.type === 'grid' ? rule.target.gridNames : []);
	
	const defaultColumns = rule?.target.type === 'column' ? rule.target.columnTargets : [];
	let selectedColumns = $state(defaultColumns);

	// Condition builder state
	let useSimpleBuilder = $state(true);
	let conditionExpression = $state(rule?.condition ?? '');

	// Simple builder fields
	let conditionField = $state('');
	let conditionOperator = $state('==');
	let conditionValue = $state('');
	let conditionLogic = $state('single');
	let secondConditionField = $state('');
	let secondConditionOperator = $state('==');
	let secondConditionValue = $state('');

	// Parse existing condition into simple builder format
	$effect(() => {
		if (rule?.condition) {
			parseConditionToBuilder(rule.condition);
		}
	});

	function parseConditionToBuilder(expr: string) {
		// Try to parse simple conditions
		let cleanExpr = expr.trim();
		if (cleanExpr.startsWith('${') && cleanExpr.endsWith('}')) {
			cleanExpr = cleanExpr.slice(2, -1).trim();
		}

		// Check for AND/OR
		if (cleanExpr.includes('&&')) {
			conditionLogic = 'and';
			const parts = cleanExpr.split('&&').map((p) => p.trim());
			if (parts.length === 2) {
				parseSimpleCondition(parts[0], 'first');
				parseSimpleCondition(parts[1], 'second');
				return;
			}
		} else if (cleanExpr.includes('||')) {
			conditionLogic = 'or';
			const parts = cleanExpr.split('||').map((p) => p.trim());
			if (parts.length === 2) {
				parseSimpleCondition(parts[0], 'first');
				parseSimpleCondition(parts[1], 'second');
				return;
			}
		}

		// Single condition
		conditionLogic = 'single';
		parseSimpleCondition(cleanExpr, 'first');
	}

	function parseSimpleCondition(expr: string, which: 'first' | 'second') {
		// Handle isEmpty/isNotEmpty
		const isEmptyMatch = expr.match(/^isEmpty\((\w+)\)$/);
		if (isEmptyMatch) {
			if (which === 'first') {
				conditionField = isEmptyMatch[1];
				conditionOperator = 'isEmpty';
				conditionValue = '';
			} else {
				secondConditionField = isEmptyMatch[1];
				secondConditionOperator = 'isEmpty';
				secondConditionValue = '';
			}
			return;
		}

		const isNotEmptyMatch = expr.match(/^isNotEmpty\((\w+)\)$/);
		if (isNotEmptyMatch) {
			if (which === 'first') {
				conditionField = isNotEmptyMatch[1];
				conditionOperator = 'isNotEmpty';
				conditionValue = '';
			} else {
				secondConditionField = isNotEmptyMatch[1];
				secondConditionOperator = 'isNotEmpty';
				secondConditionValue = '';
			}
			return;
		}

		// Handle comparison operators
		const operators = ['>=', '<=', '!=', '==', '>', '<'];
		for (const op of operators) {
			const idx = expr.indexOf(op);
			if (idx > 0) {
				const field = expr.slice(0, idx).trim();
				let value = expr.slice(idx + op.length).trim();

				// Remove quotes from string values
				if ((value.startsWith('"') && value.endsWith('"')) ||
				    (value.startsWith("'") && value.endsWith("'"))) {
					value = value.slice(1, -1);
				}

				if (which === 'first') {
					conditionField = field;
					conditionOperator = op as typeof conditionOperator;
					conditionValue = value;
				} else {
					secondConditionField = field;
					secondConditionOperator = op as typeof secondConditionOperator;
					secondConditionValue = value;
				}
				return;
			}
		}
	}

	function buildConditionExpression(): string {
		if (!useSimpleBuilder) {
			return conditionExpression;
		}

		const buildSingle = (
			field: string,
			operator: string,
			value: string
		): string => {
			if (!field) return '';

			if (operator === 'isEmpty') {
				return `isEmpty(${field})`;
			}
			if (operator === 'isNotEmpty') {
				return `isNotEmpty(${field})`;
			}

			// Determine if value is a number or string
			const numValue = parseFloat(value);
			const isNumber = !isNaN(numValue) && value === String(numValue);
			const formattedValue = isNumber ? value : `"${value}"`;

			return `${field} ${operator} ${formattedValue}`;
		};

		const first = buildSingle(conditionField, conditionOperator, conditionValue);
		if (!first) return '';

		if (conditionLogic === 'single') {
			return `\${${first}}`;
		}

		const second = buildSingle(
			secondConditionField,
			secondConditionOperator,
			secondConditionValue
		);
		if (!second) return `\${${first}}`;

		const logicOp = conditionLogic === 'and' ? '&&' : '||';
		return `\${${first} ${logicOp} ${second}}`;
	}

	function buildTarget(): ConditionTarget {
		switch (targetType) {
			case 'all':
				return { type: 'all' };
			case 'field':
				return { type: 'field', fieldNames: selectedFields };
			case 'grid':
				return { type: 'grid', gridNames: selectedGrids };
			case 'column':
				return { type: 'column', columnTargets: selectedColumns };
			default:
				return { type: 'all' };
		}
	}

	function handleSave() {
		const newRule: FieldConditionRule = {
			id: rule?.id ?? generateRuleId(),
			name,
			description,
			condition: buildConditionExpression(),
			effect: ruleEffect,
			target: buildTarget(),
			priority,
			enabled
		};
		onSave(newRule);
	}

	function toggleFieldSelection(fieldName: string) {
		if (selectedFields.includes(fieldName)) {
			selectedFields = selectedFields.filter((f: string) => f !== fieldName);
		} else {
			selectedFields = [...selectedFields, fieldName];
		}
	}

	function toggleGridSelection(gridName: string) {
		if (selectedGrids.includes(gridName)) {
			selectedGrids = selectedGrids.filter((g: string) => g !== gridName);
		} else {
			selectedGrids = [...selectedGrids, gridName];
		}
	}

	function toggleColumnSelection(gridName: string, columnName: string) {
		const existing = selectedColumns.find((c: { gridName: string }) => c.gridName === gridName);
		if (existing) {
			if (existing.columnNames.includes(columnName)) {
				existing.columnNames = existing.columnNames.filter((c: string) => c !== columnName);
				if (existing.columnNames.length === 0) {
					selectedColumns = selectedColumns.filter((c: { gridName: string }) => c.gridName !== gridName);
				} else {
					selectedColumns = [...selectedColumns];
				}
			} else {
				existing.columnNames = [...existing.columnNames, columnName];
				selectedColumns = [...selectedColumns];
			}
		} else {
			selectedColumns = [...selectedColumns, { gridName, columnNames: [columnName] }];
		}
	}

	function isColumnSelected(gridName: string, columnName: string): boolean {
		const gridTarget = selectedColumns.find((c: { gridName: string; columnNames: string[] }) => c.gridName === gridName);
		return gridTarget?.columnNames.includes(columnName) ?? false;
	}

	// Get all available variables for condition builder
	const allVariables = $derived([
		...availableFields.map((f) => ({ name: f.name, label: f.label, source: 'field' })),
		{ name: 'user.id', label: 'User ID', source: 'user' },
		{ name: 'user.username', label: 'Username', source: 'user' },
		{ name: 'user.roles', label: 'User Roles', source: 'user' },
		{ name: 'user.groups', label: 'User Groups', source: 'user' },
		{ name: 'process.initiator', label: 'Process Initiator', source: 'process' }
	]);
</script>

<div class="bg-white border rounded-lg shadow-sm">
	<div class="p-4 border-b bg-gray-50">
		<h3 class="text-lg font-semibold text-gray-900">
			{rule ? 'Edit Rule' : 'New Condition Rule'}
		</h3>
	</div>

	<div class="p-4 space-y-6">
		<!-- Basic Info -->
		<div class="grid grid-cols-2 gap-4">
			<div>
				<label for="ruleName" class="block text-sm font-medium text-gray-700 mb-1">Rule Name</label>
				<input
					id="ruleName"
					type="text"
					bind:value={name}
					class="w-full px-3 py-2 border border-gray-300 rounded-md focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
					placeholder="e.g., High Value Approval"
				/>
			</div>
			<div>
				<label for="priority" class="block text-sm font-medium text-gray-700 mb-1">Priority</label>
				<input
					id="priority"
					type="number"
					bind:value={priority}
					class="w-full px-3 py-2 border border-gray-300 rounded-md focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
					placeholder="Higher = evaluated first"
				/>
			</div>
		</div>

		<div>
			<label for="description" class="block text-sm font-medium text-gray-700 mb-1"
				>Description (optional)</label
			>
			<input
				id="description"
				type="text"
				bind:value={description}
				class="w-full px-3 py-2 border border-gray-300 rounded-md focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
				placeholder="Describe what this rule does"
			/>
		</div>

		<!-- Condition Builder -->
		<div class="border rounded-lg p-4 bg-gray-50">
			<div class="flex items-center justify-between mb-3">
				<h4 class="font-medium text-gray-900">When this condition is true:</h4>
				<label class="flex items-center gap-2 text-sm">
					<input type="checkbox" bind:checked={useSimpleBuilder} class="rounded" />
					<span>Use simple builder</span>
				</label>
			</div>

			{#if useSimpleBuilder}
				<!-- Simple Condition Builder -->
				<div class="space-y-3">
					<div class="flex flex-wrap items-center gap-2">
						<select
							bind:value={conditionField}
							class="px-3 py-2 border border-gray-300 rounded-md bg-white focus:ring-2 focus:ring-blue-500"
						>
							<option value="">Select field...</option>
							<optgroup label="Form Fields">
								{#each availableFields as field}
									<option value={field.name}>{field.label || field.name}</option>
								{/each}
							</optgroup>
							<optgroup label="User Context">
								<option value="user.id">User ID</option>
								<option value="user.username">Username</option>
								<option value="hasRole('...')">Has Role</option>
								<option value="hasGroup('...')">Has Group</option>
							</optgroup>
							<optgroup label="Process">
								<option value="process.initiator">Initiator</option>
							</optgroup>
						</select>

						<select
							bind:value={conditionOperator}
							class="px-3 py-2 border border-gray-300 rounded-md bg-white focus:ring-2 focus:ring-blue-500"
						>
							<option value="==">equals (==)</option>
							<option value="!=">not equals (!=)</option>
							<option value=">">greater than (&gt;)</option>
							<option value="<">less than (&lt;)</option>
							<option value=">=">greater or equal (&gt;=)</option>
							<option value="<=">less or equal (&lt;=)</option>
							<option value="isEmpty">is empty</option>
							<option value="isNotEmpty">is not empty</option>
						</select>

						{#if conditionOperator !== 'isEmpty' && conditionOperator !== 'isNotEmpty'}
							<input
								type="text"
								bind:value={conditionValue}
								class="px-3 py-2 border border-gray-300 rounded-md focus:ring-2 focus:ring-blue-500"
								placeholder="Value"
							/>
						{/if}
					</div>

					{#if conditionLogic !== 'single'}
						<div class="flex flex-wrap items-center gap-2">
							<span
								class="px-3 py-1 bg-blue-100 text-blue-800 rounded-full text-sm font-medium"
							>
								{conditionLogic.toUpperCase()}
							</span>

							<select
								bind:value={secondConditionField}
								class="px-3 py-2 border border-gray-300 rounded-md bg-white focus:ring-2 focus:ring-blue-500"
							>
								<option value="">Select field...</option>
								<optgroup label="Form Fields">
									{#each availableFields as field}
										<option value={field.name}>{field.label || field.name}</option>
									{/each}
								</optgroup>
								<optgroup label="User Context">
									<option value="user.id">User ID</option>
									<option value="user.username">Username</option>
								</optgroup>
							</select>

							<select
								bind:value={secondConditionOperator}
								class="px-3 py-2 border border-gray-300 rounded-md bg-white focus:ring-2 focus:ring-blue-500"
							>
								<option value="==">equals (==)</option>
								<option value="!=">not equals (!=)</option>
								<option value=">">greater than (&gt;)</option>
								<option value="<">less than (&lt;)</option>
								<option value=">=">greater or equal (&gt;=)</option>
								<option value="<=">less or equal (&lt;=)</option>
								<option value="isEmpty">is empty</option>
								<option value="isNotEmpty">is not empty</option>
							</select>

							{#if secondConditionOperator !== 'isEmpty' && secondConditionOperator !== 'isNotEmpty'}
								<input
									type="text"
									bind:value={secondConditionValue}
									class="px-3 py-2 border border-gray-300 rounded-md focus:ring-2 focus:ring-blue-500"
									placeholder="Value"
								/>
							{/if}
						</div>
					{/if}

					<div class="flex gap-2">
						<button
							type="button"
							onclick={() => (conditionLogic = 'single')}
							class="px-3 py-1 text-sm rounded-md {conditionLogic === 'single'
								? 'bg-blue-600 text-white'
								: 'bg-gray-200 text-gray-700 hover:bg-gray-300'}"
						>
							Single
						</button>
						<button
							type="button"
							onclick={() => (conditionLogic = 'and')}
							class="px-3 py-1 text-sm rounded-md {conditionLogic === 'and'
								? 'bg-blue-600 text-white'
								: 'bg-gray-200 text-gray-700 hover:bg-gray-300'}"
						>
							AND
						</button>
						<button
							type="button"
							onclick={() => (conditionLogic = 'or')}
							class="px-3 py-1 text-sm rounded-md {conditionLogic === 'or'
								? 'bg-blue-600 text-white'
								: 'bg-gray-200 text-gray-700 hover:bg-gray-300'}"
						>
							OR
						</button>
					</div>
				</div>
			{:else}
				<!-- Expression Editor -->
				<div>
					<textarea
						bind:value={conditionExpression}
						class="w-full px-3 py-2 border border-gray-300 rounded-md font-mono text-sm focus:ring-2 focus:ring-blue-500"
						rows="3"
						placeholder={'${amount > 1000 && status == \'pending\'}'}
					></textarea>
					<p class="mt-1 text-xs text-gray-500">
						Available: form fields, user.id, user.username, user.roles, user.groups,
						process.initiator, hasRole('...'), hasGroup('...')
					</p>
				</div>
			{/if}

			{#if useSimpleBuilder}
				<div class="mt-3 p-2 bg-white border rounded text-sm font-mono text-gray-600">
					Preview: {buildConditionExpression() || '(no condition set)'}
				</div>
			{/if}
		</div>

		<!-- Effect -->
		<div class="border rounded-lg p-4">
			<h4 class="font-medium text-gray-900 mb-3">Then apply:</h4>
			<div class="flex flex-wrap gap-3">
				<label
					class="flex items-center gap-2 px-4 py-2 border rounded-lg cursor-pointer hover:bg-gray-50 {ruleEffect ===
					'hidden'
						? 'border-blue-500 bg-blue-50'
						: ''}"
				>
					<input type="radio" name="effect" value="hidden" bind:group={ruleEffect} class="sr-only" />
					<svg class="w-5 h-5 text-gray-600" fill="none" viewBox="0 0 24 24" stroke="currentColor">
						<path
							stroke-linecap="round"
							stroke-linejoin="round"
							stroke-width="2"
							d="M13.875 18.825A10.05 10.05 0 0112 19c-4.478 0-8.268-2.943-9.543-7a9.97 9.97 0 011.563-3.029m5.858.908a3 3 0 114.243 4.243M9.878 9.878l4.242 4.242M9.88 9.88l-3.29-3.29m7.532 7.532l3.29 3.29M3 3l3.59 3.59m0 0A9.953 9.953 0 0112 5c4.478 0 8.268 2.943 9.543 7a10.025 10.025 0 01-4.132 5.411m0 0L21 21"
						/>
					</svg>
					<span>Hide</span>
				</label>

				<label
					class="flex items-center gap-2 px-4 py-2 border rounded-lg cursor-pointer hover:bg-gray-50 {ruleEffect ===
					'visible'
						? 'border-blue-500 bg-blue-50'
						: ''}"
				>
					<input type="radio" name="effect" value="visible" bind:group={ruleEffect} class="sr-only" />
					<svg class="w-5 h-5 text-gray-600" fill="none" viewBox="0 0 24 24" stroke="currentColor">
						<path
							stroke-linecap="round"
							stroke-linejoin="round"
							stroke-width="2"
							d="M15 12a3 3 0 11-6 0 3 3 0 016 0z"
						/>
						<path
							stroke-linecap="round"
							stroke-linejoin="round"
							stroke-width="2"
							d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z"
						/>
					</svg>
					<span>Show</span>
				</label>

				<label
					class="flex items-center gap-2 px-4 py-2 border rounded-lg cursor-pointer hover:bg-gray-50 {ruleEffect ===
					'readonly'
						? 'border-blue-500 bg-blue-50'
						: ''}"
				>
					<input type="radio" name="effect" value="readonly" bind:group={ruleEffect} class="sr-only" />
					<svg class="w-5 h-5 text-gray-600" fill="none" viewBox="0 0 24 24" stroke="currentColor">
						<path
							stroke-linecap="round"
							stroke-linejoin="round"
							stroke-width="2"
							d="M12 15v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2zm10-10V7a4 4 0 00-8 0v4h8z"
						/>
					</svg>
					<span>Read-Only</span>
				</label>

				<label
					class="flex items-center gap-2 px-4 py-2 border rounded-lg cursor-pointer hover:bg-gray-50 {ruleEffect ===
					'editable'
						? 'border-blue-500 bg-blue-50'
						: ''}"
				>
					<input type="radio" name="effect" value="editable" bind:group={ruleEffect} class="sr-only" />
					<svg class="w-5 h-5 text-gray-600" fill="none" viewBox="0 0 24 24" stroke="currentColor">
						<path
							stroke-linecap="round"
							stroke-linejoin="round"
							stroke-width="2"
							d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z"
						/>
					</svg>
					<span>Editable</span>
				</label>
			</div>
		</div>

		<!-- Target -->
		<div class="border rounded-lg p-4">
			<h4 class="font-medium text-gray-900 mb-3">To:</h4>

			<div class="flex flex-wrap gap-2 mb-4">
				<button
					type="button"
					onclick={() => (targetType = 'all')}
					class="px-4 py-2 rounded-md text-sm {targetType === 'all'
						? 'bg-blue-600 text-white'
						: 'bg-gray-100 text-gray-700 hover:bg-gray-200'}"
				>
					All Fields & Grids
				</button>
				<button
					type="button"
					onclick={() => (targetType = 'field')}
					class="px-4 py-2 rounded-md text-sm {targetType === 'field'
						? 'bg-blue-600 text-white'
						: 'bg-gray-100 text-gray-700 hover:bg-gray-200'}"
				>
					Specific Fields
				</button>
				<button
					type="button"
					onclick={() => (targetType = 'grid')}
					class="px-4 py-2 rounded-md text-sm {targetType === 'grid'
						? 'bg-blue-600 text-white'
						: 'bg-gray-100 text-gray-700 hover:bg-gray-200'}"
				>
					Specific Grids
				</button>
				<button
					type="button"
					onclick={() => (targetType = 'column')}
					class="px-4 py-2 rounded-md text-sm {targetType === 'column'
						? 'bg-blue-600 text-white'
						: 'bg-gray-100 text-gray-700 hover:bg-gray-200'}"
				>
					Specific Columns
				</button>
			</div>

			{#if targetType === 'field'}
				<div class="flex flex-wrap gap-2">
					{#each availableFields as field}
						<label
							class="flex items-center gap-2 px-3 py-1 border rounded-lg cursor-pointer hover:bg-gray-50 {selectedFields.includes(
								field.name
							)
								? 'border-blue-500 bg-blue-50'
								: ''}"
						>
							<input
								type="checkbox"
								checked={selectedFields.includes(field.name)}
								onchange={() => toggleFieldSelection(field.name)}
								class="rounded"
							/>
							<span class="text-sm">{field.label || field.name}</span>
						</label>
					{/each}
					{#if availableFields.length === 0}
						<p class="text-sm text-gray-500 italic">No fields available</p>
					{/if}
				</div>
			{:else if targetType === 'grid'}
				<div class="flex flex-wrap gap-2">
					{#each availableGrids as grid}
						<label
							class="flex items-center gap-2 px-3 py-1 border rounded-lg cursor-pointer hover:bg-gray-50 {selectedGrids.includes(
								grid.name
							)
								? 'border-blue-500 bg-blue-50'
								: ''}"
						>
							<input
								type="checkbox"
								checked={selectedGrids.includes(grid.name)}
								onchange={() => toggleGridSelection(grid.name)}
								class="rounded"
							/>
							<span class="text-sm">{grid.label || grid.name}</span>
						</label>
					{/each}
					{#if availableGrids.length === 0}
						<p class="text-sm text-gray-500 italic">No grids available</p>
					{/if}
				</div>
			{:else if targetType === 'column'}
				<div class="space-y-3">
					{#each availableGrids as grid}
						<div class="border rounded p-3">
							<h5 class="font-medium text-sm text-gray-700 mb-2">{grid.label || grid.name}</h5>
							<div class="flex flex-wrap gap-2">
								{#each grid.columns as column}
									<label
										class="flex items-center gap-2 px-3 py-1 border rounded-lg cursor-pointer hover:bg-gray-50 {isColumnSelected(
											grid.name,
											column.name
										)
											? 'border-blue-500 bg-blue-50'
											: ''}"
									>
										<input
											type="checkbox"
											checked={isColumnSelected(grid.name, column.name)}
											onchange={() => toggleColumnSelection(grid.name, column.name)}
											class="rounded"
										/>
										<span class="text-sm">{column.label || column.name}</span>
									</label>
								{/each}
							</div>
						</div>
					{/each}
					{#if availableGrids.length === 0}
						<p class="text-sm text-gray-500 italic">No grids available</p>
					{/if}
				</div>
			{/if}
		</div>

		<!-- Enabled Toggle -->
		<div class="flex items-center gap-2">
			<input type="checkbox" id="enabled" bind:checked={enabled} class="rounded" />
			<label for="enabled" class="text-sm font-medium text-gray-700">Rule is enabled</label>
		</div>
	</div>

	<!-- Footer -->
	<div class="px-4 py-3 border-t bg-gray-50 flex justify-between">
		<div>
			{#if rule && onDelete}
				<button
					type="button"
					onclick={() => onDelete(rule.id)}
					class="px-4 py-2 text-red-600 hover:text-red-700 hover:bg-red-50 rounded-md"
				>
					Delete Rule
				</button>
			{/if}
		</div>
		<div class="flex gap-3">
			<button
				type="button"
				onclick={onCancel}
				class="px-4 py-2 text-gray-700 hover:bg-gray-100 rounded-md"
			>
				Cancel
			</button>
			<button
				type="button"
				onclick={handleSave}
				class="px-4 py-2 bg-blue-600 text-white rounded-md hover:bg-blue-700"
			>
				{rule ? 'Save Changes' : 'Create Rule'}
			</button>
		</div>
	</div>
</div>
