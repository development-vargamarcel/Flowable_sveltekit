<script lang="ts">
    import type { FormField } from '$lib/types';

    interface Props {
        field: FormField;
        value: unknown;
        isReadonly: boolean;
        error?: string;
        onchange: (value: unknown) => void;
    }

    const { field, value, isReadonly, error, onchange }: Props = $props();

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
</script>

{#if field.type === 'currency'}
    <div class="relative">
        <span class="absolute left-3 top-1/2 transform -translate-y-1/2 text-gray-500">$</span>
        <input
            id={`field-${field.name}`}
            type="number"
            value={formatCurrency(value)}
            oninput={(e) => onchange(e.currentTarget.valueAsNumber)}
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
            oninput={(e) => onchange(e.currentTarget.valueAsNumber)}
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
{:else}
    <input
        id={`field-${field.name}`}
        type={getInputType(field.type)}
        value={value ?? ''}
        oninput={(e) => {
            const inputType = getInputType(field.type);
            if (inputType === 'number') {
                onchange(e.currentTarget.valueAsNumber);
            } else {
                onchange(e.currentTarget.value);
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
