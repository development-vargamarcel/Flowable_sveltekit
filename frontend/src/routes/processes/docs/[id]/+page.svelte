<script lang="ts">
  import { onMount } from 'svelte';
  import { page } from '$app/stores';
  import { api } from '$lib/api/client';

  import BpmnViewer from 'bpmn-js/lib/Viewer';

  type ViewerHandle = {
    importXML: (xml: string) => Promise<unknown>;
    get: (service: 'canvas' | 'elementRegistry') => unknown;
    destroy?: () => void;
  };

  type BpmnBusinessObject = {
    id?: string;
    name?: string;
    assignee?: string;
    candidateGroups?: string;
    formKey?: string;
    class?: string;
    delegateExpression?: string;
    expression?: string;
    scriptFormat?: string;
    documentation?: Array<{ text?: string }>;
  };

  type BpmnElement = {
    id: string;
    type: string;
    businessObject?: BpmnBusinessObject;
  };

  type Canvas = {
    zoom: (mode: 'fit-viewport') => void;
  };

  type ElementRegistry = {
    filter: (predicate: (element: BpmnElement) => boolean) => BpmnElement[];
  };

  type UserTaskSummary = {
    id: string;
    name: string;
    assignee?: string;
    candidateGroups?: string;
    formKey?: string;
    docs?: string;
  };

  type ServiceTaskSummary = {
    id: string;
    name: string;
    type: 'Java Class' | 'Delegate' | 'Expression';
    implementation?: string;
  };

  let processId = $state('');
  let bpmnXml = $state('');
  let isLoading = $state(true);
  let error = $state('');

  let viewerContainer = $state<HTMLDivElement>();
  let viewer: ViewerHandle | null = null;

  // Extracted metadata
  let processName = $state('');
  let description = $state('');
  let userTasks: UserTaskSummary[] = $state([]);
  let serviceTasks: ServiceTaskSummary[] = $state([]);

  onMount(() => {
    processId = $page.params.id ?? '';
    if (!processId) {
      error = 'No process ID provided';
      return;
    }

    void loadData();

    return () => {
      viewer?.destroy?.();
      viewer = null;
    };
  });

  async function loadData() {
    isLoading = true;
    try {
      const res = await api.getProcessBpmn(processId);
      bpmnXml = res.bpmn;

      initializeViewer();
    } catch (err) {
      console.error(err);
      error = 'Failed to load process documentation.';
    } finally {
      isLoading = false;
    }
  }

  function initializeViewer() {
    if (!viewerContainer) return;

    viewer = new BpmnViewer({
      container: viewerContainer
    }) as ViewerHandle;

    viewer.importXML(bpmnXml).then(() => {
      const canvas = viewer?.get('canvas') as Canvas | undefined;
      canvas?.zoom('fit-viewport');

      // Extract metadata using ElementRegistry
      const elementRegistry = viewer?.get('elementRegistry') as ElementRegistry | undefined;
      if (!elementRegistry) {
        return;
      }

      // Find Process element
      const process = elementRegistry.filter((e) => e.type === 'bpmn:Process')[0];
      const businessObject = process?.businessObject;
      if (businessObject) {
        processName = businessObject.name || businessObject.id || 'Unnamed Process';
        description = businessObject.documentation?.[0]?.text || '';
      }

      // Collect tasks
      userTasks = elementRegistry.filter((e) => e.type === 'bpmn:UserTask').map((e) => ({
        id: e.id,
        name: e.businessObject?.name || 'Unnamed Task',
        assignee: e.businessObject?.assignee,
        candidateGroups: e.businessObject?.candidateGroups,
        formKey: e.businessObject?.formKey,
        docs: e.businessObject?.documentation?.[0]?.text
      }));

      serviceTasks = elementRegistry.filter((e) => e.type === 'bpmn:ServiceTask').map((e) => ({
        id: e.id,
        name: e.businessObject?.name || 'Unnamed Service',
        type: e.businessObject?.class
          ? 'Java Class'
          : e.businessObject?.delegateExpression
            ? 'Delegate'
            : 'Expression',
        implementation:
          e.businessObject?.class || e.businessObject?.delegateExpression || e.businessObject?.expression
      }));
    });
  }

  function print() {
    window.print();
  }
</script>

<style>
  @media print {
    .no-print {
      display: none;
    }
    :global(body) {
       background: white;
    }
  }
</style>

<div class="min-h-screen bg-gray-50 print:bg-white">
  <!-- Navbar / Header -->
  <div class="no-print bg-white border-b border-gray-200 px-6 py-4 flex justify-between items-center sticky top-0 z-10 shadow-sm">
    <div>
      <h1 class="text-xl font-bold text-gray-900">Process Documentation</h1>
      <p class="text-sm text-gray-500">ID: {processId}</p>
    </div>
    <div class="flex gap-3">
      <a href="/processes/manage" class="px-4 py-2 text-sm text-gray-600 hover:text-gray-900">Back</a>
      <button
        onclick={print}
        class="bg-blue-600 text-white px-4 py-2 rounded-md hover:bg-blue-700 text-sm font-medium"
      >
        Print / Save PDF
      </button>
    </div>
  </div>

  {#if isLoading}
    <div class="p-12 text-center">Loading...</div>
  {:else if error}
    <div class="p-6">
      <div class="bg-red-50 text-red-700 p-4 rounded">{error}</div>
    </div>
  {:else}
    <div class="max-w-5xl mx-auto p-8 bg-white my-6 shadow-lg print:shadow-none print:my-0 print:p-0">

      <!-- Title Section -->
      <div class="mb-8 border-b pb-4">
        <h1 class="text-3xl font-bold text-gray-900 mb-2">{processName}</h1>
        <div class="text-gray-500 text-sm mb-4">Process Definition Key: {processId}</div>

        {#if description}
          <div class="bg-gray-50 p-4 rounded text-gray-700 border-l-4 border-blue-500">
             {description}
          </div>
        {/if}
      </div>

      <!-- Diagram -->
      <div class="mb-10">
        <h2 class="text-xl font-semibold mb-4 text-gray-800 border-b pb-2">Process Diagram</h2>
        <div class="border border-gray-200 h-[400px] w-full" bind:this={viewerContainer}></div>
      </div>

      <!-- User Tasks -->
      {#if userTasks.length > 0}
        <div class="mb-10 page-break-inside-avoid">
          <h2 class="text-xl font-semibold mb-4 text-gray-800 border-b pb-2">User Tasks</h2>
          <div class="overflow-x-auto">
            <table class="min-w-full text-sm text-left">
               <thead class="bg-gray-100 uppercase text-xs font-semibold text-gray-600">
                 <tr>
                   <th class="px-4 py-2">Task Name</th>
                   <th class="px-4 py-2">Assignee / Group</th>
                   <th class="px-4 py-2">Form Key</th>
                   <th class="px-4 py-2">Documentation</th>
                 </tr>
               </thead>
               <tbody class="divide-y divide-gray-200">
                 {#each userTasks as task}
                   <tr>
                     <td class="px-4 py-3 font-medium">{task.name}</td>
                     <td class="px-4 py-3">
                        {#if task.assignee}
                           <span class="inline-flex items-center px-2 py-0.5 rounded text-xs font-medium bg-blue-100 text-blue-800">
                             User: {task.assignee}
                           </span>
                        {:else if task.candidateGroups}
                           <span class="inline-flex items-center px-2 py-0.5 rounded text-xs font-medium bg-purple-100 text-purple-800">
                             Group: {task.candidateGroups}
                           </span>
                        {:else}
                           <span class="text-gray-400">-</span>
                        {/if}
                     </td>
                     <td class="px-4 py-3 font-mono text-xs">{task.formKey || '-'}</td>
                     <td class="px-4 py-3 text-gray-600">{task.docs || '-'}</td>
                   </tr>
                 {/each}
               </tbody>
            </table>
          </div>
        </div>
      {/if}

      <!-- Service Tasks -->
      {#if serviceTasks.length > 0}
        <div class="mb-10 page-break-inside-avoid">
          <h2 class="text-xl font-semibold mb-4 text-gray-800 border-b pb-2">Automated Steps (Service Tasks)</h2>
          <table class="min-w-full text-sm text-left">
             <thead class="bg-gray-100 uppercase text-xs font-semibold text-gray-600">
               <tr>
                 <th class="px-4 py-2">Name</th>
                 <th class="px-4 py-2">Type</th>
                 <th class="px-4 py-2">Implementation</th>
               </tr>
             </thead>
             <tbody class="divide-y divide-gray-200">
               {#each serviceTasks as task}
                 <tr>
                   <td class="px-4 py-3 font-medium">{task.name}</td>
                   <td class="px-4 py-3">{task.type}</td>
                   <td class="px-4 py-3 font-mono text-xs truncate max-w-xs" title={task.implementation}>
                     {task.implementation}
                   </td>
                 </tr>
               {/each}
             </tbody>
          </table>
        </div>
      {/if}

      <!-- Footer -->
      <div class="mt-12 pt-6 border-t border-gray-200 text-center text-xs text-gray-500">
        Generated on {new Date().toLocaleDateString()} by Flowable BPM Demo
      </div>
    </div>
  {/if}
</div>
