<script lang="ts">
    import type { FormField } from '$lib/types';

    interface Props {
        field: FormField;
        isReadonly: boolean;
        error?: string;
        onchange: (value: string) => void;
    }

    const { field, isReadonly, error, onchange }: Props = $props();
</script>

<input
    id={`field-${field.name}`}
    type="file"
    onchange={(e) => {
        const file = e.currentTarget.files?.[0];
        onchange(file?.name || '');
    }}
    disabled={isReadonly}
    class="w-full px-3 py-2 border rounded-md focus:ring-2 focus:ring-blue-500 text-sm
        {error ? 'border-red-500' : 'border-gray-300'}
        {isReadonly ? 'bg-gray-50' : ''}"
/>
