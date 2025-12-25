<script lang="ts">
	import { goto } from '$app/navigation';
	import { onMount } from 'svelte';
	import { api } from '$lib/api/client';
	import { authStore } from '$lib/stores/auth.svelte';
	import Toast from '$lib/components/Toast.svelte';
	import type { User } from '$lib/types';

	let title = $state('');
	let description = $state('');
	let priority = $state('medium');
	let assignee = $state('');
	let submitting = $state(false);
	let users = $state<User[]>([]);
	let toast = $state<{ message: string; type: 'success' | 'error' } | null>(null);

	const priorities = [
		{ value: 'low', label: 'Low', color: 'text-green-600' },
		{ value: 'medium', label: 'Medium', color: 'text-yellow-600' },
		{ value: 'high', label: 'High', color: 'text-red-600' }
	];

	onMount(async () => {
		try {
			users = await api.getUsers();
		} catch (err) {
			console.error('Failed to load users:', err);
		}
	});

	async function handleSubmit(event: Event) {
		event.preventDefault();

		if (!title.trim()) {
			toast = { message: 'Please enter a task title', type: 'error' };
			return;
		}

		submitting = true;
		try {
			const response = await api.startProcess('task-assignment', {
				title,
				description,
				priority,
				assignee: assignee || null,
				createdBy: authStore.user?.username,
				createdDate: new Date().toISOString().split('T')[0]
			});

			toast = { message: 'Task created successfully!', type: 'success' };
			setTimeout(() => goto('/'), 1500);
		} catch (err) {
			toast = { message: err instanceof Error ? err.message : 'Failed to create task', type: 'error' };
		} finally {
			submitting = false;
		}
	}
</script>

<svelte:head>
	<title>Create Task - BPM Demo</title>
</svelte:head>

{#if toast}
	<Toast message={toast.message} type={toast.type} onClose={() => toast = null} />
{/if}

<div class="max-w-2xl mx-auto px-4 py-8">
	<a href="/processes" class="inline-flex items-center text-sm text-gray-600 hover:text-gray-900 mb-6">
		<svg class="w-4 h-4 mr-1" fill="none" viewBox="0 0 24 24" stroke="currentColor">
			<path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 19l-7-7 7-7" />
		</svg>
		Back to Processes
	</a>

	<div class="card">
		<div class="flex items-center space-x-4 mb-6">
			<div class="w-12 h-12 bg-amber-500 rounded-lg flex items-center justify-center text-white text-2xl">
				📋
			</div>
			<div>
				<h1 class="text-xl font-bold text-gray-900">Create Task</h1>
				<p class="text-gray-600">Assign a task to yourself or team members</p>
			</div>
		</div>

		<form onsubmit={handleSubmit} class="space-y-4">
			<div>
				<label for="title" class="label">Task Title *</label>
				<input
					id="title"
					type="text"
					bind:value={title}
					class="input"
					placeholder="Enter task title..."
					required
				/>
			</div>

			<div>
				<label for="description" class="label">Description</label>
				<textarea
					id="description"
					bind:value={description}
					rows="3"
					class="input"
					placeholder="Describe the task..."
				></textarea>
			</div>

			<div>
				<label class="label">Priority</label>
				<div class="flex space-x-4">
					{#each priorities as p}
						<label class="flex items-center space-x-2 cursor-pointer">
							<input
								type="radio"
								name="priority"
								value={p.value}
								bind:group={priority}
								class="text-blue-600"
							/>
							<span class="font-medium {p.color}">{p.label}</span>
						</label>
					{/each}
				</div>
			</div>

			<div>
				<label for="assignee" class="label">Assign To</label>
				<select id="assignee" bind:value={assignee} class="input">
					<option value="">Leave unassigned (anyone can claim)</option>
					{#each users as user}
						<option value={user.username}>{user.displayName} ({user.username})</option>
					{/each}
				</select>
				<p class="text-sm text-gray-500 mt-1">
					If left unassigned, any team member can claim this task.
				</p>
			</div>

			<div class="flex justify-end space-x-3 pt-4">
				<a href="/processes" class="btn btn-secondary">Cancel</a>
				<button
					type="submit"
					class="btn btn-success"
					disabled={submitting}
				>
					{submitting ? 'Creating...' : 'Create Task'}
				</button>
			</div>
		</form>
	</div>
</div>
