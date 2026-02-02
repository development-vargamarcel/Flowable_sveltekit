<script lang="ts">
    import type { FormField } from '$lib/types';

    interface Props {
        field: FormField;
        value: unknown;
    }

    const { field, value }: Props = $props();
</script>

<div class="px-3 py-2 bg-gray-100 border border-gray-300 rounded-md text-sm text-gray-600">
    {value ?? field.defaultExpression ?? '-'}
</div>
