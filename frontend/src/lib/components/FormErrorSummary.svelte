<script lang="ts">
	import { AlertCircle, X } from '@lucide/svelte';
	import { tick } from 'svelte';

	interface Props {
		errors: Record<string, string>;
		title?: string;
		dismissable?: boolean;
		onDismiss?: () => void;
		/** Map of field names to labels for better error messages */
		fieldLabels?: Record<string, string>;
		/** Auto-focus first error link when errors appear */
		autoFocus?: boolean;
	}

	const {
		errors,
		title = 'Please fix the following errors:',
		dismissable = false,
		onDismiss,
		fieldLabels = {},
		autoFocus = false
	}: Props = $props();

	const errorEntries = $derived(Object.entries(errors));
	const hasErrors = $derived(errorEntries.length > 0);

	let containerRef: HTMLDivElement | undefined = $state();
	let focusedIndex = $state(0);

	// Auto-focus first error when errors appear
	$effect(() => {
		if (hasErrors && autoFocus && containerRef) {
			tick().then(() => {
				const firstButton = containerRef?.querySelector('button[data-error-link]');
				if (firstButton instanceof HTMLElement) {
					firstButton.focus();
				}
			});
		}
	});

	function getFieldLabel(fieldName: string): string {
		return fieldLabels[fieldName] || formatFieldName(fieldName);
	}

	function formatFieldName(name: string): string {
		// Convert camelCase or snake_case to Title Case
		return name
			.replace(/([A-Z])/g, ' $1')
			.replace(/_/g, ' ')
			.replace(/^\w/, c => c.toUpperCase())
			.trim();
	}

	function scrollToField(fieldName: string) {
		const element = document.getElementById(`field-${fieldName}`)
			|| document.querySelector(`[name="${fieldName}"]`)
			|| document.querySelector(`#${fieldName}`);
		
		if (element) {
			element.scrollIntoView({ behavior: 'smooth', block: 'center' });
			// Focus the element if it's focusable
			if (element instanceof HTMLElement && 'focus' in element) {
				setTimeout(() => (element as HTMLElement).focus(), 300);
			}
		}
	}

	function handleKeydown(e: KeyboardEvent, index: number) {
		const buttons = containerRef?.querySelectorAll('button[data-error-link]');
		if (!buttons) return;

		let newIndex = index;

		if (e.key === 'ArrowDown' || e.key === 'ArrowRight') {
			e.preventDefault();
			newIndex = (index + 1) % buttons.length;
		} else if (e.key === 'ArrowUp' || e.key === 'ArrowLeft') {
			e.preventDefault();
			newIndex = (index - 1 + buttons.length) % buttons.length;
		} else if (e.key === 'Home') {
			e.preventDefault();
			newIndex = 0;
		} else if (e.key === 'End') {
			e.preventDefault();
			newIndex = buttons.length - 1;
		}

		if (newIndex !== index) {
			focusedIndex = newIndex;
			const targetButton = buttons[newIndex];
			if (targetButton instanceof HTMLElement) {
				targetButton.focus();
			}
		}
	}
</script>

{#if hasErrors}
	<div
		bind:this={containerRef}
		class="bg-red-50 border border-red-200 rounded-lg p-4 mb-6 animate-shake"
		role="alert"
		aria-live="polite"
		aria-labelledby="error-summary-title"
	>
		<div class="flex items-start gap-3">
			<div class="flex-shrink-0" aria-hidden="true">
				<AlertCircle class="w-5 h-5 text-red-500" />
			</div>
			<div class="flex-1 min-w-0">
				<h4 id="error-summary-title" class="text-sm font-medium text-red-800">
					{title}
					<span class="sr-only">({errorEntries.length} {errorEntries.length === 1 ? 'error' : 'errors'})</span>
				</h4>
				<ul class="mt-2 space-y-1" role="list" aria-label="Form errors">
					{#each errorEntries as [fieldName, message], index}
						<li class="text-sm text-red-700">
							<button
								type="button"
								data-error-link
								onclick={() => scrollToField(fieldName)}
								onkeydown={(e) => handleKeydown(e, index)}
								class="hover:underline text-left focus:outline-none focus:ring-2 focus:ring-red-500 focus:ring-offset-1 rounded px-1 -mx-1"
								aria-describedby="error-nav-hint"
							>
								<span class="font-medium">{getFieldLabel(fieldName)}:</span>
								{message}
							</button>
						</li>
					{/each}
				</ul>
				<p id="error-nav-hint" class="sr-only">
					Use arrow keys to navigate between errors. Press Enter to jump to the field.
				</p>
			</div>
			{#if dismissable && onDismiss}
				<button
					type="button"
					onclick={onDismiss}
					class="flex-shrink-0 text-red-400 hover:text-red-600 focus:outline-none focus:ring-2 focus:ring-red-500 rounded"
					aria-label="Dismiss error summary"
				>
					<X class="w-5 h-5" />
				</button>
			{/if}
		</div>
	</div>
{/if}

<style>
	@keyframes shake {
		0%, 100% {
			transform: translateX(0);
		}
		10%, 30%, 50%, 70%, 90% {
			transform: translateX(-2px);
		}
		20%, 40%, 60%, 80% {
			transform: translateX(2px);
		}
	}

	.animate-shake {
		animation: shake 0.5s ease-in-out;
	}
</style>
