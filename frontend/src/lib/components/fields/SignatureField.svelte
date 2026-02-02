<script lang="ts">
    import type { FormField } from '$lib/types';
    import SignaturePad from 'signature_pad';

    interface Props {
        field: FormField;
        value: unknown;
        isReadonly: boolean;
        error?: string;
        onchange: (value: string | null) => void;
    }

    const { value, isReadonly, error, onchange }: Props = $props();

    // Signature Pad setup
    function setupSignaturePad(node: HTMLCanvasElement, { value, onUpdate, readonly }: { value: string | null, onUpdate: (dataUrl: string) => void, readonly: boolean }) {
        const pad = new SignaturePad(node);

        if (value) {
            pad.fromDataURL(value);
        }

        if (readonly) {
            pad.off();
        }

        // Handle resize properly
        const resizeCanvas = () => {
            const ratio = Math.max(window.devicePixelRatio || 1, 1);
            node.width = node.offsetWidth * ratio;
            node.height = node.offsetHeight * ratio;
            node.getContext("2d")?.scale(ratio, ratio);
            if (value) pad.fromDataURL(value); // Reload data after resize
        };

        // Initial resize
        setTimeout(resizeCanvas, 0);
        // window.addEventListener('resize', resizeCanvas); // Removed to avoid listener leak for now

        pad.addEventListener('endStroke', () => {
            if (!readonly) {
                onUpdate(pad.toDataURL());
            }
        });

        return {
            destroy() {
                pad.off();
            },
            update(newParams: { readonly: boolean }) {
                if (newParams.readonly) pad.off(); else pad.on();
            }
        };
    }
</script>

<div class="border rounded-md {error ? 'border-red-500' : 'border-gray-300'} bg-white">
    <canvas
        class="w-full h-40 touch-none"
        use:setupSignaturePad={{
            value: value,
            readonly: isReadonly,
            onUpdate: (dataUrl: string) => onchange(dataUrl)
        }}
    ></canvas>
    {#if !isReadonly}
        <div class="border-t border-gray-200 p-2 text-right bg-gray-50">
            <button
                class="text-xs text-gray-500 hover:text-red-500"
                onclick={() => onchange(null)}
            >
                Clear Signature
            </button>
        </div>
    {/if}
</div>
