<script lang="ts">
  import type { Snippet } from 'svelte';
  import type { DesignerFormGrid } from '$lib/designer/form-builders';

  type Props = {
    open?: boolean;
    formGrids?: DesignerFormGrid[];
    onadd?: () => void;
    onsave?: () => void;
    onclose?: () => void;
    children?: Snippet;
  };

  // Props are destructured with `let` because Svelte bindable props must be declared from $props().
  // eslint-disable-next-line prefer-const
  let { open = false, formGrids = $bindable<DesignerFormGrid[]>([]), onadd, onsave, onclose, children }: Props = $props();
</script>

{#if open}
  <div class="fixed inset-0 z-50 flex items-center justify-center bg-black/50">
    <div class="max-h-[90vh] w-full max-w-5xl overflow-auto rounded-lg bg-white p-6 shadow-xl">
      <div class="mb-4 flex items-center justify-between">
        <h2 class="text-xl font-semibold">Grid Builder</h2>
        <button class="text-gray-500 hover:text-gray-700" onclick={onclose}>×</button>
      </div>
      {@render children?.()}
      <div class="mt-4 flex justify-end gap-2">
        <button class="rounded bg-gray-200 px-4 py-2" onclick={onadd}>Add Grid</button>
        <button class="rounded bg-blue-600 px-4 py-2 text-white" onclick={onsave}>Save Data Grids ({formGrids.length})</button>
      </div>
    </div>
  </div>
{/if}
