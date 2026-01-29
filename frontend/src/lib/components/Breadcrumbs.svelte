<script lang="ts">
	import { page } from '$app/stores';
	import { ChevronRight, Home, RefreshCw } from '@lucide/svelte';
	import { getBreadcrumbLabel } from '$lib/stores/breadcrumbContext.svelte';

	interface Props {
		/** Show loading state for the current page */
		loading?: boolean;
		/** Custom label overrides for specific segments */
		overrides?: Record<string, string>;
	}

	const { loading = false, overrides = {} }: Props = $props();

	const labelMap: Record<string, string> = {
		dashboard: 'Dashboard',
		tasks: 'Tasks',
		processes: 'Processes',
		'document-definitions': 'Document Types',
		database: 'Database',
		login: 'Login',
		compare: 'Compare',
		designer: 'Designer',
		docs: 'Documentation',
		manage: 'Manage',
		start: 'Start',
		expense: 'Expense',
		leave: 'Leave',
		purchase: 'Purchase',
		project: 'Project',
		task: 'Task',
		profile: 'Profile',
		notifications: 'Notifications',
		seed: 'Seed Data',
		types: 'Types',
		'process-instances': 'Process Instances'
	};

	function getSegmentLabel(segment: string): string {
		// Priority: 1. Props override, 2. Context store, 3. Static map, 4. Auto-format
		if (overrides[segment]) {
			return overrides[segment];
		}

		const contextLabel = getBreadcrumbLabel(segment);
		if (contextLabel) {
			return contextLabel;
		}

		const staticLabel = labelMap[segment];
		if (staticLabel) {
			return staticLabel;
		}

		// Heuristic for IDs: long alphanumeric strings or UUIDs
		if (segment.length > 20 || /^\d+$/.test(segment) || /^[0-9a-f]{8}-/.test(segment)) {
			return 'Details';
		}

		// Auto-format: capitalize and replace hyphens with spaces
		return segment.charAt(0).toUpperCase() + segment.slice(1).replace(/-/g, ' ');
	}

	function getBreadcrumbs(path: string) {
		const segments = path.split('/').filter(Boolean);
		let accumPath = '';

		return segments.map((segment, index) => {
			accumPath += `/${segment}`;
			const label = getSegmentLabel(segment);

			return {
				label,
				href: accumPath,
				isLast: index === segments.length - 1,
				segment
			};
		});
	}

	const breadcrumbs = $derived(getBreadcrumbs($page.url.pathname));
</script>

{#if breadcrumbs.length > 0}
	<nav class="flex px-4 py-3 text-sm text-gray-600 border-b border-gray-200 bg-gray-50" aria-label="Breadcrumb">
		<ol class="flex items-center space-x-2">
			<li>
				<a href="/" class="text-gray-400 hover:text-gray-500 transition-colors">
					<Home class="w-4 h-4" />
					<span class="sr-only">Home</span>
				</a>
			</li>
			{#each breadcrumbs as crumb}
				<li>
					<div class="flex items-center">
						<ChevronRight class="w-4 h-4 text-gray-400 flex-shrink-0" />
						{#if crumb.isLast}
							<span class="ml-2 font-medium text-gray-900 flex items-center gap-2" aria-current="page">
								{crumb.label}
								{#if loading}
									<RefreshCw class="w-3 h-3 text-blue-500 animate-spin" />
								{/if}
							</span>
						{:else}
							<a
								href={crumb.href}
								class="ml-2 font-medium text-gray-500 hover:text-gray-700 transition-colors"
								title="Navigate to {crumb.label}"
							>
								{crumb.label}
							</a>
						{/if}
					</div>
				</li>
			{/each}
		</ol>
	</nav>
{/if}
