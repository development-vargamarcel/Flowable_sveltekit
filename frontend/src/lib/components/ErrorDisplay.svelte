<script lang="ts">
	import { ApiError } from '$lib/api/client';
	import { toast } from 'svelte-sonner';

	interface Props {
		error: ApiError | string | Error | null;
		onRetry?: () => void;
		title?: string;
	}

	const { error, onRetry, title = 'Error' }: Props = $props();

	function copyErrorDetails() {
		if (error instanceof ApiError) {
			const details = JSON.stringify(
				{
					message: error.message,
					status: error.status,
					details: error.details,
					fieldErrors: error.fieldErrors,
					timestamp: error.timestamp
				},
				null,
				2
			);
			navigator.clipboard.writeText(details);
			toast.success('Error details copied to clipboard');
		} else {
			navigator.clipboard.writeText(String(error));
			toast.success('Error message copied to clipboard');
		}
	}

	function getErrorMessage(err: any): string {
		if (err instanceof ApiError) {
			return err.getFullMessage();
		}
		if (err instanceof Error) {
			return err.message;
		}
		return String(err);
	}
</script>

{#if error}
	<div class="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded-lg" role="alert">
		<div class="flex items-start">
			<div class="flex-1">
				<h3 class="font-bold">{title}</h3>
				<p class="mt-1">
					{getErrorMessage(error)}
				</p>
				{#if error instanceof ApiError && error.timestamp}
					<p class="mt-2 text-xs text-red-500">Timestamp: {error.timestamp}</p>
				{/if}
			</div>
			<div class="ml-4 flex flex-col gap-2">
				{#if onRetry}
					<button
						onclick={onRetry}
						class="px-3 py-1 bg-red-100 hover:bg-red-200 rounded text-sm transition-colors"
					>
						Retry
					</button>
				{/if}
				<button
					onclick={copyErrorDetails}
					class="px-3 py-1 bg-red-100 hover:bg-red-200 rounded text-sm transition-colors"
				>
					Copy Error
				</button>
			</div>
		</div>
	</div>
{/if}
