<script lang="ts">
	import type { Snippet } from 'svelte';

	interface Props {
		open: boolean;
		title: string;
		onClose: () => void;
		children: Snippet;
		footer?: Snippet;
		maxWidth?: 'sm' | 'md' | 'lg' | 'xl';
	}

	const { open, title, onClose, children, footer, maxWidth = 'md' }: Props = $props();

	const widthClasses: Record<string, string> = {
		sm: 'max-w-sm',
		md: 'max-w-md',
		lg: 'max-w-lg',
		xl: 'max-w-xl'
	};

	function handleBackdropClick(e: MouseEvent) {
		if (e.target === e.currentTarget) {
			onClose();
		}
	}

	function handleKeydown(e: KeyboardEvent) {
		if (e.key === 'Escape') {
			onClose();
		}
	}
</script>

<svelte:window onkeydown={handleKeydown} />

{#if open}
	<div
		class="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 p-4"
		onclick={handleBackdropClick}
		onkeydown={handleKeydown}
		role="dialog"
		aria-modal="true"
		aria-labelledby="modal-title"
		tabindex="-1"
	>
		<div class="bg-white rounded-lg shadow-xl w-full {widthClasses[maxWidth]} p-6">
			<div class="flex items-center justify-between mb-4">
				<h3 id="modal-title" class="text-lg font-bold text-gray-900">{title}</h3>
				<button
					onclick={onClose}
					class="text-gray-400 hover:text-gray-600"
					aria-label="Close modal"
				>
					<svg class="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
						<path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
					</svg>
				</button>
			</div>

			<div>
				{@render children()}
			</div>

			{#if footer}
				<div class="flex gap-3 mt-6">
					{@render footer()}
				</div>
			{/if}
		</div>
	</div>
{/if}
