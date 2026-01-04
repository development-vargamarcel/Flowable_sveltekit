<script lang="ts">
  import { Checkbox as CheckboxPrimitive } from 'bits-ui';
  import { Check, Minus } from '@lucide/svelte';
  import { cn } from '$lib/utils';

  interface Props {
    checked?: boolean | 'indeterminate';
    disabled?: boolean;
    required?: boolean;
    name?: string;
    value?: string;
    class?: string;
    onCheckedChange?: (checked: boolean | 'indeterminate') => void;
  }

  /* eslint-disable prefer-const */
  let {
    checked = $bindable(false),
    disabled = false,
    required = false,
    name,
    value,
    class: className,
    onCheckedChange
  }: Props = $props();
  /* eslint-enable prefer-const */

  // Convert indeterminate to boolean for the primitive component
  const primitiveChecked = $derived(checked === 'indeterminate' ? false : checked);
  const isIndeterminate = $derived(checked === 'indeterminate');

  function handleCheckedChange(newChecked: boolean | 'indeterminate') {
    if (isIndeterminate) {
      checked = true;
    } else {
      checked = newChecked as boolean;
    }
    onCheckedChange?.(checked);
  }
</script>

<CheckboxPrimitive.Root
  checked={primitiveChecked}
  {disabled}
  {required}
  {name}
  {value}
  onCheckedChange={handleCheckedChange}
  class={cn(
    'border-primary ring-offset-background focus-visible:ring-ring data-[state=checked]:bg-primary data-[state=checked]:text-primary-foreground peer h-4 w-4 shrink-0 rounded-sm border focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-offset-2 disabled:cursor-not-allowed disabled:opacity-50',
    isIndeterminate ? 'data-[state=unchecked]:bg-muted' : '',
    className
  )}
>
  {#snippet children({ checked: isChecked })}
    <div class="flex items-center justify-center text-current">
      {#if isIndeterminate}
        <Minus class="h-3.5 w-3.5" />
      {:else if isChecked}
        <Check class="h-3.5 w-3.5" />
      {/if}
    </div>
  {/snippet}
</CheckboxPrimitive.Root>
