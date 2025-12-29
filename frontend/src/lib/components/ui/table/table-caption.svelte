<script lang="ts">
  import type { Snippet } from 'svelte';
  import type { HTMLAttributes } from 'svelte/elements';
  import { cn } from '$lib/utils';

  interface Props extends HTMLAttributes<HTMLTableCaptionElement> {
    children?: Snippet;
    class?: string;
  }

  // eslint-disable-next-line svelte/valid-compile
  const { class: className, children, ...restProps }: Props = $props();
</script>

<caption class={cn('text-muted-foreground mt-4 text-sm', className)} {...restProps}>
  {#if children}
    {@render children()}
  {/if}
</caption>
