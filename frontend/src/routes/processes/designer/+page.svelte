<script lang="ts">
  import { onMount } from 'svelte';
  import { goto } from '$app/navigation';
  import { api } from '$lib/api/client';
  import BpmnModeler from 'bpmn-js/lib/Modeler';
  import 'bpmn-js/dist/assets/diagram-js.css';
  import 'bpmn-js/dist/assets/bpmn-font/css/bpmn-embedded.css';

  let modelerContainer: HTMLDivElement;
  let modeler: BpmnModeler | null = null;
  let processName = $state('my-process');
  let processDescription = $state('');
  let isDeploying = $state(false);
  let error = $state('');
  let success = $state('');

  // Default BPMN template with a simple start -> user task -> end flow
  const defaultBpmn = `<?xml version="1.0" encoding="UTF-8"?>
<bpmn:definitions xmlns:bpmn="http://www.omg.org/spec/BPMN/20100524/MODEL"
                  xmlns:bpmndi="http://www.omg.org/spec/BPMN/20100524/DI"
                  xmlns:dc="http://www.omg.org/spec/DD/20100524/DC"
                  xmlns:di="http://www.omg.org/spec/DD/20100524/DI"
                  xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                  xmlns:flowable="http://flowable.org/bpmn"
                  id="Definitions_1"
                  targetNamespace="http://bpmn.io/schema/bpmn">
  <bpmn:process id="process_1" name="New Process" isExecutable="true">
    <bpmn:startEvent id="startEvent" name="Start">
      <bpmn:outgoing>Flow_1</bpmn:outgoing>
    </bpmn:startEvent>
    <bpmn:userTask id="userTask1" name="Review Task" flowable:assignee="\${initiator}">
      <bpmn:incoming>Flow_1</bpmn:incoming>
      <bpmn:outgoing>Flow_2</bpmn:outgoing>
    </bpmn:userTask>
    <bpmn:endEvent id="endEvent" name="End">
      <bpmn:incoming>Flow_2</bpmn:incoming>
    </bpmn:endEvent>
    <bpmn:sequenceFlow id="Flow_1" sourceRef="startEvent" targetRef="userTask1"/>
    <bpmn:sequenceFlow id="Flow_2" sourceRef="userTask1" targetRef="endEvent"/>
  </bpmn:process>
  <bpmndi:BPMNDiagram id="BPMNDiagram_1">
    <bpmndi:BPMNPlane id="BPMNPlane_1" bpmnElement="process_1">
      <bpmndi:BPMNShape id="startEvent_di" bpmnElement="startEvent">
        <dc:Bounds x="152" y="102" width="36" height="36"/>
        <bpmndi:BPMNLabel>
          <dc:Bounds x="158" y="145" width="24" height="14"/>
        </bpmndi:BPMNLabel>
      </bpmndi:BPMNShape>
      <bpmndi:BPMNShape id="userTask1_di" bpmnElement="userTask1">
        <dc:Bounds x="240" y="80" width="100" height="80"/>
      </bpmndi:BPMNShape>
      <bpmndi:BPMNShape id="endEvent_di" bpmnElement="endEvent">
        <dc:Bounds x="392" y="102" width="36" height="36"/>
        <bpmndi:BPMNLabel>
          <dc:Bounds x="400" y="145" width="20" height="14"/>
        </bpmndi:BPMNLabel>
      </bpmndi:BPMNShape>
      <bpmndi:BPMNEdge id="Flow_1_di" bpmnElement="Flow_1">
        <di:waypoint x="188" y="120"/>
        <di:waypoint x="240" y="120"/>
      </bpmndi:BPMNEdge>
      <bpmndi:BPMNEdge id="Flow_2_di" bpmnElement="Flow_2">
        <di:waypoint x="340" y="120"/>
        <di:waypoint x="392" y="120"/>
      </bpmndi:BPMNEdge>
    </bpmndi:BPMNPlane>
  </bpmndi:BPMNDiagram>
</bpmn:definitions>`;

  onMount(async () => {
    // Initialize the BPMN modeler
    modeler = new BpmnModeler({
      container: modelerContainer,
      keyboard: {
        bindTo: document
      }
    });

    try {
      await modeler.importXML(defaultBpmn);
      const canvas = modeler.get('canvas');
      canvas.zoom('fit-viewport');
    } catch (err) {
      console.error('Error loading BPMN diagram:', err);
      error = 'Failed to initialize the process modeler';
    }

    return () => {
      if (modeler) {
        modeler.destroy();
      }
    };
  });

  async function handleDeploy() {
    if (!modeler) {
      error = 'Modeler not initialized';
      return;
    }

    if (!processName || processName.trim() === '') {
      error = 'Please enter a process name';
      return;
    }

    isDeploying = true;
    error = '';
    success = '';

    try {
      // Get the BPMN XML from the modeler
      const result = await modeler.saveXML({ format: true });
      const bpmnXml = result.xml;

      if (!bpmnXml) {
        throw new Error('Failed to generate BPMN XML');
      }

      // Deploy the process
      const response = await api.deployProcess(processName, bpmnXml);
      success = response.message || 'Process deployed successfully!';

      // Redirect to processes page after a short delay
      setTimeout(() => {
        goto('/processes');
      }, 2000);
    } catch (err) {
      console.error('Deployment error:', err);
      error = err instanceof Error ? err.message : 'Failed to deploy process';
    } finally {
      isDeploying = false;
    }
  }

  function handleCancel() {
    goto('/processes');
  }

  async function handleExport() {
    if (!modeler) return;

    try {
      const result = await modeler.saveXML({ format: true });
      const bpmnXml = result.xml;

      if (!bpmnXml) {
        throw new Error('Failed to generate BPMN XML');
      }

      // Create a download link
      const blob = new Blob([bpmnXml], { type: 'application/xml' });
      const url = URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = `${processName || 'process'}.bpmn20.xml`;
      a.click();
      URL.revokeObjectURL(url);
    } catch (err) {
      console.error('Export error:', err);
      error = 'Failed to export BPMN diagram';
    }
  }

  async function handleImport(event: Event) {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];

    if (!file || !modeler) return;

    try {
      const text = await file.text();
      await modeler.importXML(text);
      const canvas = modeler.get('canvas');
      canvas.zoom('fit-viewport');
      success = 'BPMN diagram imported successfully';
      setTimeout(() => (success = ''), 3000);
    } catch (err) {
      console.error('Import error:', err);
      error = 'Failed to import BPMN diagram';
    }

    // Reset input
    input.value = '';
  }
</script>

<div class="min-h-screen bg-gray-50 p-6">
  <div class="mx-auto max-w-7xl">
    <!-- Header -->
    <div class="mb-6">
      <h1 class="text-3xl font-bold text-gray-900">Process Designer</h1>
      <p class="mt-2 text-gray-600">Create and deploy custom BPMN processes</p>
    </div>

    <!-- Process Metadata Form -->
    <div class="mb-6 rounded-lg bg-white p-6 shadow">
      <div class="grid gap-4 md:grid-cols-2">
        <div>
          <label for="processName" class="mb-1 block text-sm font-medium text-gray-700">
            Process Key <span class="text-red-500">*</span>
          </label>
          <input
            type="text"
            id="processName"
            bind:value={processName}
            class="w-full rounded-md border border-gray-300 px-3 py-2 focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500"
            placeholder="e.g., my-custom-process"
            required
          />
          <p class="mt-1 text-xs text-gray-500">
            Unique identifier for the process (lowercase, no spaces)
          </p>
        </div>
        <div>
          <label for="processDescription" class="mb-1 block text-sm font-medium text-gray-700">
            Description
          </label>
          <input
            type="text"
            id="processDescription"
            bind:value={processDescription}
            class="w-full rounded-md border border-gray-300 px-3 py-2 focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500"
            placeholder="Brief description of the process"
          />
        </div>
      </div>
    </div>

    <!-- Alerts -->
    {#if error}
      <div class="mb-4 rounded-md bg-red-50 p-4">
        <div class="flex">
          <div class="flex-shrink-0">
            <svg class="h-5 w-5 text-red-400" viewBox="0 0 20 20" fill="currentColor">
              <path
                fill-rule="evenodd"
                d="M10 18a8 8 0 100-16 8 8 0 000 16zM8.707 7.293a1 1 0 00-1.414 1.414L8.586 10l-1.293 1.293a1 1 0 101.414 1.414L10 11.414l1.293 1.293a1 1 0 001.414-1.414L11.414 10l1.293-1.293a1 1 0 00-1.414-1.414L10 8.586 8.707 7.293z"
                clip-rule="evenodd"
              />
            </svg>
          </div>
          <div class="ml-3">
            <p class="text-sm text-red-800">{error}</p>
          </div>
        </div>
      </div>
    {/if}

    {#if success}
      <div class="mb-4 rounded-md bg-green-50 p-4">
        <div class="flex">
          <div class="flex-shrink-0">
            <svg class="h-5 w-5 text-green-400" viewBox="0 0 20 20" fill="currentColor">
              <path
                fill-rule="evenodd"
                d="M10 18a8 8 0 100-16 8 8 0 000 16zm3.707-9.293a1 1 0 00-1.414-1.414L9 10.586 7.707 9.293a1 1 0 00-1.414 1.414l2 2a1 1 0 001.414 0l4-4z"
                clip-rule="evenodd"
              />
            </svg>
          </div>
          <div class="ml-3">
            <p class="text-sm text-green-800">{success}</p>
          </div>
        </div>
      </div>
    {/if}

    <!-- BPMN Modeler -->
    <div class="mb-6 rounded-lg bg-white shadow">
      <div class="border-b border-gray-200 p-4">
        <div class="flex items-center justify-between">
          <h2 class="text-lg font-semibold text-gray-900">BPMN Diagram</h2>
          <div class="flex gap-2">
            <label
              class="cursor-pointer rounded-md bg-gray-100 px-4 py-2 text-sm font-medium text-gray-700 hover:bg-gray-200"
            >
              Import
              <input
                type="file"
                accept=".bpmn,.bpmn20.xml,.xml"
                class="hidden"
                onchange={handleImport}
              />
            </label>
            <button
              onclick={handleExport}
              class="rounded-md bg-gray-100 px-4 py-2 text-sm font-medium text-gray-700 hover:bg-gray-200"
            >
              Export
            </button>
          </div>
        </div>
      </div>
      <div bind:this={modelerContainer} class="h-[600px] w-full"></div>
    </div>

    <!-- Action Buttons -->
    <div class="flex justify-end gap-4">
      <button
        onclick={handleCancel}
        class="rounded-md border border-gray-300 bg-white px-6 py-2 text-sm font-medium text-gray-700 hover:bg-gray-50"
        disabled={isDeploying}
      >
        Cancel
      </button>
      <button
        onclick={handleDeploy}
        class="rounded-md bg-blue-600 px-6 py-2 text-sm font-medium text-white hover:bg-blue-700 disabled:bg-blue-300"
        disabled={isDeploying}
      >
        {isDeploying ? 'Deploying...' : 'Deploy Process'}
      </button>
    </div>

    <!-- Instructions -->
    <div class="mt-8 rounded-lg bg-blue-50 p-6">
      <h3 class="mb-3 text-lg font-semibold text-blue-900">Getting Started</h3>
      <ul class="space-y-2 text-sm text-blue-800">
        <li class="flex items-start">
          <span class="mr-2">•</span>
          <span
            >Use the toolbar on the left to add BPMN elements (tasks, gateways, events) to your
            diagram</span
          >
        </li>
        <li class="flex items-start">
          <span class="mr-2">•</span>
          <span>Click on elements to configure their properties in the properties panel</span>
        </li>
        <li class="flex items-start">
          <span class="mr-2">•</span>
          <span
            >For user tasks, set the assignee to <code class="rounded bg-blue-100 px-1"
              >${'{'}initiator{'}'}</code
            >
            to assign to process starter</span
          >
        </li>
        <li class="flex items-start">
          <span class="mr-2">•</span>
          <span
            >Use exclusive gateways with conditions to create approval flows (e.g., <code
              class="rounded bg-blue-100 px-1">${'{'}decision == 'approved'{'}'}</code
            >)</span
          >
        </li>
        <li class="flex items-start">
          <span class="mr-2">•</span>
          <span>Import existing BPMN files or export your diagram for backup</span>
        </li>
        <li class="flex items-start">
          <span class="mr-2">•</span>
          <span>Click "Deploy Process" to make your process available for execution</span>
        </li>
      </ul>
    </div>
  </div>
</div>

<style>
  /* Override bpmn-js default styles for better integration */
  :global(.djs-palette) {
    background-color: white;
    border: 1px solid #e5e7eb;
  }

  :global(.djs-context-pad) {
    background-color: white;
  }

  :global(.bjs-powered-by) {
    display: none;
  }
</style>
