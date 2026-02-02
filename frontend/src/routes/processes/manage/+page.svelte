<script lang="ts">
  import { onMount, onDestroy } from 'svelte';
  import { goto } from '$app/navigation';
  import { api } from '$lib/api/client';
  import { processStore } from '$lib/stores/processes.svelte';
  import ConfirmDialog from '$lib/components/ConfirmDialog.svelte';

  let isLoading = $state(true);
  let error = $state('');
  let success = $state('');
  
  // Delete confirmation state
  let deleteDialog = $state<{
    open: boolean;
    processDefId: string;
    processKey: string;
    processName: string;
    loading: boolean;
  }>({
    open: false,
    processDefId: '',
    processKey: '',
    processName: '',
    loading: false
  });

  // Start Instance Modal State
  let showStartModal = $state(false);
  let startProcessKey = $state('');
  let startProcessName = $state('');
  let isStarting = $state(false);
  let businessKey = $state('');
  let variablesJson = $state('{}');
  let variablesError = $state('');

  // Category Edit State
  let editingCategory = $state<string | null>(null);
  let newCategory = $state('');

  // Comparison State
  let selectionMode = $state(false);
  let selectedForCompare: string[] = $state([]);

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
      // We want ALL definitions for management, so we use the new endpoint
      await processStore.loadDefinitions(() => api.getAllProcessDefinitions(), forceRefresh);
    } catch (err) {
      console.error('Error loading processes:', err);
      error = err instanceof Error ? err.message : 'Failed to load processes';
    } finally {
      isLoading = false;
    }
  }

  function openDeleteDialog(processDefId: string, processKey: string, processName: string) {
    deleteDialog = {
      open: true,
      processDefId,
      processKey,
      processName,
      loading: false
    };
  }

  function closeDeleteDialog() {
    deleteDialog = { ...deleteDialog, open: false, loading: false };
  }

  async function confirmDelete() {
    deleteDialog.loading = true;
    error = '';
    success = '';

    try {
      await api.deleteProcess(deleteDialog.processDefId, false);
      success = `Process "${deleteDialog.processName}" deleted successfully`;
      processStore.removeProcess(deleteDialog.processDefId);
      closeDeleteDialog();
      await loadProcesses(true);
    } catch (err) {
      console.error('Error deleting process:', err);
      error = err instanceof Error ? err.message : 'Failed to delete process';
      deleteDialog.loading = false;
    }
  }

  async function handleSuspend(processDefId: string) {
    try {
      await api.suspendProcess(processDefId);
      success = 'Process suspended';
      await loadProcesses(true);
    } catch (err) {
      error = 'Failed to suspend process';
    }
  }

  async function handleActivate(processDefId: string) {
    try {
      await api.activateProcess(processDefId);
      success = 'Process activated';
      await loadProcesses(true);
    } catch (err) {
      error = 'Failed to activate process';
    }
  }

  async function updateCategory(processDefId: string) {
    if (!newCategory.trim()) return;
    try {
      await api.updateProcessCategory(processDefId, newCategory.trim());
      success = 'Category updated';
      editingCategory = null;
      await loadProcesses(true);
    } catch (err) {
      error = 'Failed to update category';
    }
  }

  function handleEdit(processDefId: string, processKey: string) {
    goto(`/processes/designer?edit=${processKey}&processDefinitionId=${processDefId}`);
  }

  function handleCreate() {
    goto('/processes/designer');
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
    let variables: Record<string, unknown> = {};
    try {
      const trimmed = variablesJson.trim();
      if (trimmed.length > 0) {
        const parsed = JSON.parse(trimmed);
        if (typeof parsed !== 'object' || parsed === null || Array.isArray(parsed)) {
          variablesError = 'Variables must be a JSON object';
          return;
        }
        variables = parsed;
      }
    } catch (e) {
      variablesError = 'Invalid JSON format';
      return;
    }

    isStarting = true;
    error = '';
    success = '';

    try {
      const trimmedBusinessKey = businessKey.trim();
      const result = await api.startProcess(
        startProcessKey,
        variables,
        trimmedBusinessKey.length > 0 ? trimmedBusinessKey : undefined
      );
      success = `Process instance started successfully! Instance ID: ${result.processInstance?.id || 'unknown'}`;
      closeStartModal();
    } catch (err) {
      console.error('Error starting process:', err);
      error = err instanceof Error ? err.message : 'Failed to start process instance';
    } finally {
      isStarting = false;
    }
  }

  function formatVariablesJson() {
    variablesError = '';
    try {
      const trimmed = variablesJson.trim();
      if (!trimmed) {
        variablesJson = '{\n  \n}';
        return;
      }
      const parsed = JSON.parse(trimmed);
      if (typeof parsed !== 'object' || parsed === null || Array.isArray(parsed)) {
        variablesError = 'Variables must be a JSON object';
        return;
      }
      variablesJson = JSON.stringify(parsed, null, 2);
    } catch (e) {
      variablesError = 'Invalid JSON format';
    }
  }

  function resetVariablesJson() {
    variablesError = '';
    variablesJson = '{\n  \n}';
  }

  function toggleCompareSelection(id: string) {
    if (selectedForCompare.includes(id)) {
      selectedForCompare = selectedForCompare.filter(pid => pid !== id);
    } else {
      if (selectedForCompare.length < 2) {
        selectedForCompare = [...selectedForCompare, id];
      }
    }
  }

  function runComparison() {
    if (selectedForCompare.length === 2) {
      goto(`/processes/compare?left=${selectedForCompare[0]}&right=${selectedForCompare[1]}`);
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
      <div class="flex gap-2">
        {#if selectionMode}
          <div class="flex items-center gap-2 bg-blue-50 px-3 py-1 rounded border border-blue-200">
             <span class="text-sm font-medium text-blue-800">{selectedForCompare.length}/2 selected</span>
             <button
               disabled={selectedForCompare.length !== 2}
               onclick={runComparison}
               class="rounded bg-blue-600 px-3 py-1 text-xs font-bold text-white disabled:opacity-50 hover:bg-blue-700"
             >
               Compare
             </button>
             <button
               onclick={() => { selectionMode = false; selectedForCompare = []; }}
               class="ml-2 text-xs text-blue-600 hover:underline"
             >
               Cancel
             </button>
          </div>
        {:else}
          <button
             onclick={() => selectionMode = true}
             class="rounded-md border border-gray-300 bg-white px-4 py-2 text-sm font-medium text-gray-700 hover:bg-gray-50"
          >
             Compare Versions
          </button>
        {/if}
        <button
          onclick={handleCreate}
          class="rounded-md bg-blue-600 px-4 py-2 text-sm font-medium text-white hover:bg-blue-700"
        >
          + Create New Process
        </button>
      </div>
    </div>

    <!-- Alerts -->
    {#if error}
      <div class="mb-4 rounded-md bg-red-50 p-4">
        <div class="flex">
          <div class="flex-shrink-0">
             <svg class="h-5 w-5 text-red-400" viewBox="0 0 20 20" fill="currentColor">
               <path fill-rule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zM8.707 7.293a1 1 0 00-1.414 1.414L8.586 10l-1.293 1.293a1 1 0 101.414 1.414L10 11.414l1.293 1.293a1 1 0 001.414-1.414L11.414 10l1.293-1.293a1 1 0 00-1.414-1.414L10 8.586 8.707 7.293z" clip-rule="evenodd" />
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
              <path fill-rule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zm3.707-9.293a1 1 0 00-1.414-1.414L9 10.586 7.707 9.293a1 1 0 00-1.414 1.414l2 2a1 1 0 001.414 0l4-4z" clip-rule="evenodd" />
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
          <div class="inline-block h-8 w-8 animate-spin rounded-full border-4 border-solid border-blue-600 border-r-transparent"></div>
          <p class="mt-2 text-gray-600">Loading processes...</p>
        </div>
      </div>
    {:else if groupedProcesses().length === 0}
      <!-- Empty State -->
      <div class="rounded-lg bg-white p-12 text-center shadow">
        <svg class="mx-auto h-12 w-12 text-gray-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
        </svg>
        <h3 class="mt-2 text-sm font-medium text-gray-900">No processes</h3>
        <p class="mt-1 text-sm text-gray-500">Get started by creating a new process.</p>
        <div class="mt-6">
          <button onclick={handleCreate} class="inline-flex items-center rounded-md bg-blue-600 px-4 py-2 text-sm font-medium text-white hover:bg-blue-700">
            + Create Process
          </button>
        </div>
      </div>
    {:else}
      <!-- Process List -->
      <div class="space-y-4">
        {#each groupedProcesses() as { key, versions, latest }}
          <div class="rounded-lg bg-white p-6 shadow border border-transparent {selectedForCompare.includes(latest.id) ? '!border-blue-500 ring-1 ring-blue-500' : ''}">
            <div class="flex items-start justify-between">
              <!-- Left Side -->
              <div class="flex-1">
                <div class="flex items-center gap-3">
                  {#if selectionMode}
                    <input
                      type="checkbox"
                      checked={selectedForCompare.includes(latest.id)}
                      onchange={() => toggleCompareSelection(latest.id)}
                      class="h-4 w-4 rounded border-gray-300 text-blue-600 focus:ring-blue-500"
                    />
                  {/if}

                  <h3 class="text-lg font-semibold text-gray-900">
                    {latest.name || key}
                  </h3>
                  <span class="rounded-full bg-blue-100 px-3 py-1 text-xs font-medium text-blue-800">
                    v{latest.version}
                  </span>
                  {#if latest.suspended}
                    <span class="rounded-full bg-red-100 px-3 py-1 text-xs font-medium text-red-800">Suspended</span>
                  {:else}
                    <span class="rounded-full bg-green-100 px-3 py-1 text-xs font-medium text-green-800">Active</span>
                  {/if}

                  {#if editingCategory === latest.id}
                    <div class="flex items-center gap-1">
                      <input
                        type="text"
                        bind:value={newCategory}
                        class="h-6 w-32 rounded border border-gray-300 px-2 text-xs"
                        placeholder="Category"
                      />
                      <button onclick={() => updateCategory(latest.id)} class="text-green-600 hover:text-green-800">✓</button>
                      <button onclick={() => editingCategory = null} class="text-gray-400 hover:text-gray-600">✕</button>
                    </div>
                  {:else}
                    <button
                      onclick={() => { editingCategory = latest.id; newCategory = latest.category || ''; }}
                      class="rounded-full bg-gray-100 px-3 py-1 text-xs font-medium text-gray-600 hover:bg-gray-200"
                    >
                      {latest.category || '+ Add Category'}
                    </button>
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
                        <div class="flex items-center gap-2 text-sm text-gray-600">
                          {#if selectionMode}
                            <input
                              type="checkbox"
                              checked={selectedForCompare.includes(version.id)}
                              onchange={() => toggleCompareSelection(version.id)}
                              class="h-3 w-3 rounded border-gray-300 text-blue-600 focus:ring-blue-500"
                            />
                          {/if}
                          <span>Version {version.version}</span>
                          {#if version.id === latest.id}
                            <span class="text-xs text-blue-600">(latest)</span>
                          {/if}
                          {#if version.suspended}
                             <span class="text-xs text-red-500">[Suspended]</span>
                          {/if}
                        </div>
                      {/each}
                    </div>
                  </details>
                {/if}
              </div>

              <!-- Action Buttons -->
              <div class="ml-4 flex flex-wrap justify-end gap-2 max-w-[400px]">
                <a
                  href="/processes/start/{latest.id}"
                  class="rounded-md bg-green-600 px-3 py-1.5 text-sm font-medium text-white hover:bg-green-700"
                  title="Start a new process instance with form"
                >
                  Start
                </a>
                <button
                  onclick={() => openStartModal(key, latest.name || key)}
                  class="rounded-md bg-green-100 px-3 py-1.5 text-sm font-medium text-green-700 hover:bg-green-200"
                  title="Start with raw JSON variables"
                >
                  Start (JSON)
                </button>
                <button
                  onclick={() => handleEdit(latest.id, key)}
                  class="rounded-md bg-blue-100 px-3 py-1.5 text-sm font-medium text-blue-700 hover:bg-blue-200"
                  title="View in designer"
                >
                  View
                </button>
                <a
                  href="/processes/docs/{latest.id}"
                  class="rounded-md bg-purple-100 px-3 py-1.5 text-sm font-medium text-purple-700 hover:bg-purple-200"
                >
                  Docs
                </a>

                {#if latest.suspended}
                  <button
                    onclick={() => handleActivate(latest.id)}
                    class="rounded-md bg-yellow-100 px-3 py-1.5 text-sm font-medium text-yellow-700 hover:bg-yellow-200"
                  >
                    Activate
                  </button>
                {:else}
                  <button
                    onclick={() => handleSuspend(latest.id)}
                    class="rounded-md bg-orange-100 px-3 py-1.5 text-sm font-medium text-orange-700 hover:bg-orange-200"
                  >
                    Suspend
                  </button>
                {/if}

                <button
                  onclick={() => openDeleteDialog(latest.id, key, latest.name || key)}
                  class="rounded-md bg-red-100 px-3 py-1.5 text-sm font-medium text-red-700 hover:bg-red-200"
                  title="Delete process (all versions)"
                >
                  Delete
                </button>
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
          <span>Each process can have multiple versions. The latest version is used when starting new process instances.</span>
        </li>
        <li class="flex items-start">
          <span class="mr-2">•</span>
          <span><b>Suspended</b> processes cannot be started but existing instances continue.</span>
        </li>
        <li class="flex items-start">
          <span class="mr-2">•</span>
          <span>Use <b>Compare Versions</b> to see visual differences between two process definitions.</span>
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
            aria-label="Close modal"
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
              placeholder='&#123;"amount": 100, "description": "Test"&#125;'
            ></textarea>
            <div class="mt-2 flex flex-wrap items-center justify-between gap-2 text-xs text-gray-500">
              <span>Enter process variables as a JSON object</span>
              <div class="flex items-center gap-2">
                <button
                  type="button"
                  class="rounded-md border border-gray-300 bg-white px-2.5 py-1 text-xs font-medium text-gray-700 hover:bg-gray-50"
                  onclick={formatVariablesJson}
                >
                  Format JSON
                </button>
                <button
                  type="button"
                  class="rounded-md border border-gray-300 bg-white px-2.5 py-1 text-xs font-medium text-gray-700 hover:bg-gray-50"
                  onclick={resetVariablesJson}
                >
                  Reset
                </button>
              </div>
            </div>
            {#if variablesError}
              <p class="mt-1 text-xs text-red-600">{variablesError}</p>
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

<!-- Delete Confirmation Dialog -->
<ConfirmDialog
  open={deleteDialog.open}
  title="Delete Process"
  message="Are you sure you want to delete '{deleteDialog.processName}'? This will remove all versions of this process definition. Running process instances will not be affected."
  confirmText="Delete"
  cancelText="Cancel"
  variant="danger"
  loading={deleteDialog.loading}
  onConfirm={confirmDelete}
  onCancel={closeDeleteDialog}
/>
