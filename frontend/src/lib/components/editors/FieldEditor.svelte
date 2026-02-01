<script lang="ts">
	import type { FormField, ProcessFieldLibrary } from '$lib/types';
	import Modal from '../Modal.svelte';
	import CodeEditor from '../CodeEditor.svelte';

	interface Props {
		field: FormField | null;
		library: ProcessFieldLibrary;
		onSave: (field: FormField) => void;
		onClose: () => void;
		open: boolean;
	}

	const { field, library, onSave, onClose, open }: Props = $props();

	let fieldForm = $state({
		id: '',
		name: '',
		label: '',
		type: 'text' as FormField['type'],
		required: false,
		placeholder: '',
		defaultValue: '' as any,
		tooltip: '',
		readonly: false,
		hidden: false,
        richText: false,
        signature: false,
        pickerType: 'user' as 'user' | 'group',
		hiddenExpression: '',
		readonlyExpression: '',
		requiredExpression: '',
		calculationExpression: '',
		options: [] as { value: string; label: string }[],
		validation: {
			minLength: null as number | null,
			maxLength: null as number | null,
			min: null as number | null,
			max: null as number | null,
			pattern: '',
			patternMessage: '',
            allowedMimeTypes: [] as string[],
            maxFileSize: null as number | null
		}
	});

	const fieldTypes = [
		{ value: 'text', label: 'Text' },
		{ value: 'number', label: 'Number' },
		{ value: 'email', label: 'Email' },
		{ value: 'phone', label: 'Phone' },
		{ value: 'date', label: 'Date' },
		{ value: 'datetime', label: 'Date & Time' },
		{ value: 'select', label: 'Select' },
		{ value: 'multiselect', label: 'Multi-Select' },
		{ value: 'textarea', label: 'Text Area' },
		{ value: 'checkbox', label: 'Checkbox' },
		{ value: 'radio', label: 'Radio Buttons' },
		{ value: 'currency', label: 'Currency' },
		{ value: 'percentage', label: 'Percentage' },
		{ value: 'file', label: 'File Upload' },
        { value: 'image', label: 'Image' },
        { value: 'signature', label: 'Signature' },
        { value: 'userPicker', label: 'User Picker' },
        { value: 'groupPicker', label: 'Group Picker' },
		{ value: 'header', label: 'Section Header' }
	];

	function generateId(prefix: string): string {
		return `${prefix}_${Date.now()}_${Math.random().toString(36).substr(2, 6)}`;
	}

    function getCodeSuggestions() {
		const suggestions: Array<{ label: string; value: string; type: 'field' | 'grid' | 'column' | 'variable' | 'function'; description?: string }> = [];

		suggestions.push(
			{ label: 'value', value: 'value', type: 'variable', description: 'Current field value' },
			{ label: 'form', value: 'form', type: 'variable', description: 'All form fields' },
			{ label: 'grids', value: 'grids', type: 'variable', description: 'All grids data' },
			{ label: 'row', value: 'row', type: 'variable', description: 'Current grid row' }
		);

		library.fields.forEach((f: any) => {
			suggestions.push({ label: `form.${f.name}`, value: `form.${f.name}`, type: 'field', description: f.label });
		});

		library.grids.forEach((g: any) => {
			suggestions.push({ label: `grids.${g.name}`, value: `grids.${g.name}`, type: 'grid', description: g.label });
			suggestions.push({ label: `grids.${g.name}.selectedRow`, value: `grids.${g.name}.selectedRow`, type: 'grid', description: 'Selected row' });
			suggestions.push({ label: `grids.${g.name}.selectedRows`, value: `grids.${g.name}.selectedRows`, type: 'grid', description: 'Selected rows (array)' });
			suggestions.push({ label: `grids.${g.name}.selectedRows.length`, value: `grids.${g.name}.selectedRows.length`, type: 'grid', description: 'Count of selected' });
			suggestions.push({ label: `grids.${g.name}.rows`, value: `grids.${g.name}.rows`, type: 'grid', description: 'All rows' });
			suggestions.push({ label: `grids.${g.name}.sum`, value: `grids.${g.name}.sum('')`, type: 'function', description: 'Sum column' });

			g.columns.forEach((c: any) => {
				suggestions.push({ label: `row.${c.name}`, value: `row.${c.name}`, type: 'column', description: `${g.label} - ${c.label}` });
			});
		});

		return suggestions;
	}

	$effect(() => {
		if (field) {
			fieldForm = {
				id: field.id || '',
				name: field.name,
				label: field.label,
				type: field.type as typeof fieldForm.type,
				required: field.required || false,
				placeholder: field.placeholder || '',
				defaultValue: field.defaultValue || '',
				tooltip: field.tooltip || '',
				readonly: field.readonly || false,
				hidden: field.hidden || false,
                richText: field.richText || false,
                signature: field.signature || false,
                pickerType: (field.pickerType as 'user' | 'group') || 'user',
				hiddenExpression: field.hiddenExpression || '',
				readonlyExpression: field.readonlyExpression || '',
				requiredExpression: field.requiredExpression || '',
				calculationExpression: field.calculationExpression || '',
				options: (field.options || []).map(o => typeof o === 'string' ? { label: o, value: o } : o),
				validation: field.validation ? {
                    minLength: field.validation.minLength || null,
                    maxLength: field.validation.maxLength || null,
                    min: field.validation.min || null,
                    max: field.validation.max || null,
                    pattern: field.validation.pattern || '',
                    patternMessage: field.validation.patternMessage || '',
                    allowedMimeTypes: field.validation.allowedMimeTypes || [],
                    maxFileSize: field.validation.maxFileSize || null
                } : {
					minLength: null,
					maxLength: null,
					min: null,
					max: null,
					pattern: '',
					patternMessage: '',
                    allowedMimeTypes: [],
                    maxFileSize: null
				}
			};
		} else {
            fieldForm = {
                id: generateId('field'),
                name: '',
                label: '',
                type: 'text',
                required: false,
                placeholder: '',
                defaultValue: '',
                tooltip: '',
                readonly: false,
                hidden: false,
                richText: false,
                signature: false,
                pickerType: 'user',
                hiddenExpression: '',
                readonlyExpression: '',
                requiredExpression: '',
                calculationExpression: '',
                options: [],
                validation: {
                    minLength: null,
                    maxLength: null,
                    min: null,
                    max: null,
                    pattern: '',
                    patternMessage: '',
                    allowedMimeTypes: [],
                    maxFileSize: null
                }
            };
        }
	});

	function handleSave() {
		const newField: FormField = {
			id: fieldForm.id,
			name: fieldForm.name || fieldForm.label.toLowerCase().replace(/\s+/g, '_'),
			label: fieldForm.label,
			type: fieldForm.type,
			required: fieldForm.required,
			validation: {
				minLength: fieldForm.validation.minLength || undefined,
				maxLength: fieldForm.validation.maxLength || undefined,
				min: fieldForm.validation.min || undefined,
				max: fieldForm.validation.max || undefined,
				pattern: fieldForm.validation.pattern || undefined,
				patternMessage: fieldForm.validation.patternMessage || undefined,
                allowedMimeTypes: fieldForm.validation.allowedMimeTypes.length ? fieldForm.validation.allowedMimeTypes : undefined,
                maxFileSize: fieldForm.validation.maxFileSize || undefined
			},
			options: ['select', 'multiselect', 'radio'].includes(fieldForm.type) ? fieldForm.options : undefined,
			placeholder: fieldForm.placeholder,
			defaultValue: fieldForm.defaultValue,
			defaultExpression: '',
			tooltip: fieldForm.tooltip,
			readonly: fieldForm.readonly,
			hidden: fieldForm.hidden,
            richText: fieldForm.type === 'textarea' ? fieldForm.richText : undefined,
            signature: fieldForm.type === 'signature' ? true : undefined,
            pickerType: ['userPicker', 'groupPicker'].includes(fieldForm.type) ? (fieldForm.type === 'userPicker' ? 'user' : 'group') : undefined,
			hiddenExpression: fieldForm.hiddenExpression,
			readonlyExpression: fieldForm.readonlyExpression,
			requiredExpression: fieldForm.requiredExpression,
			calculationExpression: fieldForm.calculationExpression,
			gridColumn: 1,
			gridRow: library.fields.length + 1,
			gridWidth: 1,
			cssClass: ''
		};
		onSave(newField);
	}

	function addFieldOption() {
		fieldForm.options = [...fieldForm.options, { value: '', label: '' }];
	}

	function removeFieldOption(index: number) {
		fieldForm.options = fieldForm.options.filter((_, i) => i !== index);
	}
</script>

<Modal open={open} title={field ? 'Edit Field' : 'Add Field'} onClose={onClose} maxWidth="lg">
  <div class="space-y-6 max-h-[70vh] overflow-y-auto px-1">
    <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
      <div>
        <label for="f_name" class="block text-sm font-medium text-gray-700">Field ID (Name)</label>
        <input
          id="f_name"
          type="text"
          bind:value={fieldForm.name}
          class="mt-1 block w-full rounded-md border-gray-300 shadow-sm focus:border-blue-500 focus:ring-blue-500 sm:text-sm"
          placeholder="e.g. customerName"
        />
      </div>
      <div>
        <label for="f_label" class="block text-sm font-medium text-gray-700">Label</label>
        <input
          id="f_label"
          type="text"
          bind:value={fieldForm.label}
          class="mt-1 block w-full rounded-md border-gray-300 shadow-sm focus:border-blue-500 focus:ring-blue-500 sm:text-sm"
          placeholder="e.g. Customer Name"
        />
      </div>
      <div>
        <label for="f_type" class="block text-sm font-medium text-gray-700">Type</label>
        <select
          id="f_type"
          bind:value={fieldForm.type}
          class="mt-1 block w-full rounded-md border-gray-300 shadow-sm focus:border-blue-500 focus:ring-blue-500 sm:text-sm"
        >
          {#each fieldTypes as type}
            <option value={type.value}>{type.label}</option>
          {/each}
        </select>
      </div>
      <div>
        <label for="f_placeholder" class="block text-sm font-medium text-gray-700"
          >Placeholder</label
        >
        <input
          id="f_placeholder"
          type="text"
          bind:value={fieldForm.placeholder}
          class="mt-1 block w-full rounded-md border-gray-300 shadow-sm focus:border-blue-500 focus:ring-blue-500 sm:text-sm"
        />
      </div>
    </div>

    {#if fieldForm.type === 'textarea'}
         <div class="flex items-center mt-2">
            <input
              id="f_richtext"
              type="checkbox"
              bind:checked={fieldForm.richText}
              class="h-4 w-4 rounded border-gray-300 text-blue-600 focus:ring-blue-500"
            />
            <label for="f_richtext" class="ml-2 block text-sm text-gray-900">Enable Rich Text Editor</label>
          </div>
    {/if}

    {#if fieldForm.type === 'userPicker' || fieldForm.type === 'groupPicker'}
         <p class="text-xs text-gray-500">Selects {fieldForm.type === 'userPicker' ? 'a user' : 'a group'} from the system.</p>
    {/if}

    <!-- Advanced Behavior (Expressions) -->
    <div class="border rounded-md p-4 bg-purple-50 border-purple-100 mb-4">
      <h4 class="text-sm font-medium text-purple-900 mb-3">Dynamic Behavior (Expressions)</h4>
      <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
        <div>
          <label for="hiddenExpr" class="block text-xs font-medium text-gray-600">Hidden Expression</label>
          <input
            id="hiddenExpr"
            type="text"
            bind:value={fieldForm.hiddenExpression}
            placeholder="!form.showField"
            class="mt-1 w-full px-3 py-2 border border-gray-300 rounded-md focus:ring-2 focus:ring-purple-500 font-mono text-xs"
          />
        </div>
        <div>
          <label for="readonlyExpr" class="block text-xs font-medium text-gray-600">Readonly Expression</label>
          <input
            id="readonlyExpr"
            type="text"
            bind:value={fieldForm.readonlyExpression}
            placeholder="form.status === 'approved'"
            class="mt-1 w-full px-3 py-2 border border-gray-300 rounded-md focus:ring-2 focus:ring-purple-500 font-mono text-xs"
          />
        </div>
        <div>
          <label for="requiredExpr" class="block text-xs font-medium text-gray-600">Required Expression</label>
          <input
            id="requiredExpr"
            type="text"
            bind:value={fieldForm.requiredExpression}
            placeholder="form.amount > 1000"
            class="mt-1 w-full px-3 py-2 border border-gray-300 rounded-md focus:ring-2 focus:ring-purple-500 font-mono text-xs"
          />
        </div>
        <div>
          <label for="calcExpr" class="block text-xs font-medium text-gray-600">Calculation Expression</label>
          <input
            id="calcExpr"
            type="text"
            bind:value={fieldForm.calculationExpression}
            placeholder="form.price * form.quantity"
            class="mt-1 w-full px-3 py-2 border border-gray-300 rounded-md focus:ring-2 focus:ring-purple-500 font-mono text-xs"
          />
        </div>
      </div>
    </div>

    <!-- Options for Choice Types (Select, Radio, etc.) -->
    {#if ['select', 'multiselect', 'radio'].includes(fieldForm.type)}
      <div class="border rounded-md p-4 bg-gray-50">
        <span class="block text-sm font-medium text-gray-700 mb-2">Options</span>
        {#each fieldForm.options as option, i}
          <div class="flex gap-2 mb-2">
            <input
              type="text"
              bind:value={option.label}
              placeholder="Label"
              aria-label="Option label"
              class="flex-1 rounded-md border-gray-300 shadow-sm focus:border-blue-500 focus:ring-blue-500 sm:text-sm"
            />
            <input
              type="text"
              bind:value={option.value}
              placeholder="Value"
              aria-label="Option value"
              class="flex-1 rounded-md border-gray-300 shadow-sm focus:border-blue-500 focus:ring-blue-500 sm:text-sm"
            />
            <button
              onclick={() => removeFieldOption(i)}
              class="text-red-600 hover:text-red-800 text-sm"
            >
              ×
            </button>
          </div>
        {/each}
        <button
          onclick={addFieldOption}
          class="text-sm text-blue-600 hover:text-blue-800"
        >
          + Add Option
        </button>
      </div>
    {/if}

    <!-- Validation Rules -->
    <div class="border rounded-md p-4 bg-gray-50">
      <h4 class="text-sm font-medium text-gray-900 mb-3">Validation Rules</h4>
      <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
        <div class="flex items-center">
          <input
            id="f_required"
            type="checkbox"
            bind:checked={fieldForm.required}
            class="h-4 w-4 rounded border-gray-300 text-blue-600 focus:ring-blue-500"
          />
          <label for="f_required" class="ml-2 block text-sm text-gray-900">Required Field</label>
        </div>

        {#if ['text', 'textarea', 'email', 'password'].includes(fieldForm.type)}
          <div>
            <label for="f_minlen" class="block text-xs font-medium text-gray-500">Min Length</label>
            <input
              id="f_minlen"
              type="number"
              bind:value={fieldForm.validation.minLength}
              class="mt-1 block w-full rounded-md border-gray-300 shadow-sm focus:border-blue-500 focus:ring-blue-500 sm:text-sm"
            />
          </div>
          <div>
            <label for="f_maxlen" class="block text-xs font-medium text-gray-500">Max Length</label>
            <input
              id="f_maxlen"
              type="number"
              bind:value={fieldForm.validation.maxLength}
              class="mt-1 block w-full rounded-md border-gray-300 shadow-sm focus:border-blue-500 focus:ring-blue-500 sm:text-sm"
            />
          </div>
          <div class="col-span-2">
            <label for="f_pattern" class="block text-xs font-medium text-gray-500"
              >Regex Pattern</label
            >
            <input
              id="f_pattern"
              type="text"
              bind:value={fieldForm.validation.pattern}
              placeholder="e.g. ^[A-Z]+$"
              class="mt-1 block w-full rounded-md border-gray-300 shadow-sm focus:border-blue-500 focus:ring-blue-500 sm:text-sm"
            />
          </div>
          <div class="col-span-2">
            <label for="f_patmsg" class="block text-xs font-medium text-gray-500"
              >Pattern Error Message</label
            >
            <input
              id="f_patmsg"
              type="text"
              bind:value={fieldForm.validation.patternMessage}
              class="mt-1 block w-full rounded-md border-gray-300 shadow-sm focus:border-blue-500 focus:ring-blue-500 sm:text-sm"
            />
          </div>
        {/if}

        {#if ['number', 'currency', 'percentage', 'date', 'datetime'].includes(fieldForm.type)}
          <div>
            <label for="f_min" class="block text-xs font-medium text-gray-500">Minimum Value</label>
            <input
              id="f_min"
              type="number"
              bind:value={fieldForm.validation.min}
              class="mt-1 block w-full rounded-md border-gray-300 shadow-sm focus:border-blue-500 focus:ring-blue-500 sm:text-sm"
            />
          </div>
          <div>
            <label for="f_max" class="block text-xs font-medium text-gray-500">Maximum Value</label>
            <input
              id="f_max"
              type="number"
              bind:value={fieldForm.validation.max}
              class="mt-1 block w-full rounded-md border-gray-300 shadow-sm focus:border-blue-500 focus:ring-blue-500 sm:text-sm"
            />
          </div>
        {/if}

        {#if fieldForm.type === 'file' || fieldForm.type === 'image'}
            <div>
                 <label for="f_max_size" class="block text-xs font-medium text-gray-500">Max File Size (Bytes)</label>
                 <input
                  id="f_max_size"
                  type="number"
                  bind:value={fieldForm.validation.maxFileSize}
                  class="mt-1 block w-full rounded-md border-gray-300 shadow-sm focus:border-blue-500 focus:ring-blue-500 sm:text-sm"
                 />
            </div>
        {/if}
      </div>
    </div>

    <div class="flex items-center space-x-4">
      <div class="flex items-center">
        <input
          id="f_readonly"
          type="checkbox"
          bind:checked={fieldForm.readonly}
          class="h-4 w-4 rounded border-gray-300 text-blue-600 focus:ring-blue-500"
        />
        <label for="f_readonly" class="ml-2 block text-sm text-gray-900">Read-only</label>
      </div>
      <div class="flex items-center">
        <input
          id="f_hidden"
          type="checkbox"
          bind:checked={fieldForm.hidden}
          class="h-4 w-4 rounded border-gray-300 text-blue-600 focus:ring-blue-500"
        />
        <label for="f_hidden" class="ml-2 block text-sm text-gray-900">Hidden</label>
      </div>
    </div>
  </div>

  <div class="mt-5 sm:mt-6 sm:grid sm:grid-flow-row-dense sm:grid-cols-2 sm:gap-3">
    <button
      onclick={handleSave}
      class="inline-flex w-full justify-center rounded-md border border-transparent bg-blue-600 px-4 py-2 text-base font-medium text-white shadow-sm hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:ring-offset-2 sm:col-start-2 sm:text-sm"
    >
      Save Field
    </button>
    <button
      onclick={onClose}
      class="mt-3 inline-flex w-full justify-center rounded-md border border-gray-300 bg-white px-4 py-2 text-base font-medium text-gray-700 shadow-sm hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:ring-offset-2 sm:col-start-1 sm:mt-0 sm:text-sm"
    >
      Cancel
    </button>
  </div>
</Modal>
