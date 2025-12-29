<script lang="ts">
  import type { Snippet } from 'svelte';
  import type { HTMLAttributes } from 'svelte/elements';
  import { cn } from '$lib/utils';

  interface Props extends HTMLAttributes<HTMLTableRowElement> {
    children?: Snippet;
    class?: string;
  }

  // eslint-disable-next-line svelte/valid-compile
  const { class: className, children, ...restProps }: Props = $props();
</script>

<tr
  class={cn(
    'hover:bg-muted/50 data-[state=selected]:bg-muted border-b transition-colors',
    className
  )}
  {...restProps}
>
  {#if children}
    {@render children()}
  {/if}
</tr>
