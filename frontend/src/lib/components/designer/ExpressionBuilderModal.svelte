<script lang="ts">
  import type { Snippet } from 'svelte';
  const { open = false, onclose, children }: { open?: boolean; onclose?: () => void; children?: Snippet } = $props();
</script>

{#if open}
  <div class="fixed inset-0 z-50">
    {@render children?.()}
    <button onclick={onclose}>Close</button>
  </div>
{/if}
