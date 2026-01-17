<script lang="ts">
	import { page } from '$app/stores';
	import { ChevronRight, Home } from '@lucide/svelte';

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
        'expense': 'Expense',
        'leave': 'Leave',
        'purchase': 'Purchase',
        'project': 'Project',
        'task': 'Task'
	};

	function getBreadcrumbs(path: string) {
		const segments = path.split('/').filter(Boolean);
		let accumPath = '';

		return segments.map((segment, index) => {
			accumPath += `/${segment}`;
            let label = labelMap[segment];

            if (!label) {
                // Heuristic for IDs: long alphanumeric strings or UUIDs
                if (segment.length > 20 || /^\d+$/.test(segment) || /^[0-9a-f]{8}-/.test(segment)) {
                    label = 'Details';
                } else {
                    // Capitalize
                    label = segment.charAt(0).toUpperCase() + segment.slice(1).replace(/-/g, ' ');
                }
            }

			return {
				label,
				href: accumPath,
				isLast: index === segments.length - 1
			};
		});
	}

	const breadcrumbs = $derived(getBreadcrumbs($page.url.pathname));
</script>

{#if breadcrumbs.length > 0}
	<nav class="flex px-4 py-3 text-sm text-gray-600 border-b border-gray-200 bg-gray-50" aria-label="Breadcrumb">
		<ol class="flex items-center space-x-2">
			<li>
				<a href="/" class="text-gray-400 hover:text-gray-500">
					<Home class="w-4 h-4" />
					<span class="sr-only">Home</span>
				</a>
			</li>
			{#each breadcrumbs as crumb}
				<li>
					<div class="flex items-center">
						<ChevronRight class="w-4 h-4 text-gray-400 flex-shrink-0" />
						{#if crumb.isLast}
							<span class="ml-2 font-medium text-gray-900" aria-current="page">
								{crumb.label}
							</span>
						{:else}
							<a href={crumb.href} class="ml-2 font-medium text-gray-500 hover:text-gray-700">
								{crumb.label}
							</a>
						{/if}
					</div>
				</li>
			{/each}
		</ol>
	</nav>
{/if}
