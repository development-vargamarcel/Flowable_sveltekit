<script lang="ts">
  import { Select as SelectPrimitive } from 'bits-ui';
  import type { Snippet } from 'svelte';
  import { cn } from '$lib/utils';
  import { Check } from '@lucide/svelte';

  interface Props {
    value: string;
    label?: string;
    disabled?: boolean;
    class?: string;
    children?: Snippet;
  }

  const { value, label, disabled = false, class: className, children }: Props = $props();
</script>

<SelectPrimitive.Item
  {value}
  {label}
  {disabled}
  class={cn(
    'focus:bg-accent focus:text-accent-foreground relative flex w-full cursor-default select-none items-center rounded-sm py-1.5 pl-8 pr-2 text-sm outline-none data-[disabled]:pointer-events-none data-[disabled]:opacity-50',
    className
  )}
>
  <span class="absolute left-2 flex h-3.5 w-3.5 items-center justify-center">
    <SelectPrimitive.ItemIndicator>
      <Check class="h-4 w-4" />
    </SelectPrimitive.ItemIndicator>
  </span>
  {#if children}
    {@render children()}
  {:else}
    {label || value}
  {/if}
</SelectPrimitive.Item>
