<script lang="ts">
	import type { Snippet } from 'svelte';

	interface Props {
		message?: string;
		children?: Snippet;
		icon?: Snippet;
	}

	const { message = 'No items found', children, icon }: Props = $props();
</script>

<div class="text-center py-12">
	<div class="flex flex-col items-center justify-center text-gray-500">
		{#if icon}
			{@render icon()}
		{:else}
			<svg
				class="w-12 h-12 text-gray-400 mb-3"
				fill="none"
				viewBox="0 0 24 24"
				stroke="currentColor"
				aria-hidden="true"
				role="img"
			>
				<path
					stroke-linecap="round"
					stroke-linejoin="round"
					stroke-width="1.5"
					d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"
				/>
			</svg>
		{/if}
		<p>{message}</p>
		{#if children}
			<div class="mt-4">
				{@render children()}
			</div>
		{/if}
	</div>
</div>
