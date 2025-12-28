<script lang="ts">
	import { getProcessCardHoverClasses, getIconBgClass, type ColorName } from '$lib/utils/theme';

	interface Props {
		title: string;
		description: string;
		icon: string;
		color: ColorName;
		href: string;
	}

	const { title, description, icon, color, href }: Props = $props();
</script>

<a
	{href}
	class="block card border-2 transition-all {getProcessCardHoverClasses(color)}"
>
	<div class="flex items-start space-x-4">
		<div class="w-12 h-12 rounded-lg flex items-center justify-center text-white text-2xl {getIconBgClass(color)}">
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
