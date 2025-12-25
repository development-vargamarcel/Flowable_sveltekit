<script lang="ts">
	interface Props {
		message: string;
		type?: 'success' | 'error' | 'info';
		onClose: () => void;
	}

	let { message, type = 'info', onClose }: Props = $props();

	function getTypeClasses(type: string): string {
		switch (type) {
			case 'success':
				return 'bg-green-50 border-green-200 text-green-800';
			case 'error':
				return 'bg-red-50 border-red-200 text-red-800';
			default:
				return 'bg-blue-50 border-blue-200 text-blue-800';
		}
	}

	function getIcon(type: string): string {
		switch (type) {
			case 'success':
				return 'M5 13l4 4L19 7';
			case 'error':
				return 'M6 18L18 6M6 6l12 12';
			default:
				return 'M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z';
		}
	}

	$effect(() => {
		const timer = setTimeout(onClose, 5000);
		return () => clearTimeout(timer);
	});
</script>

<div class="fixed bottom-4 right-4 z-50 animate-slide-up">
	<div class="flex items-center space-x-3 px-4 py-3 rounded-lg border shadow-lg {getTypeClasses(type)}">
		<svg class="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
			<path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d={getIcon(type)} />
		</svg>
		<span class="font-medium">{message}</span>
		<button onclick={onClose} class="ml-2 hover:opacity-70">
			<svg class="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
				<path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
			</svg>
		</button>
	</div>
</div>

<style>
	@keyframes slide-up {
		from {
			transform: translateY(100%);
			opacity: 0;
		}
		to {
			transform: translateY(0);
			opacity: 1;
		}
	}

	.animate-slide-up {
		animation: slide-up 0.3s ease-out;
	}
</style>
