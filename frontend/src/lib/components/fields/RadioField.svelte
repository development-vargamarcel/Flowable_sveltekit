<script lang="ts">
    import type { FormField } from '$lib/types';

    interface Props {
        field: FormField;
        value: unknown;
        isReadonly: boolean;
        onchange: (value: string) => void;
    }

    const { field, value, isReadonly, onchange }: Props = $props();
</script>

<div class="space-y-2">
    {#each field.options || [] as option}
        {@const optValue = typeof option === 'string' ? option : option.value}
        {@const optLabel = typeof option === 'string' ? option : option.label}
        <label class="flex items-center space-x-2 cursor-pointer">
            <input
                type="radio"
                name={field.name}
                value={optValue}
                checked={value === optValue}
                onchange={() => onchange(optValue)}
                disabled={isReadonly}
                class="h-4 w-4 border-gray-300 text-blue-600 focus:ring-blue-500"
            />
            <span class="text-sm text-gray-700">{optLabel}</span>
        </label>
    {/each}
</div>
