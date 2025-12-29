<script lang="ts">
  import { onMount, onDestroy } from 'svelte';
  import { goto } from '$app/navigation';
  import { api } from '$lib/api/client';
  import { processStore } from '$lib/stores/processes.svelte';

  let isLoading = $state(true);
  let error = $state('');
  let success = $state('');
  let deleteConfirm = $state<string | null>(null);

  // Start Instance Modal State
  let showStartModal = $state(false);
  let startProcessKey = $state('');
  let startProcessName = $state('');
  let isStarting = $state(false);
  let businessKey = $state('');
  let variablesJson = $state('{}');
  let variablesError = $state('');

  // Subscribe to process changes for reactive updates
  let unsubscribe: (() => void) | null = null;

  onMount(async () => {
    // Subscribe to process changes from other components
    unsubscribe = processStore.onProcessChange(() => {
      // Force refresh when processes change elsewhere
      loadProcesses(true);
    });

    await loadProcesses();
  });

  onDestroy(() => {
    if (unsubscribe) {
      unsubscribe();
    }
  });

  async function loadProcesses(forceRefresh = false) {
    isLoading = true;
    error = '';
    try {
      await processStore.loadDefinitions(() => api.getProcesses(), forceRefresh);
    } catch (err) {
      console.error('Error loading processes:', err);
      error = err instanceof Error ? err.message : 'Failed to load processes';
    } finally {
      isLoading = false;
    }
  }

  async function handleDelete(processDefId: string, processKey: string, processName: string) {
    if (deleteConfirm !== processKey) {
      deleteConfirm = processKey;
      return;
    }

    error = '';
    success = '';

    try {
      await api.deleteProcess(processDefId, false);
      success = `Process "${processName}" deleted successfully`;
      // Update the store to remove the process and invalidate cache
      processStore.removeProcess(processDefId);
      // Force refresh to ensure we have latest data
      await loadProcesses(true);
    } catch (err) {
      console.error('Error deleting process:', err);
      error = err instanceof Error ? err.message : 'Failed to delete process';
    } finally {
      deleteConfirm = null;
    }
  }

  function handleEdit(processDefId: string, processKey: string) {
    // Navigate to designer with the process definition ID
    goto(`/processes/designer?edit=${processKey}&processDefinitionId=${processDefId}`);
  }

  function handleCreate() {
    goto('/processes/designer');
  }

  function cancelDelete() {
    deleteConfirm = null;
  }

  function openStartModal(processKey: string, processName: string) {
    startProcessKey = processKey;
    startProcessName = processName;
    businessKey = '';
    variablesJson = '{\n  \n}';
    variablesError = '';
    showStartModal = true;
  }

  function closeStartModal() {
    showStartModal = false;
    startProcessKey = '';
    startProcessName = '';
    variablesError = '';
  }

  async function handleStartInstance() {
    variablesError = '';

    // Parse variables JSON
    let variables: Record<string, unknown> = {};
    try {
      const parsed = JSON.parse(variablesJson);
      if (typeof parsed !== 'object' || parsed === null || Array.isArray(parsed)) {
        variablesError = 'Variables must be a JSON object';
        return;
      }
      variables = parsed;
    } catch (e) {
      variablesError = 'Invalid JSON format';
      return;
    }

    // Add business key to variables if provided
    if (businessKey.trim()) {
      variables.businessKey = businessKey.trim();
    }

    isStarting = true;
    error = '';
    success = '';

    try {
      const result = await api.startProcess(startProcessKey, variables);
      success = `Process instance started successfully! Instance ID: ${result.processInstance?.id || 'unknown'}`;
      closeStartModal();
    } catch (err) {
      console.error('Error starting process:', err);
      error = err instanceof Error ? err.message : 'Failed to start process instance';
    } finally {
      isStarting = false;
    }
  }

  // Use the store's grouped definitions
  const groupedProcesses = $derived(() => processStore.groupedDefinitions);
</script>

<svelte:head>
  <title>Manage Processes - BPM Demo</title>
</svelte:head>

<div class="min-h-screen bg-gray-50 p-6">
  <div class="mx-auto max-w-6xl">
    <!-- Header -->
    <div class="mb-6 flex items-center justify-between">
      <div>
        <h1 class="text-3xl font-bold text-gray-900">Manage Processes</h1>
        <p class="mt-2 text-gray-600">View and manage all deployed process definitions</p>
      </div>
      <button
        onclick={handleCreate}
        class="rounded-md bg-blue-600 px-4 py-2 text-sm font-medium text-white hover:bg-blue-700"
      >
        + Create New Process
      </button>
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

    <!-- Loading State -->
    {#if isLoading}
      <div class="flex items-center justify-center py-12">
        <div class="text-center">
          <div
            class="inline-block h-8 w-8 animate-spin rounded-full border-4 border-solid border-blue-600 border-r-transparent"
          ></div>
          <p class="mt-2 text-gray-600">Loading processes...</p>
        </div>
      </div>
    {:else if groupedProcesses().length === 0}
      <!-- Empty State -->
      <div class="rounded-lg bg-white p-12 text-center shadow">
        <svg
          class="mx-auto h-12 w-12 text-gray-400"
          fill="none"
          viewBox="0 0 24 24"
          stroke="currentColor"
        >
          <path
            stroke-linecap="round"
            stroke-linejoin="round"
            stroke-width="2"
            d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"
          />
        </svg>
        <h3 class="mt-2 text-sm font-medium text-gray-900">No processes</h3>
        <p class="mt-1 text-sm text-gray-500">Get started by creating a new process.</p>
        <div class="mt-6">
          <button
            onclick={handleCreate}
            class="inline-flex items-center rounded-md bg-blue-600 px-4 py-2 text-sm font-medium text-white hover:bg-blue-700"
          >
            + Create Process
          </button>
        </div>
      </div>
    {:else}
      <!-- Process List -->
      <div class="space-y-4">
        {#each groupedProcesses() as { key, versions, latest }}
          <div class="rounded-lg bg-white p-6 shadow">
            <div class="flex items-start justify-between">
              <div class="flex-1">
                <div class="flex items-center gap-3">
                  <h3 class="text-lg font-semibold text-gray-900">
                    {latest.name || key}
                  </h3>
                  <span
                    class="rounded-full bg-blue-100 px-3 py-1 text-xs font-medium text-blue-800"
                  >
                    v{latest.version}
                  </span>
                  {#if versions.length > 1}
                    <span class="text-xs text-gray-500">
                      ({versions.length} version{versions.length === 1 ? '' : 's'})
                    </span>
                  {/if}
                </div>
                <p class="mt-1 text-sm text-gray-600">
                  <span class="font-medium">Key:</span>
                  <code class="rounded bg-gray-100 px-1">{key}</code>
                </p>
                {#if latest.description}
                  <p class="mt-2 text-sm text-gray-600">{latest.description}</p>
                {/if}

                <!-- Version History -->
                {#if versions.length > 1}
                  <details class="mt-3">
                    <summary class="cursor-pointer text-sm text-blue-600 hover:text-blue-800">
                      View all versions
                    </summary>
                    <div class="mt-2 space-y-1 pl-4">
                      {#each versions as version}
                        <div class="text-sm text-gray-600">
                          Version {version.version}
                          {#if version === latest}
                            <span class="text-xs text-blue-600">(latest)</span>
                          {/if}
                        </div>
                      {/each}
                    </div>
                  </details>
                {/if}
              </div>

              <div class="ml-4 flex gap-2">
                <button
                  onclick={() => openStartModal(key, latest.name || key)}
                  class="rounded-md bg-green-100 px-3 py-1.5 text-sm font-medium text-green-700 hover:bg-green-200"
                  title="Start a new process instance"
                >
                  Start Instance
                </button>
                <button
                  onclick={() => handleEdit(latest.id, key)}
                  class="rounded-md bg-blue-100 px-3 py-1.5 text-sm font-medium text-blue-700 hover:bg-blue-200"
                  title="View in designer"
                >
                  View
                </button>
                {#if deleteConfirm === key}
                  <div class="flex gap-2">
                    <button
                      onclick={() => handleDelete(latest.id, key, latest.name || key)}
                      class="rounded-md bg-red-600 px-3 py-1.5 text-sm font-medium text-white hover:bg-red-700"
                    >
                      Confirm
                    </button>
                    <button
                      onclick={cancelDelete}
                      class="rounded-md bg-gray-200 px-3 py-1.5 text-sm font-medium text-gray-700 hover:bg-gray-300"
                    >
                      Cancel
                    </button>
                  </div>
                {:else}
                  <button
                    onclick={() => (deleteConfirm = key)}
                    class="rounded-md bg-red-100 px-3 py-1.5 text-sm font-medium text-red-700 hover:bg-red-200"
                    title="Delete process (all versions)"
                  >
                    Delete
                  </button>
                {/if}
              </div>
            </div>
          </div>
        {/each}
      </div>
    {/if}

    <!-- Info Box -->
    <div class="mt-8 rounded-lg bg-blue-50 p-6">
      <h3 class="mb-3 text-lg font-semibold text-blue-900">About Process Management</h3>
      <ul class="space-y-2 text-sm text-blue-800">
        <li class="flex items-start">
          <span class="mr-2">•</span>
          <span
            >Each process can have multiple versions. The latest version is used when starting new
            process instances.</span
          >
        </li>
        <li class="flex items-start">
          <span class="mr-2">•</span>
          <span
            >Click "Start Instance" to create a new running instance of a process with custom variables.</span
          >
        </li>
        <li class="flex items-start">
          <span class="mr-2">•</span>
          <span
            >Deleting a process will remove all versions and may affect running process instances.</span
          >
        </li>
        <li class="flex items-start">
          <span class="mr-2">•</span>
          <span>Use the Process Designer to create new processes or modify existing ones.</span>
        </li>
      </ul>
    </div>
  </div>
</div>

<!-- Start Instance Modal -->
{#if showStartModal}
  <div class="fixed inset-0 z-50 flex items-center justify-center bg-black bg-opacity-50">
    <div class="w-full max-w-lg rounded-lg bg-white shadow-xl">
      <div class="border-b border-gray-200 p-6">
        <div class="flex items-center justify-between">
          <div>
            <h3 class="text-xl font-semibold text-gray-900">Start Process Instance</h3>
            <p class="mt-1 text-sm text-gray-500">Starting: <strong>{startProcessName}</strong></p>
          </div>
          <button
            onclick={closeStartModal}
            class="text-gray-400 hover:text-gray-600"
          >
            <svg class="h-6 w-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"/>
            </svg>
          </button>
        </div>
      </div>

      <div class="p-6">
        <div class="space-y-4">
          <!-- Business Key -->
          <div>
            <label for="businessKey" class="mb-1 block text-sm font-medium text-gray-700">
              Business Key <span class="text-gray-400">(optional)</span>
            </label>
            <input
              type="text"
              id="businessKey"
              bind:value={businessKey}
              class="w-full rounded-md border border-gray-300 px-3 py-2 focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500"
              placeholder="e.g., ORDER-001, REQ-2024-001"
            />
            <p class="mt-1 text-xs text-gray-500">A unique identifier for this process instance</p>
          </div>

          <!-- Variables -->
          <div>
            <label for="variables" class="mb-1 block text-sm font-medium text-gray-700">
              Process Variables <span class="text-gray-400">(JSON)</span>
            </label>
            <textarea
              id="variables"
              bind:value={variablesJson}
              rows="6"
              class="w-full rounded-md border border-gray-300 px-3 py-2 font-mono text-sm focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500"
              placeholder='{"amount": 100, "description": "Test"}'
            ></textarea>
            {#if variablesError}
              <p class="mt-1 text-xs text-red-600">{variablesError}</p>
            {:else}
              <p class="mt-1 text-xs text-gray-500">Enter process variables as a JSON object</p>
            {/if}
          </div>

          <!-- Example Variables -->
          <div class="rounded-md bg-gray-50 p-3">
            <p class="text-xs font-medium text-gray-700">Example Variables:</p>
            <pre class="mt-1 text-xs text-gray-600">{`{
  "amount": 500,
  "description": "Equipment purchase",
  "priority": "high"
}`}</pre>
          </div>
        </div>
      </div>

      <div class="border-t border-gray-200 bg-gray-50 p-4">
        <div class="flex justify-end gap-3">
          <button
            onclick={closeStartModal}
            class="rounded-md border border-gray-300 bg-white px-4 py-2 text-sm font-medium text-gray-700 hover:bg-gray-50"
            disabled={isStarting}
          >
            Cancel
          </button>
          <button
            onclick={handleStartInstance}
            disabled={isStarting}
            class="rounded-md bg-green-600 px-4 py-2 text-sm font-medium text-white hover:bg-green-700 disabled:cursor-not-allowed disabled:bg-green-300"
          >
            {isStarting ? 'Starting...' : 'Start Instance'}
          </button>
        </div>
      </div>
    </div>
  </div>
{/if}
