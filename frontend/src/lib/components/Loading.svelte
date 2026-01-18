<script lang="ts">
	interface Props {
		text?: string;
		fullScreen?: boolean;
	}

	const { text = 'Loading...', fullScreen = false }: Props = $props();
</script>

{#if fullScreen}
	<div class="flex-1 flex items-center justify-center min-h-[50vh]">
		<div class="text-center">
			<div
				class="w-12 h-12 border-4 border-blue-600 border-t-transparent rounded-full animate-spin mx-auto"
			></div>
			<p class="mt-4 text-gray-600">{text}</p>
		</div>
	</div>
{:else}
	<div class="text-center py-12">
		<div
			class="w-12 h-12 border-4 border-blue-600 border-t-transparent rounded-full animate-spin mx-auto"
		></div>
		<p class="mt-4 text-gray-600">{text}</p>
	</div>
{/if}
