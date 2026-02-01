<script lang="ts">
	import Modal from './Modal.svelte';
	import { api } from '$lib/api/client';
	import type { User } from '$lib/types';
	import { onMount } from 'svelte';
	import { toast } from 'svelte-sonner';

	interface Props {
		open: boolean;
		taskId: string | null;
		currentAssignee?: string | null;
		onClose: () => void;
		onSuccess: () => void;
	}

	const { open, taskId, currentAssignee, onClose, onSuccess }: Props = $props();

	let users = $state<User[]>([]);
	let selectedUserId = $state('');
	let loading = $state(false);
	let submitting = $state(false);

	onMount(async () => {
		try {
			// api.getUsers() maps to GET /api/processes/users which returns all users
			// Note: The return type in client.ts says Promise<User[]> but let's verify if response structure matches
			// In TaskController it returns Map, but client.ts implies User[].
			// Wait, ProcessController.java: getUsers() returns userService.getAllUsers().
			// UserService usually returns List<UserDTO>.
			// client.ts: async getUsers(): Promise<User[]>
			// So it should be fine.
			users = await api.getUsers();
		} catch (e) {
			console.error('Failed to load users', e);
			toast.error('Failed to load users list');
		}
	});

	async function handleSubmit() {
		if (!taskId || !selectedUserId) return;

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
</script>

<Modal
	{open}
	title="Delegate Task"
	{onClose}
	maxWidth="sm"
>
	<div class="space-y-4">
		<p class="text-sm text-gray-500">
			Select a user to reassign this task to. You will no longer be responsible for this task.
		</p>

		<div>
			<label for="delegate-user" class="block text-sm font-medium text-gray-700 mb-1">
				Assign To
			</label>
			<select
				id="delegate-user"
				bind:value={selectedUserId}
				class="w-full rounded-md border-gray-300 shadow-sm focus:border-blue-500 focus:ring-blue-500"
				disabled={loading || submitting}
			>
				<option value="">Select a user...</option>
				{#each users as user}
					{#if user.username !== currentAssignee}
						<option value={user.username}>
							{user.firstName} {user.lastName} ({user.username})
						</option>
					{/if}
				{/each}
			</select>
		</div>

		<div class="flex justify-end gap-3 pt-4">
			<button
				onclick={onClose}
				class="px-4 py-2 text-sm font-medium text-gray-700 bg-white border border-gray-300 rounded-md hover:bg-gray-50"
				disabled={submitting}
			>
				Cancel
			</button>
			<button
				onclick={handleSubmit}
				class="px-4 py-2 text-sm font-medium text-white bg-blue-600 rounded-md hover:bg-blue-700 disabled:opacity-50"
				disabled={!selectedUserId || submitting}
			>
				{submitting ? 'Delegating...' : 'Delegate Task'}
			</button>
		</div>
	</div>
</Modal>
