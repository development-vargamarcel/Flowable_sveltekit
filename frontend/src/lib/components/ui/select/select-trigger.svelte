<script lang="ts">
  import { Select as SelectPrimitive } from 'bits-ui';
  import type { Snippet } from 'svelte';
  import { cn } from '$lib/utils';
  import { ChevronDown } from '@lucide/svelte';

  interface Props {
    class?: string;
    children?: Snippet;
  }

  const { class: className, children }: Props = $props();
</script>

<SelectPrimitive.Trigger
  class={cn(
    'border-input bg-background ring-offset-background placeholder:text-muted-foreground focus:ring-ring flex h-10 w-full items-center justify-between rounded-md border px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-offset-2 disabled:cursor-not-allowed disabled:opacity-50 [&>span]:line-clamp-1',
    className
  )}
>
  {#if children}
    {@render children()}
  {/if}
  <ChevronDown class="h-4 w-4 opacity-50" />
</SelectPrimitive.Trigger>
