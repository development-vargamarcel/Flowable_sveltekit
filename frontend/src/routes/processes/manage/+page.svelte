<script lang="ts">
  import { onMount } from 'svelte';
  import { goto } from '$app/navigation';
  import { api } from '$lib/api/client';
  import type { ProcessDefinition } from '$lib/types';

  let processes = $state<ProcessDefinition[]>([]);
  let isLoading = $state(true);
  let error = $state('');
  let success = $state('');
  let deleteConfirm = $state<string | null>(null);

  onMount(async () => {
    await loadProcesses();
  });

  async function loadProcesses() {
    isLoading = true;
    error = '';
    try {
      processes = await api.getProcesses();
    } catch (err) {
      console.error('Error loading processes:', err);
      error = err instanceof Error ? err.message : 'Failed to load processes';
    } finally {
      isLoading = false;
    }
  }

  async function handleDelete(processKey: string, processName: string) {
    if (deleteConfirm !== processKey) {
      deleteConfirm = processKey;
      return;
    }

    error = '';
    success = '';

    try {
      // Find the process definition by key
      const process = processes.find((p) => p.key === processKey);
      if (!process) {
        throw new Error('Process not found');
      }

      // Note: We need the process definition ID, which we don't have in the current API
      // For now, we'll show a message that this feature needs backend support
      error =
        'Process deletion is not yet implemented. The backend needs to be updated to support deletion by process key.';
      deleteConfirm = null;

      // TODO: Implement deletion when backend supports it
      // await api.deleteProcess(process.id, false);
      // success = `Process "${processName}" deleted successfully`;
      // await loadProcesses();
    } catch (err) {
      console.error('Error deleting process:', err);
      error = err instanceof Error ? err.message : 'Failed to delete process';
    } finally {
      deleteConfirm = null;
    }
  }

  function handleEdit(processKey: string) {
    // Navigate to designer with the process key
    goto(`/processes/designer?edit=${processKey}`);
  }

  function handleCreate() {
    goto('/processes/designer');
  }

  function cancelDelete() {
    deleteConfirm = null;
  }

  // Group processes by key to show all versions
  const groupedProcesses = $derived(() => {
    const groups = new Map<string, ProcessDefinition[]>();

    processes.forEach((process) => {
      if (!groups.has(process.key)) {
        groups.set(process.key, []);
      }
      groups.get(process.key)?.push(process);
    });

    // Sort versions within each group (highest version first)
    groups.forEach((versions) => {
      versions.sort((a, b) => (b.version || 0) - (a.version || 0));
    });

    return Array.from(groups.entries()).map(([key, versions]) => ({
      key,
      versions,
      latest: versions[0]
    }));
  });
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
                  onclick={() => handleEdit(key)}
                  class="rounded-md bg-blue-100 px-3 py-1.5 text-sm font-medium text-blue-700 hover:bg-blue-200"
                  title="View in designer"
                >
                  View
                </button>
                {#if deleteConfirm === key}
                  <div class="flex gap-2">
                    <button
                      onclick={() => handleDelete(key, latest.name || key)}
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
            >Deleting a process will remove all versions and may affect running process instances.</span
          >
        </li>
        <li class="flex items-start">
          <span class="mr-2">•</span>
          <span>Use the Process Designer to create new processes or modify existing ones.</span>
        </li>
        <li class="flex items-start">
          <span class="mr-2">•</span>
          <span
            >Process definitions are BPMN 2.0 compliant and can be exported/imported as XML files.</span
          >
        </li>
      </ul>
    </div>
  </div>
</div>
