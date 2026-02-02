<script lang="ts">
    import type { FormField } from '$lib/types';
    import { Editor } from '@tiptap/core';
    import StarterKit from '@tiptap/starter-kit';

    interface Props {
        field: FormField;
        value: unknown;
        isReadonly: boolean;
        error?: string;
        onchange: (value: string) => void;
    }

    const { field, value, isReadonly, error, onchange }: Props = $props();

    // Tiptap setup
    function setupTiptap(node: HTMLElement, { content, onUpdate, editable }: { content: string, onUpdate: (html: string) => void, editable: boolean }) {
        const editor = new Editor({
            element: node,
            extensions: [StarterKit],
            content: content || '',
            editable: editable,
            onUpdate: ({ editor }) => {
                onUpdate(editor.getHTML());
            },
            editorProps: {
                attributes: {
                    class: 'prose prose-sm focus:outline-none min-h-[100px] max-w-none'
                }
            }
        });

        return {
            destroy() {
                editor.destroy();
            },
            update(newParams: { content: string, editable: boolean }) {
                if (editor.isEditable !== newParams.editable) {
                    editor.setEditable(newParams.editable);
                }
                // Only update content if it's different to prevent cursor jumps
                if (newParams.content !== editor.getHTML()) {
                    // editor.commands.setContent(newParams.content || '');
                }
            }
        };
    }
</script>

{#if field.richText}
    <div class="border rounded-md {error ? 'border-red-500' : 'border-gray-300'} {isReadonly ? 'bg-gray-50' : 'bg-white'} overflow-hidden">
        {#if !isReadonly}
            <!-- Toolbar could go here -->
            <div class="border-b border-gray-200 bg-gray-50 px-2 py-1 text-xs text-gray-500">
                Rich Text Editor
            </div>
        {/if}
        <div
            class="p-3 min-h-[100px]"
            use:setupTiptap={{
                content: String(value ?? ''),
                editable: !isReadonly,
                onUpdate: (html: string) => onchange(html)
            }}
        ></div>
    </div>
{:else}
    <textarea
        id={`field-${field.name}`}
        value={String(value ?? '')}
        oninput={(e) => onchange(e.currentTarget.value)}
        placeholder={field.placeholder}
        readonly={isReadonly}
        rows="3"
        class="w-full px-3 py-2 border rounded-md focus:ring-2 focus:ring-blue-500 text-sm
            {error ? 'border-red-500' : 'border-gray-300'}
            {isReadonly ? 'bg-gray-50' : ''}"
    ></textarea>
{/if}

<style>
    :global(.ProseMirror) {
        outline: none;
    }

    :global(.ProseMirror p.is-editor-empty:first-child::before) {
      color: #adb5bd;
      content: attr(data-placeholder);
      float: left;
      height: 0;
      pointer-events: none;
    }
</style>
