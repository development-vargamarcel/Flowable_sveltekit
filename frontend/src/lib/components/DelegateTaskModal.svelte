<script lang="ts">
	import { api } from '$lib/api/client';
	import type { User } from '$lib/types';
	import Modal from './Modal.svelte';
	import { toast } from 'svelte-sonner';
	import { onMount } from 'svelte';

	interface Props {
		open: boolean;
		taskId: string;
		currentAssignee?: string;
		onClose: () => void;
		onSuccess: () => void;
	}

	const { open, taskId, currentAssignee, onClose, onSuccess }: Props = $props();

	let users = $state<User[]>([]);
	let selectedUserId = $state('');
	let loading = $state(false);
	let submitting = $state(false);

	onMount(async () => {
		loadUsers();
	});

	async function loadUsers() {
		loading = true;
		try {
			users = await api.getUsers();
		} catch (e) {
			console.error(e);
			toast.error('Failed to load users');
		} finally {
			loading = false;
		}
	}

	async function handleDelegate() {
		if (!selectedUserId) return;

		submitting = true;
		try {
			await api.delegateTask(taskId, selectedUserId);
			toast.success('Task delegated successfully');
			onSuccess();
			onClose();
		} catch (e) {
			console.error(e);
			toast.error(e instanceof Error ? e.message : 'Failed to delegate task');
		} finally {
			submitting = false;
		}
	}

	// Filter out current user from the list if possible, or just show all
	const availableUsers = $derived(
		users.filter((u) => u.username !== currentAssignee)
	);
</script>

<Modal {open} title="Delegate Task" {onClose} maxWidth="md">
	<div class="space-y-4">
		<p class="text-sm text-gray-600 dark:text-gray-300">
			Select a user to reassign this task to. They will become the new owner.
		</p>

		{#if loading}
			<div class="py-4 text-center">Loading users...</div>
		{:else}
			<div>
				<label for="delegate-user" class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
					Select User
				</label>
				<select
					id="delegate-user"
					bind:value={selectedUserId}
					class="w-full rounded-md border-gray-300 shadow-sm focus:border-blue-500 focus:ring-blue-500 dark:bg-gray-700 dark:border-gray-600 dark:text-white sm:text-sm py-2 px-3 border"
				>
					<option value="" disabled>Select a user...</option>
					{#each availableUsers as user}
						<option value={user.username}>
							{user.displayName || user.username} ({user.email || user.username})
						</option>
					{/each}
				</select>
			</div>
		{/if}

		<div class="flex justify-end space-x-3 pt-4 border-t border-gray-100 dark:border-gray-700 mt-4">
			<button
				onclick={onClose}
				class="px-4 py-2 text-sm font-medium text-gray-700 bg-white border border-gray-300 rounded-md hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500 dark:bg-gray-700 dark:text-gray-200 dark:border-gray-600 dark:hover:bg-gray-600"
			>
				Cancel
			</button>
			<button
				onclick={handleDelegate}
				disabled={!selectedUserId || submitting}
				class="px-4 py-2 text-sm font-medium text-white bg-blue-600 border border-transparent rounded-md hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500 disabled:opacity-50 disabled:cursor-not-allowed"
			>
				{submitting ? 'Delegating...' : 'Delegate'}
			</button>
		</div>
	</div>
</Modal>
