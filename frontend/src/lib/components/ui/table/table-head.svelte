<script lang="ts">
  import type { Snippet } from 'svelte';
  import type { HTMLThAttributes } from 'svelte/elements';
  import { cn } from '$lib/utils';

  interface Props extends HTMLThAttributes {
    children?: Snippet;
    class?: string;
  }

  // eslint-disable-next-line svelte/valid-compile
  const { class: className, children, ...restProps }: Props = $props();
</script>

<th
  class={cn(
    'text-muted-foreground h-10 px-2 text-left align-middle font-medium [&:has([role=checkbox])]:pr-0 [&>[role=checkbox]]:translate-y-[2px]',
    className
  )}
  {...restProps}
>
  {#if children}
    {@render children()}
  {/if}
</th>
