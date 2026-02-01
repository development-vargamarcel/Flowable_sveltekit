<script lang="ts">
  import { onMount, onDestroy } from 'svelte';
  import { createLogger } from '$lib/utils/logger';

  // Import bpmn-js styles
  // We need to import them dynamically or rely on global styles.
  // Since this is SvelteKit, we can try importing them in <svelte:head> or just rely on a global import in +layout.svelte if needed.
  // But standard way for single component usage is often just importing css if the bundler supports it.
  import 'bpmn-js/dist/assets/diagram-js.css';
  import 'bpmn-js/dist/assets/bpmn-font/css/bpmn.css';

  // We need to use NavigatedViewer to allow zooming/panning
  // Using dynamic import for the library to avoid SSR issues if any
  let BpmnViewer = $state<any>(null);

  const log = createLogger('ProcessDiagram');

  interface Props {
    bpmnXml: string;
    activeActivityIds?: string[];
    height?: string;
  }

  let { bpmnXml, activeActivityIds = [], height = '500px' }: Props = $props();

  let container: HTMLElement;
  let viewer: any;
  let canvas: any;
  let overlays: any;

  onMount(async () => {
    // Dynamically import bpmn-js to avoid SSR issues
    const module = await import('bpmn-js/lib/NavigatedViewer');
    BpmnViewer = module.default;

    if (container) {
      initViewer();
    }
  });

  onDestroy(() => {
    if (viewer) {
      viewer.destroy();
    }
  });

  function initViewer() {
    if (!container || !bpmnXml || !BpmnViewer) return;

    if (viewer) {
      viewer.destroy();
    }

    log.debug('Initializing BPMN viewer');

    viewer = new BpmnViewer({
      container,
      height: '100%',
      width: '100%',
      keyboard: {
        bindTo: window
      }
    });

    importXML();
  }

  async function importXML() {
    if (!viewer || !bpmnXml) return;

    try {
      const { warnings } = await viewer.importXML(bpmnXml);

      if (warnings && warnings.length > 0) {
        log.warn('BPMN import warnings', { warnings });
      }

      log.debug('BPMN XML imported successfully');

      canvas = viewer.get('canvas');
      overlays = viewer.get('overlays');

      // Zoom to fit
      canvas.zoom('fit-viewport');

      highlightActiveActivities();
    } catch (err: any) {
      log.error('Failed to render BPMN', err);
    }
  }

  function highlightActiveActivities() {
    if (!canvas || !activeActivityIds || activeActivityIds.length === 0) return;

    // Remove existing highlights if any (though we usually re-render)
    // For simplicity, we assume fresh render or just add markers.

    activeActivityIds.forEach(id => {
      try {
        canvas.addMarker(id, 'highlight-active');
        log.debug(`Highlighted activity: ${id}`);
      } catch (e) {
        log.warn(`Could not highlight activity ${id}`, e as any);
      }
    });
  }

  // React to prop changes
  $effect(() => {
    if (viewer && bpmnXml) {
       // Check if xml changed? For now, just re-import if meaningful change?
       // Ideally we check if xml is different.
       // But importing again is safe enough.
       importXML();
    }
  });

  $effect(() => {
     // Re-apply highlights if activeActivityIds change
     if (viewer && activeActivityIds) {
         // We might need to clear previous markers.
         // But bpmn-js doesn't expose easy "get all markers" without walking registry.
         // Simpler to just re-import XML or keep track of markers.
         // For now, let's rely on re-import for simplicity or assume additive is okay if we don't support dynamic updates without full refresh.
         // Actually, let's try to just toggle if possible.

         // Since we don't track previous state easily, re-importing is safest to clear old markers.
         importXML();
     }
  });

</script>

<div class="bpmn-container border rounded bg-white relative" style="height: {height}" bind:this={container}>
  {#if !BpmnViewer}
    <div class="absolute inset-0 flex items-center justify-center text-muted-foreground">
      Loading diagram...
    </div>
  {/if}
</div>

<style>
  .bpmn-container {
    width: 100%;
    overflow: hidden;
  }

  :global(.highlight-active:not(.djs-connection) .djs-visual > :first-child) {
    fill: rgba(66, 180, 21, 0.2) !important; /* Green highlight */
    stroke: rgb(66, 180, 21) !important;
  }

  /* Make the powered by logo smaller or hidden if desired, but attribution is good */
  :global(.bjs-powered-by) {
    bottom: 15px !important;
    right: 15px !important;
  }
</style>
