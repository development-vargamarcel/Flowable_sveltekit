<script lang="ts">
	interface Props {
		count: number;
		showLabel?: boolean;
	}

	let { count, showLabel = false }: Props = $props();
</script>

<span
	class="inline-flex items-center gap-1 px-1.5 py-0.5 rounded text-xs font-medium bg-amber-100 text-amber-800 ml-1"
	title="{count} escalation(s)"
>
	<svg class="w-3 h-3" fill="none" viewBox="0 0 24 24" stroke="currentColor">
		<path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 10l7-7m0 0l7 7m-7-7v18" />
	</svg>
	{count}
	{#if showLabel}
		<span>escalation{count !== 1 ? 's' : ''}</span>
	{/if}
</span>
