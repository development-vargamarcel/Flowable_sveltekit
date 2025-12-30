<script lang="ts">
	import type { FieldConditionRule, FormField, FormGrid } from '$lib/types';
	import ConditionRuleEditor from './ConditionRuleEditor.svelte';
	import Modal from './Modal.svelte';

	interface Props {
		rules: FieldConditionRule[];
		availableFields: FormField[];
		availableGrids: FormGrid[];
		onChange: (rules: FieldConditionRule[]) => void;
		title?: string;
		description?: string;
	}

	const {
		rules,
		availableFields,
		availableGrids,
		onChange,
		title = 'Condition Rules',
		description = 'Rules are evaluated in priority order. "Least access wins" - if multiple rules apply, the most restrictive takes effect.'
	}: Props = $props();

	let editingRule = $state<FieldConditionRule | null>(null);
	let isCreating = $state(false);
	let showEditor = $state(false);

	function handleAddRule() {
		isCreating = true;
		editingRule = null;
		showEditor = true;
	}

	function handleEditRule(rule: FieldConditionRule) {
		isCreating = false;
		editingRule = rule;
		showEditor = true;
	}

	function handleSaveRule(rule: FieldConditionRule) {
		if (isCreating) {
			onChange([...rules, rule]);
		} else {
			onChange(rules.map((r) => (r.id === rule.id ? rule : r)));
		}
		showEditor = false;
		editingRule = null;
	}

	function handleDeleteRule(ruleId: string) {
		onChange(rules.filter((r) => r.id !== ruleId));
		showEditor = false;
		editingRule = null;
	}

	function handleToggleEnabled(rule: FieldConditionRule) {
		onChange(rules.map((r) => (r.id === rule.id ? { ...r, enabled: !r.enabled } : r)));
	}

	function handleCloseEditor() {
		showEditor = false;
		editingRule = null;
	}

	function moveRuleUp(index: number) {
		if (index <= 0) return;
		const newRules = [...rules];
		[newRules[index - 1], newRules[index]] = [newRules[index], newRules[index - 1]];
		// Update priorities based on position
		newRules.forEach((r, i) => (r.priority = newRules.length - i));
		onChange(newRules);
	}

	function moveRuleDown(index: number) {
		if (index >= rules.length - 1) return;
		const newRules = [...rules];
		[newRules[index], newRules[index + 1]] = [newRules[index + 1], newRules[index]];
		// Update priorities based on position
		newRules.forEach((r, i) => (r.priority = newRules.length - i));
		onChange(newRules);
	}

	function getEffectLabel(effect: string): string {
		switch (effect) {
			case 'hidden':
				return 'Hide';
			case 'visible':
				return 'Show';
			case 'readonly':
				return 'Read-Only';
			case 'editable':
				return 'Editable';
			default:
				return effect;
		}
	}

	function getEffectColor(effect: string): string {
		switch (effect) {
			case 'hidden':
				return 'bg-red-100 text-red-800';
			case 'visible':
				return 'bg-green-100 text-green-800';
			case 'readonly':
				return 'bg-yellow-100 text-yellow-800';
			case 'editable':
				return 'bg-blue-100 text-blue-800';
			default:
				return 'bg-gray-100 text-gray-800';
		}
	}

	function getTargetLabel(rule: FieldConditionRule): string {
		switch (rule.target.type) {
			case 'all':
				return 'All fields & grids';
			case 'field':
				return `Fields: ${rule.target.fieldNames?.join(', ') || 'none'}`;
			case 'grid':
				return `Grids: ${rule.target.gridNames?.join(', ') || 'none'}`;
			case 'column':
				return `Columns: ${rule.target.columnTargets?.map((t) => `${t.gridName}.${t.columnNames.join(', ')}`).join('; ') || 'none'}`;
			default:
				return 'Unknown';
		}
	}

	function formatCondition(condition: string): string {
		// Clean up the condition for display
		let clean = condition.trim();
		if (clean.startsWith('${') && clean.endsWith('}')) {
			clean = clean.slice(2, -1).trim();
		}
		return clean.length > 50 ? clean.slice(0, 47) + '...' : clean;
	}

	// Sort rules by priority (higher first)
	const sortedRules = $derived([...rules].sort((a, b) => b.priority - a.priority));
</script>

<div class="border rounded-lg bg-white">
	<div class="p-4 border-b bg-gray-50 flex items-center justify-between">
		<div>
			<h3 class="text-lg font-semibold text-gray-900">{title}</h3>
			{#if description}
				<p class="text-sm text-gray-500 mt-1">{description}</p>
			{/if}
		</div>
		<button
			type="button"
			onclick={handleAddRule}
			class="flex items-center gap-2 px-4 py-2 bg-blue-600 text-white rounded-md hover:bg-blue-700"
		>
			<svg class="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
				<path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4" />
			</svg>
			Add Rule
		</button>
	</div>

	{#if sortedRules.length === 0}
		<div class="p-8 text-center text-gray-500">
			<svg
				class="w-12 h-12 mx-auto mb-4 text-gray-300"
				fill="none"
				viewBox="0 0 24 24"
				stroke="currentColor"
			>
				<path
					stroke-linecap="round"
					stroke-linejoin="round"
					stroke-width="2"
					d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2"
				/>
			</svg>
			<p>No condition rules defined yet.</p>
			<p class="text-sm mt-1">Add a rule to control field visibility and editability.</p>
		</div>
	{:else}
		<ul class="divide-y">
			{#each sortedRules as rule, index}
				<li class="p-4 hover:bg-gray-50 {!rule.enabled ? 'opacity-50' : ''}">
					<div class="flex items-start gap-4">
						<!-- Reorder buttons -->
						<div class="flex flex-col gap-1">
							<button
								type="button"
								onclick={() => moveRuleUp(index)}
								disabled={index === 0}
								class="p-1 text-gray-400 hover:text-gray-600 disabled:opacity-30"
								title="Move up"
							>
								<svg class="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
									<path
										stroke-linecap="round"
										stroke-linejoin="round"
										stroke-width="2"
										d="M5 15l7-7 7 7"
									/>
								</svg>
							</button>
							<button
								type="button"
								onclick={() => moveRuleDown(index)}
								disabled={index === sortedRules.length - 1}
								class="p-1 text-gray-400 hover:text-gray-600 disabled:opacity-30"
								title="Move down"
							>
								<svg class="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
									<path
										stroke-linecap="round"
										stroke-linejoin="round"
										stroke-width="2"
										d="M19 9l-7 7-7-7"
									/>
								</svg>
							</button>
						</div>

						<!-- Rule content -->
						<div class="flex-1 min-w-0">
							<div class="flex items-center gap-3 mb-1">
								<h4 class="font-medium text-gray-900">{rule.name}</h4>
								<span class="px-2 py-0.5 text-xs rounded-full {getEffectColor(rule.effect)}">
									{getEffectLabel(rule.effect)}
								</span>
								{#if !rule.enabled}
									<span class="px-2 py-0.5 text-xs rounded-full bg-gray-200 text-gray-600">
										Disabled
									</span>
								{/if}
							</div>

							<div class="text-sm text-gray-600 space-y-1">
								<p>
									<span class="text-gray-400">When:</span>
									<code class="bg-gray-100 px-1 rounded">{formatCondition(rule.condition)}</code>
								</p>
								<p>
									<span class="text-gray-400">Target:</span>
									{getTargetLabel(rule)}
								</p>
								{#if rule.description}
									<p class="text-gray-500 italic">{rule.description}</p>
								{/if}
							</div>
						</div>

						<!-- Actions -->
						<div class="flex items-center gap-2">
							<button
								type="button"
								onclick={() => handleToggleEnabled(rule)}
								class="p-2 text-gray-400 hover:text-gray-600 rounded"
								title={rule.enabled ? 'Disable rule' : 'Enable rule'}
							>
								{#if rule.enabled}
									<svg class="w-5 h-5 text-green-500" fill="none" viewBox="0 0 24 24" stroke="currentColor">
										<path
											stroke-linecap="round"
											stroke-linejoin="round"
											stroke-width="2"
											d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z"
										/>
									</svg>
								{:else}
									<svg class="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
										<path
											stroke-linecap="round"
											stroke-linejoin="round"
											stroke-width="2"
											d="M10 14l2-2m0 0l2-2m-2 2l-2-2m2 2l2 2m7-2a9 9 0 11-18 0 9 9 0 0118 0z"
										/>
									</svg>
								{/if}
							</button>
							<button
								type="button"
								onclick={() => handleEditRule(rule)}
								class="p-2 text-gray-400 hover:text-blue-600 rounded"
								title="Edit rule"
							>
								<svg class="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
									<path
										stroke-linecap="round"
										stroke-linejoin="round"
										stroke-width="2"
										d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z"
									/>
								</svg>
							</button>
						</div>
					</div>
				</li>
			{/each}
		</ul>
	{/if}
</div>

<Modal open={showEditor} title={isCreating ? 'New Condition Rule' : 'Edit Condition Rule'} onClose={handleCloseEditor} maxWidth="xl">
	<ConditionRuleEditor
		rule={editingRule}
		{availableFields}
		{availableGrids}
		onSave={handleSaveRule}
		onCancel={handleCloseEditor}
		onDelete={isCreating ? undefined : handleDeleteRule}
	/>
</Modal>
