<script lang="ts">
  import type { Snippet } from 'svelte';
  import type { HTMLTdAttributes } from 'svelte/elements';
  import { cn } from '$lib/utils';

  interface Props extends HTMLTdAttributes {
    children?: Snippet;
    class?: string;
  }

  // eslint-disable-next-line svelte/valid-compile
  const { class: className, children, ...restProps }: Props = $props();
</script>

<td
  class={cn(
    'p-2 align-middle [&:has([role=checkbox])]:pr-0 [&>[role=checkbox]]:translate-y-[2px]',
    className
  )}
  {...restProps}
>
  {#if children}
    {@render children()}
  {/if}
</td>
