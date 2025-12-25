<script lang="ts">
	import { goto } from '$app/navigation';
	import { api } from '$lib/api/client';
	import { authStore } from '$lib/stores/auth.svelte';

	let username = $state('');
	let password = $state('');
	let error = $state('');
	let loading = $state(false);

	async function handleSubmit(event: Event) {
		event.preventDefault();
		error = '';
		loading = true;

		try {
			const response = await api.login({ username, password });
			authStore.setUser(response.user);
			goto('/');
		} catch (err) {
			error = err instanceof Error ? err.message : 'Login failed';
		} finally {
			loading = false;
		}
	}

	function selectUser(user: string) {
		username = user;
		password = 'password';
	}
</script>

<svelte:head>
	<title>Login - BPM Demo</title>
</svelte:head>

<div class="min-h-screen flex items-center justify-center bg-gradient-to-br from-blue-50 to-indigo-100 px-4">
	<div class="max-w-md w-full">
		<div class="text-center mb-8">
			<div class="w-16 h-16 bg-blue-600 rounded-2xl flex items-center justify-center mx-auto mb-4">
				<span class="text-white font-bold text-2xl">BPM</span>
			</div>
			<h1 class="text-2xl font-bold text-gray-900">Flowable + SvelteKit Demo</h1>
			<p class="text-gray-600 mt-2">Business Process Management</p>
		</div>

		<div class="card">
			<form onsubmit={handleSubmit} class="space-y-4">
				{#if error}
					<div class="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded-lg text-sm">
						{error}
					</div>
				{/if}

				<div>
					<label for="username" class="label">Username</label>
					<input
						id="username"
						type="text"
						bind:value={username}
						class="input"
						placeholder="Enter username"
						required
					/>
				</div>

				<div>
					<label for="password" class="label">Password</label>
					<input
						id="password"
						type="password"
						bind:value={password}
						class="input"
						placeholder="Enter password"
						required
					/>
				</div>

				<button
					type="submit"
					class="w-full btn btn-primary"
					disabled={loading}
				>
					{loading ? 'Signing in...' : 'Sign In'}
				</button>
			</form>

			<div class="mt-6 pt-6 border-t border-gray-200">
				<p class="text-sm text-gray-600 mb-3 text-center">Quick login (password: "password")</p>
				<div class="grid grid-cols-3 gap-2">
					<button
						type="button"
						onclick={() => selectUser('user1')}
						class="px-3 py-2 text-sm bg-gray-100 hover:bg-gray-200 rounded-lg transition-colors"
					>
						<div class="font-medium">User</div>
						<div class="text-xs text-gray-500">user1</div>
					</button>
					<button
						type="button"
						onclick={() => selectUser('supervisor1')}
						class="px-3 py-2 text-sm bg-blue-100 hover:bg-blue-200 rounded-lg transition-colors"
					>
						<div class="font-medium">Supervisor</div>
						<div class="text-xs text-gray-500">supervisor1</div>
					</button>
					<button
						type="button"
						onclick={() => selectUser('executive1')}
						class="px-3 py-2 text-sm bg-purple-100 hover:bg-purple-200 rounded-lg transition-colors"
					>
						<div class="font-medium">Executive</div>
						<div class="text-xs text-gray-500">executive1</div>
					</button>
				</div>
			</div>
		</div>

		<p class="text-center text-sm text-gray-500 mt-6">
			Powered by Flowable 7.0 + SvelteKit + Svelte 5
		</p>
	</div>
</div>
