<script lang="ts">
	/**
	 * Skeleton loader component for loading states
	 * Provides content-aware placeholders while data is loading
	 */
	interface Props {
		variant?: 'text' | 'circular' | 'rectangular' | 'card' | 'stat-card' | 'table-row';
		width?: string;
		height?: string;
		lines?: number;
		className?: string;
	}

	const {
		variant = 'text',
		width = '100%',
		height,
		lines = 1,
		className = ''
	}: Props = $props();

	const baseClasses = 'animate-pulse bg-gray-200 rounded';

	function getVariantClasses(): string {
		switch (variant) {
			case 'circular':
				return 'rounded-full';
			case 'rectangular':
				return 'rounded-lg';
			case 'card':
				return 'rounded-lg';
			case 'stat-card':
				return 'rounded-lg';
			case 'table-row':
				return 'rounded';
			default:
				return 'rounded';
		}
	}

	function getDefaultHeight(): string {
		if (height) return height;
		switch (variant) {
			case 'circular':
				return '40px';
			case 'card':
				return '120px';
			case 'stat-card':
				return '80px';
			case 'table-row':
				return '48px';
			default:
				return '16px';
		}
	}
</script>

{#if variant === 'stat-card'}
	<!-- Stat Card Skeleton -->
	<div class="bg-white rounded-lg shadow p-4 {className}" style="width: {width};">
		<div class="{baseClasses} h-3 w-20 mb-3"></div>
		<div class="{baseClasses} h-8 w-16 mb-2"></div>
		<div class="{baseClasses} h-3 w-24"></div>
	</div>
{:else if variant === 'card'}
	<!-- Card Skeleton -->
	<div class="bg-white rounded-lg shadow p-4 {className}" style="width: {width};">
		<div class="flex items-start gap-3">
			<div class="{baseClasses} rounded-full h-10 w-10 flex-shrink-0"></div>
			<div class="flex-1">
				<div class="{baseClasses} h-4 w-3/4 mb-2"></div>
				<div class="{baseClasses} h-3 w-1/2"></div>
			</div>
		</div>
		<div class="mt-4 space-y-2">
			<div class="{baseClasses} h-3 w-full"></div>
			<div class="{baseClasses} h-3 w-5/6"></div>
		</div>
	</div>
{:else if variant === 'table-row'}
	<!-- Table Row Skeleton -->
	<tr class="{className}">
		<td class="px-4 py-3">
			<div class="flex items-center gap-2">
				<div class="{baseClasses} h-8 w-8 rounded-full"></div>
				<div>
					<div class="{baseClasses} h-4 w-32 mb-1"></div>
					<div class="{baseClasses} h-3 w-20"></div>
				</div>
			</div>
		</td>
		<td class="px-4 py-3"><div class="{baseClasses} h-4 w-24"></div></td>
		<td class="px-4 py-3"><div class="{baseClasses} h-6 w-16 rounded-full"></div></td>
		<td class="px-4 py-3"><div class="{baseClasses} h-4 w-28"></div></td>
		<td class="px-4 py-3"><div class="{baseClasses} h-6 w-14 rounded"></div></td>
		<td class="px-4 py-3"><div class="{baseClasses} h-4 w-24"></div></td>
		<td class="px-4 py-3"><div class="{baseClasses} h-4 w-16"></div></td>
		<td class="px-4 py-3">
			<div class="flex gap-2">
				<div class="{baseClasses} h-6 w-14 rounded"></div>
				<div class="{baseClasses} h-6 w-16 rounded"></div>
			</div>
		</td>
	</tr>
{:else}
	<!-- Default/Text Skeleton -->
	{#each Array(lines) as _, i}
		<div
			class="{baseClasses} {getVariantClasses()} {className}"
			style="width: {i === lines - 1 && lines > 1 ? '75%' : width}; height: {getDefaultHeight()};"
		></div>
	{/each}
{/if}
