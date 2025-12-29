<script lang="ts">
  import type { Snippet } from 'svelte';
  import type { HTMLTableAttributes } from 'svelte/elements';
  import { cn } from '$lib/utils';

  interface Props extends HTMLTableAttributes {
    children?: Snippet;
    class?: string;
  }

  // eslint-disable-next-line svelte/valid-compile
  const { class: className, children, ...restProps }: Props = $props();
</script>

<div class="relative w-full overflow-auto">
  <table class={cn('w-full caption-bottom text-sm', className)} {...restProps}>
    {#if children}
      {@render children()}
    {/if}
  </table>
</div>
