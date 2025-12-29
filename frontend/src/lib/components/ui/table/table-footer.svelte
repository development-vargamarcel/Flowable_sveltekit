<script lang="ts">
  import type { Snippet } from 'svelte';
  import type { HTMLAttributes } from 'svelte/elements';
  import { cn } from '$lib/utils';

  interface Props extends HTMLAttributes<HTMLTableSectionElement> {
    children?: Snippet;
    class?: string;
  }

  // eslint-disable-next-line svelte/valid-compile
  const { class: className, children, ...restProps }: Props = $props();
</script>

<tfoot
  class={cn('bg-muted/50 border-t font-medium [&>tr]:last:border-b-0', className)}
  {...restProps}
>
  {#if children}
    {@render children()}
  {/if}
</tfoot>
