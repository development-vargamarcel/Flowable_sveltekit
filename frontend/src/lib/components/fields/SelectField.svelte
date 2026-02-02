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
</script>

{#if field.type === 'multiselect'}
    <select
        id={`field-${field.name}`}
        multiple
        value={Array.isArray(value) ? value : []}
        onchange={(e) => {
            const selected = Array.from(e.currentTarget.selectedOptions).map(o => o.value);
            onchange(selected);
        }}
        disabled={isReadonly}
        class="w-full px-3 py-2 border rounded-md focus:ring-2 focus:ring-blue-500 text-sm
            {error ? 'border-red-500' : 'border-gray-300'}
            {isReadonly ? 'bg-gray-50' : ''}"
        size="4"
    >
        {#each field.options || [] as option}
            {#if typeof option === 'string'}
                <option value={option}>{option}</option>
            {:else}
                <option value={option.value}>{option.label}</option>
            {/if}
        {/each}
    </select>
{:else}
    <select
        id={`field-${field.name}`}
        value={value ?? ''}
        onchange={(e) => onchange(e.currentTarget.value)}
        disabled={isReadonly}
        class="w-full px-3 py-2 border rounded-md focus:ring-2 focus:ring-blue-500 text-sm
            {error ? 'border-red-500' : 'border-gray-300'}
            {isReadonly ? 'bg-gray-50' : ''}"
    >
        <option value="">{field.placeholder || 'Select...'}</option>
        {#each field.options || [] as option}
            {#if typeof option === 'string'}
                <option value={option}>{option}</option>
            {:else}
                <option value={option.value}>{option.label}</option>
            {/if}
        {/each}
    </select>
{/if}
