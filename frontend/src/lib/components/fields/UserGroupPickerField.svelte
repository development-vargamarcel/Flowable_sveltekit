<script lang="ts">
    import type { FormField } from '$lib/types';

    interface Props {
        field: FormField;
        value: unknown;
        isReadonly: boolean;
        error?: string;
        onchange: (value: string) => void;
    }

    const { field, value, isReadonly, error, onchange }: Props = $props();
</script>

<select
    id={`field-${field.name}`}
    value={value ?? ''}
    onchange={(e) => onchange(e.currentTarget.value)}
    disabled={isReadonly}
    class="w-full px-3 py-2 border rounded-md focus:ring-2 focus:ring-blue-500 text-sm
        {error ? 'border-red-500' : 'border-gray-300'}
        {isReadonly ? 'bg-gray-50' : ''}"
>
    <option value="">Select {field.type === 'userPicker' ? 'User' : 'Group'}...</option>
    <option value="user1">User 1</option>
    <option value="user2">User 2</option>
    <option value="manager">Manager</option>
    <option value="admin">Admin</option>
</select>
