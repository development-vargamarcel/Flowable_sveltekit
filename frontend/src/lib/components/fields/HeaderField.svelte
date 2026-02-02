<script lang="ts">
    import type { FormField } from '$lib/types';

    interface Props {
        field: FormField;
    }

    const { field }: Props = $props();
</script>

<h3 class={field.cssClass ? '' : 'text-lg font-semibold text-gray-900 border-b pb-2 mb-2'}>
    {field.label}
</h3>
