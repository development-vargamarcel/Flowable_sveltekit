<script lang="ts">
    import type { FormField } from '$lib/types';

    interface Props {
        field: FormField;
        value: unknown;
        isReadonly: boolean;
        onchange: (value: boolean) => void;
    }

    const { field, value, isReadonly, onchange }: Props = $props();
</script>

<label class="flex items-center space-x-2 cursor-pointer">
    <input
        type="checkbox"
        checked={Boolean(value)}
        onchange={(e) => onchange(e.currentTarget.checked)}
        disabled={isReadonly}
        class="h-4 w-4 rounded border-gray-300 text-blue-600 focus:ring-blue-500 disabled:bg-gray-100"
    />
    <span class="text-sm font-medium text-gray-700">
        {field.label}
        {#if field.required}<span class="text-red-500">*</span>{/if}
    </span>
</label>
{#if field.tooltip}
    <p class="text-xs text-gray-500 mt-1">{field.tooltip}</p>
{/if}
