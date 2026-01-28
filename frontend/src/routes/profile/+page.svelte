<script lang="ts">
	import { onMount } from 'svelte';
	import { api } from '$lib/api/client';
	import { authStore } from '$lib/stores/auth.svelte';
	import { toast } from 'svelte-sonner';

	let loading = $state(false);
	let firstName = $state('');
	let lastName = $state('');
	let email = $state('');

	onMount(() => {
		if (authStore.user) {
			firstName = authStore.user.firstName || '';
			lastName = authStore.user.lastName || '';
			email = authStore.user.email || '';
		}
	});

	async function handleSubmit(e: Event) {
		e.preventDefault();
		loading = true;

		try {
			const updatedUser = await api.updateProfile({
				firstName,
				lastName,
				email
			});
			authStore.setUser(updatedUser);
			toast.success('Profile updated successfully');
		} catch (error) {
			console.error('Failed to update profile:', error);
			// Toast is handled by api client
		} finally {
			loading = false;
		}
	}
</script>

<div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
	<div class="max-w-2xl mx-auto">
		<h1 class="text-2xl font-semibold text-gray-900 mb-6">Profile Settings</h1>

		<div class="bg-white shadow rounded-lg p-6">
			<form onsubmit={handleSubmit} class="space-y-6">
				<div>
					<label for="username" class="block text-sm font-medium text-gray-700">Username</label>
					<div class="mt-1">
						<input
							type="text"
							id="username"
							value={authStore.user?.username}
							disabled
							class="block w-full rounded-md border-gray-300 bg-gray-50 shadow-sm focus:border-blue-500 focus:ring-blue-500 sm:text-sm"
						/>
					</div>
				</div>

				<div class="grid grid-cols-1 gap-y-6 gap-x-4 sm:grid-cols-2">
					<div>
						<label for="firstName" class="block text-sm font-medium text-gray-700">First Name</label>
						<div class="mt-1">
							<input
								type="text"
								id="firstName"
								bind:value={firstName}
								class="block w-full rounded-md border-gray-300 shadow-sm focus:border-blue-500 focus:ring-blue-500 sm:text-sm"
							/>
						</div>
					</div>

					<div>
						<label for="lastName" class="block text-sm font-medium text-gray-700">Last Name</label>
						<div class="mt-1">
							<input
								type="text"
								id="lastName"
								bind:value={lastName}
								class="block w-full rounded-md border-gray-300 shadow-sm focus:border-blue-500 focus:ring-blue-500 sm:text-sm"
							/>
						</div>
					</div>
				</div>

				<div>
					<label for="email" class="block text-sm font-medium text-gray-700">Email</label>
					<div class="mt-1">
						<input
							type="email"
							id="email"
							bind:value={email}
							class="block w-full rounded-md border-gray-300 shadow-sm focus:border-blue-500 focus:ring-blue-500 sm:text-sm"
						/>
					</div>
				</div>

				<div>
					<span class="block text-sm font-medium text-gray-700">Roles</span>
					<div class="mt-2 flex flex-wrap gap-2">
						{#if authStore.user?.roles}
							{#each authStore.user.roles as role}
								<span
									class="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-blue-100 text-blue-800"
								>
									{role}
								</span>
							{/each}
						{/if}
					</div>
				</div>

				<div class="flex justify-end pt-4">
					<button
						type="submit"
						disabled={loading}
						class="inline-flex justify-center rounded-md border border-transparent bg-blue-600 py-2 px-4 text-sm font-medium text-white shadow-sm hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:ring-offset-2 disabled:opacity-50"
					>
						{#if loading}
							Saving...
						{:else}
							Save Changes
						{/if}
					</button>
				</div>
			</form>
		</div>
	</div>
</div>
