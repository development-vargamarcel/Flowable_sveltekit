<script lang="ts">
  import { onMount } from 'svelte';
  import { api } from '$lib/api/client';
  import { demoDocumentTypes } from '$lib/utils/demo-document-types';

  let documentTypes = $state<any[]>([]);
  let isLoading = $state(true);
  let error = $state('');

  onMount(async () => {
    await loadDocumentTypes();
  });

  async function loadDocumentTypes() {
    isLoading = true;
    error = '';
    try {
      documentTypes = await api.getDocumentTypes();
    } catch (err) {
      console.error('Error loading document types:', err);
      error = 'Failed to load document types';
    } finally {
      isLoading = false;
    }
  }

  async function handleDelete(key: string) {
    if (!confirm('Are you sure you want to delete this document type?')) return;

    try {
      await api.deleteDocumentType(key);
      await loadDocumentTypes();
    } catch (err) {
      error = 'Failed to delete document type';
    }
  }

  async function handleLoadDemos() {
    isLoading = true;
    error = '';
    let loadCount = 0;

    try {
      // First, get existing types to avoid duplicates/errors
      const existing = await api.getDocumentTypes();
      const existingKeys = new Set(existing.map((d) => d.key));

      for (const demo of demoDocumentTypes) {
        if (!existingKeys.has(demo.key)) {
          await api.createDocumentType({
            key: demo.key,
            name: demo.name,
            description: demo.description,
            schemaJson: JSON.stringify(demo.schema)
          });
          loadCount++;
        }
      }

      await loadDocumentTypes();
      if (loadCount === 0) {
        alert('All demo types are already installed.');
      }
    } catch (err) {
      console.error('Error loading demos:', err);
      error = 'Failed to load some demo document types';
    } finally {
      isLoading = false;
    }
  }
</script>

<div class="min-h-screen bg-gray-50 p-6">
  <div class="mx-auto max-w-6xl">
    <div class="mb-6 flex items-center justify-between">
      <div>
        <h1 class="text-3xl font-bold text-gray-900">Document Types</h1>
        <p class="mt-2 text-gray-600">Define reusable document structures (fields and grids)</p>
      </div>
      <div class="flex gap-2">
        <button
          onclick={handleLoadDemos}
          class="rounded-md bg-white border border-gray-300 px-4 py-2 text-sm font-medium text-gray-700 hover:bg-gray-50"
        >
          Load Demo Content
        </button>
        <a
          href="/document-types/types/designer"
          class="rounded-md bg-blue-600 px-4 py-2 text-sm font-medium text-white hover:bg-blue-700"
        >
          + Create Document Type
        </a>
      </div>
    </div>

    {#if error}
      <div class="mb-4 rounded-md bg-red-50 p-4 text-sm text-red-700">{error}</div>
    {/if}

    {#if isLoading}
      <div class="text-center py-12">Loading...</div>
    {:else if documentTypes.length === 0}
      <div class="rounded-lg bg-white p-12 text-center shadow">
        <p class="text-gray-500">No document types defined yet.</p>
      </div>
    {:else}
      <div class="grid gap-6 md:grid-cols-2 lg:grid-cols-3">
        {#each documentTypes as docType}
          <div class="rounded-lg bg-white p-6 shadow">
            <h3 class="text-lg font-semibold text-gray-900">{docType.name}</h3>
            <p class="text-sm text-gray-500 mb-2">Key: {docType.key}</p>
            <p class="text-sm text-gray-600 mb-4 line-clamp-2">
              {docType.description || 'No description'}
            </p>

            <div class="flex justify-end gap-2">
              <a
                href={`/document-types/types/designer?key=${docType.key}`}
                class="rounded bg-blue-100 px-3 py-1.5 text-sm font-medium text-blue-700 hover:bg-blue-200"
              >
                Edit
              </a>
              <button
                onclick={() => handleDelete(docType.key)}
                class="rounded bg-red-100 px-3 py-1.5 text-sm font-medium text-red-700 hover:bg-red-200"
              >
                Delete
              </button>
            </div>
          </div>
        {/each}
      </div>
    {/if}
  </div>
</div>
