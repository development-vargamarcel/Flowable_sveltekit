<script lang="ts">
	interface Props {
		title: string;
		description: string;
		icon: string;
		color: string;
		href: string;
	}

	let { title, description, icon, color, href }: Props = $props();

	function getColorClasses(color: string): string {
		switch (color) {
			case 'green':
				return 'bg-emerald-50 border-emerald-200 hover:bg-emerald-100';
			case 'blue':
				return 'bg-sky-50 border-sky-200 hover:bg-sky-100';
			case 'amber':
				return 'bg-amber-50 border-amber-200 hover:bg-amber-100';
			default:
				return 'bg-gray-50 border-gray-200 hover:bg-gray-100';
		}
	}

	function getIconColorClasses(color: string): string {
		switch (color) {
			case 'green':
				return 'bg-emerald-500';
			case 'blue':
				return 'bg-sky-500';
			case 'amber':
				return 'bg-amber-500';
			default:
				return 'bg-gray-500';
		}
	}
</script>

<a
	{href}
	class="block card border-2 transition-all {getColorClasses(color)}"
>
	<div class="flex items-start space-x-4">
		<div class="w-12 h-12 rounded-lg flex items-center justify-center text-white text-2xl {getIconColorClasses(color)}">
			{icon}
		</div>
		<div class="flex-1">
			<h3 class="font-semibold text-gray-900">{title}</h3>
			<p class="text-sm text-gray-600 mt-1">{description}</p>
		</div>
		<svg class="w-5 h-5 text-gray-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
			<path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7" />
		</svg>
	</div>
</a>
