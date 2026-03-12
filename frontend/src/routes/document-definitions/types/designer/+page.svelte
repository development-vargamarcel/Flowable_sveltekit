<script lang="ts">
  import { onMount } from 'svelte';
  import { page } from '$app/stores';
  import { goto } from '$app/navigation';
  import { api } from '$lib/api/client';
  import type { FormField, GridDefinition, ProcessFieldLibrary } from '$lib/types';
  import FieldLibraryPanel from '$lib/components/FieldLibraryPanel.svelte';
  import DynamicForm from '$lib/components/DynamicForm.svelte';
  import { getErrorMessage } from '$lib/utils/error-message';

  let key = $state('');
  let name = $state('');
  let description = $state('');
  let isEditMode = $state(false);
  let isLoading = $state(false);
  let isSaving = $state(false);
  let error = $state('');
  let success = $state('');

  let schema = $state<ProcessFieldLibrary>({
    fields: [],
    grids: []
  });

  let showPreview = $state(true);
  let previewValues = $state<Record<string, unknown>>({});
  const previewErrors = $state<Record<string, string>>({});

  function toProcessFieldLibrary(value: unknown): ProcessFieldLibrary {
    if (!value || typeof value !== 'object') {
      return { fields: [], grids: [] };
    }

    const candidate = value as { fields?: unknown; grids?: unknown };

    // We only need structural safety for preview/editor binding; detailed field validation happens server-side.
    return {
      fields: Array.isArray(candidate.fields) ? (candidate.fields as FormField[]) : [],
      grids: Array.isArray(candidate.grids) ? (candidate.grids as GridDefinition[]) : []
    };
  }

  onMount(async () => {
    const keyParam = $page.url.searchParams.get('key');
    if (keyParam) {
      isEditMode = true;
      key = keyParam;
      await loadDocumentType(keyParam);
    }
  });

  async function loadDocumentType(keyParam: string) {
    isLoading = true;
    try {
      const docType = await api.getDocumentType(keyParam);
      key = docType.key;
      name = docType.name;
      description = docType.description || '';
      if (docType.schemaJson) {
        schema = toProcessFieldLibrary(JSON.parse(docType.schemaJson));
      }
    } catch (err: unknown) {
      error = getErrorMessage(err, 'Failed to load document type');
      console.error(err);
    } finally {
      isLoading = false;
    }
  }

  async function handleSave() {
    if (!key || !name) {
      error = 'Key and Name are required';
      return;
    }

    isSaving = true;
    error = '';
    success = '';

    const payload = {
      key,
      name,
      description,
      schemaJson: JSON.stringify(schema)
    };

    try {
      if (isEditMode) {
        await api.updateDocumentType(key, payload);
        success = 'Document type updated successfully';
      } else {
        await api.createDocumentType(payload);
        success = 'Document type created successfully';
        isEditMode = true;
      }

      setTimeout(() => {
        goto('/documents/types');
      }, 1500);
    } catch (err: unknown) {
      error = getErrorMessage(err, 'Failed to save document type');
    } finally {
      isSaving = false;
    }
  }

  function handleSchemaChange(newSchema: ProcessFieldLibrary) {
    schema = newSchema;
  }
</script>

<div class="min-h-screen bg-gray-50 flex flex-col">
  <div class="bg-white border-b border-gray-200 px-6 py-4 flex items-center justify-between">
    <div>
      <h1 class="text-2xl font-bold text-gray-900">
        {isEditMode ? 'Edit Document Type' : 'Create Document Type'}
      </h1>
      <p class="text-sm text-gray-500">Define the structure for {name || 'a new document'}</p>
    </div>
    <div class="flex gap-3">
      <a
        href="/documents/types"
        class="rounded-md border border-gray-300 bg-white px-4 py-2 text-sm font-medium text-gray-700 hover:bg-gray-50"
      >
        Cancel
      </a>
      <button
        onclick={handleSave}
        disabled={isSaving}
        class="rounded-md bg-blue-600 px-4 py-2 text-sm font-medium text-white hover:bg-blue-700 disabled:opacity-50"
      >
        {isSaving ? 'Saving...' : 'Save Document Type'}
      </button>

      <button
        onclick={() => (showPreview = !showPreview)}
        class="rounded-md px-3 py-2 text-sm font-medium {showPreview
          ? 'bg-indigo-100 text-indigo-700'
          : 'bg-gray-100 text-gray-700 hover:bg-gray-200'}"
      >
        {showPreview ? 'Hide Preview' : 'Show Preview'}
      </button>
    </div>
  </div>

  <div class="flex-1 flex flex-col md:flex-row overflow-hidden">
    <div
      class="w-full md:w-80 bg-white border-r border-gray-200 p-6 overflow-y-auto md:h-full h-auto border-b md:border-b-0 flex-shrink-0"
    >
      <h3 class="font-semibold text-gray-900 mb-4">Basic Information</h3>

      <div class="space-y-4">
        <div>
          <label for="key" class="block text-sm font-medium text-gray-700 mb-1">Key (ID)</label>
          <input
            id="key"
            type="text"
            bind:value={key}
            disabled={isEditMode}
            class="w-full rounded-md border border-gray-300 px-3 py-2 text-sm focus:border-blue-500 focus:outline-none disabled:bg-gray-100"
            placeholder="e.g. invoice"
          />
          <p class="text-xs text-gray-500 mt-1">Unique identifier. Cannot be changed later.</p>
        </div>

        <div>
          <label for="name" class="block text-sm font-medium text-gray-700 mb-1">Name</label>
          <input
            id="name"
            type="text"
            bind:value={name}
            class="w-full rounded-md border border-gray-300 px-3 py-2 text-sm focus:border-blue-500 focus:outline-none"
            placeholder="e.g. Invoice Document"
          />
        </div>

        <div>
          <label for="description" class="block text-sm font-medium text-gray-700 mb-1"
            >Description</label
          >
          <textarea
            id="description"
            bind:value={description}
            rows="4"
            class="w-full rounded-md border border-gray-300 px-3 py-2 text-sm focus:border-blue-500 focus:outline-none"
            placeholder="Describe what this document type is used for..."
          ></textarea>
        </div>
      </div>

      {#if error}
        <div class="mt-6 rounded-md bg-red-50 p-4 text-sm text-red-700">
          {error}
        </div>
      {/if}

      {#if success}
        <div class="mt-6 rounded-md bg-green-50 p-4 text-sm text-green-700">
          {success}
        </div>
      {/if}
    </div>

    <div class="flex-1 overflow-y-auto bg-gray-50 p-4 md:p-6">
      <div class="h-full flex flex-col xl:flex-row gap-6">
        <div class="flex-1 min-w-0 flex flex-col gap-6">
          <div class="bg-white rounded-lg shadow p-6 flex-1">
            <h3 class="text-lg font-semibold text-gray-900 mb-4">Schema Definition</h3>
            <p class="text-sm text-gray-600 mb-6">
              Define the fields and grids that make up this document. These definitions can be used
              in process forms to automatically generate input fields.
            </p>

            {#if isLoading}
              <div class="text-center py-12">Loading...</div>
            {:else}
              <FieldLibraryPanel library={schema} onChange={handleSchemaChange} />
            {/if}
          </div>
        </div>

        {#if showPreview}
          <div class="w-full xl:w-96 flex-shrink-0 flex flex-col gap-6">
            <div class="bg-white rounded-lg shadow p-6 flex-1 overflow-y-auto xl:sticky xl:top-6">
              <div class="flex items-center justify-between mb-4">
                <h3 class="text-lg font-semibold text-gray-900">Live Preview</h3>
                <span
                  class="text-xs font-medium px-2 py-1 bg-indigo-50 text-indigo-700 rounded-full"
                  >Read-only Test</span
                >
              </div>

              <div
                class="p-4 border border-dashed border-gray-300 rounded-lg bg-gray-50 min-h-[300px]"
              >
                <DynamicForm
                  fields={schema.fields}
                  grids={schema.grids}
                  gridConfig={{ columns: 1, gap: 16 }}
                  values={previewValues}
                  errors={previewErrors}
                  onValuesChange={(vals) => (previewValues = vals)}
                />
              </div>

              <div class="mt-6 p-4 bg-gray-100 rounded text-xs font-mono overflow-x-auto">
                <p class="font-bold text-gray-500 mb-2">Form Data (JSON):</p>
                <pre>{JSON.stringify(previewValues, null, 2)}</pre>
              </div>
            </div>
          </div>
        {/if}
      </div>
    </div>
  </div>
</div>
