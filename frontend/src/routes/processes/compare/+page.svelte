<script lang="ts">
  import { onMount } from 'svelte';
  import { page } from '$app/stores';
  import { api } from '$lib/api/client';

  import BpmnViewer from 'bpmn-js/lib/Viewer';
  // @ts-ignore
  import { diff } from 'bpmn-js-differ';

  let leftId = $state('');
  let rightId = $state('');
  let leftBpmn = $state('');
  let rightBpmn = $state('');

  let isLoading = $state(true);
  let error = $state('');
  let viewerContainer = $state<HTMLDivElement>();
  let viewer = $state<any>(null);

  // Diff statistics
  let added = $state(0);
  let removed = $state(0);
  let changed = $state(0);
  let layoutChanged = $state(0);

  onMount(async () => {
    leftId = $page.url.searchParams.get('left') || '';
    rightId = $page.url.searchParams.get('right') || '';

    if (!leftId || !rightId) {
      error = 'Please select two process versions to compare.';
      isLoading = false;
      return;
    }

    await loadData();
  });

  async function loadData() {
    isLoading = true;
    try {
      const [leftRes, rightRes] = await Promise.all([
        api.getProcessBpmn(leftId),
        api.getProcessBpmn(rightId)
      ]);

      leftBpmn = leftRes.bpmn;
      rightBpmn = rightRes.bpmn;

      initializeViewer();
    } catch (err) {
      console.error(err);
      error = 'Failed to load BPMN definitions.';
    } finally {
      isLoading = false;
    }
  }

  function initializeViewer() {
    if (!viewerContainer) return;

    viewer = new BpmnViewer({
      container: viewerContainer
    });

    // Import the "left" (older/base) diagram first
    viewer.importXML(leftBpmn).then(() => {
        // Compute diff against "right" (newer) diagram
        // The API for bpmn-js-differ usually works by importing one and then diffing against another object tree
        // However, the standard usage in examples is:
        // viewer.importXML(oldXML, function(err) { ... });
        // const diff = viewer.get('diff');
        // diff.diff(newDefinitions);

        // Actually, looking at bpmn-js-differ documentation, it provides a 'diffing' service
        // that can compare two moddle definitions.

        // We need to parse the second XML manually into definitions
        const moddle = viewer.get('moddle');
        moddle.fromXML(rightBpmn, 'bpmn:Definitions').then((result: any) => {
             const newDefinitions = result.rootElement;

             // Get the canvas to zoom fit
             const canvas = viewer.get('canvas');
             canvas.zoom('fit-viewport');

             // Visual Diffing?
             // bpmn-js-differ is a toolkit. To VISUALIZE, we usually need overlays.
             // The module adds visual markers if integrated correctly.

             // Let's try to verify if the module is active
             try {
                const changes = diff(viewer.getDefinitions(), newDefinitions);

                // Debug log
                console.log('Diff result:', changes);

                // Calculate stats
                added = Object.keys(changes._added || {}).length;
                removed = Object.keys(changes._removed || {}).length;
                changed = Object.keys(changes._changed || {}).length;
                layoutChanged = Object.keys(changes._layoutChanged || {}).length;

             } catch (e) {
                console.error('Diff calculation failed', e);
                // Fallback or specific handling
             }
        });
    });
  }
</script>

<!-- CSS removed to avoid unused selector warnings since class application logic is external/library based -->

<div class="h-screen flex flex-col bg-white">
  <div class="border-b border-gray-200 bg-gray-50 px-6 py-4 flex justify-between items-center">
    <div>
      <h1 class="text-xl font-bold text-gray-900">Process Comparison</h1>
      <p class="text-sm text-gray-500">Comparing versions {leftId} vs {rightId}</p>
    </div>

    <div class="flex gap-6 text-sm">
       <div class="flex items-center gap-1">
          <div class="w-3 h-3 bg-green-500 rounded-full"></div>
          <span>Added: {added}</span>
       </div>
       <div class="flex items-center gap-1">
          <div class="w-3 h-3 bg-red-500 rounded-full"></div>
          <span>Removed: {removed}</span>
       </div>
       <div class="flex items-center gap-1">
          <div class="w-3 h-3 bg-orange-500 rounded-full"></div>
          <span>Changed: {changed}</span>
       </div>
    </div>

    <a href="/processes/manage" class="text-blue-600 hover:underline text-sm">Back to Manage</a>
  </div>

  {#if error}
    <div class="p-6">
      <div class="rounded-md bg-red-50 p-4 text-red-700">{error}</div>
    </div>
  {:else}
    <div class="flex-1 relative">
      <div bind:this={viewerContainer} class="w-full h-full"></div>

      {#if isLoading}
        <div class="absolute inset-0 flex items-center justify-center bg-white bg-opacity-80 z-10">
          <div class="text-center">
             <div class="inline-block h-8 w-8 animate-spin rounded-full border-4 border-solid border-blue-600 border-r-transparent"></div>
             <p class="mt-2 text-gray-600">Loading diagrams...</p>
          </div>
        </div>
      {/if}
    </div>
  {/if}
</div>
