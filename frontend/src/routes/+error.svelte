<script lang="ts">
	import { page } from '$app/stores';
</script>

<div class="flex h-screen w-full flex-col items-center justify-center bg-gray-50 p-4">
	<div class="text-center">
		<h1 class="text-9xl font-black text-gray-200">{$page.status}</h1>
		<p class="text-2xl font-bold tracking-tight text-gray-900 sm:text-4xl">
			Uh-oh!
		</p>
		<p class="mt-4 text-gray-500">
			{$page.error?.message || 'Something went wrong.'}
		</p>
		<a
			href="/"
			class="mt-6 inline-block rounded-md bg-indigo-600 px-5 py-3 text-sm font-medium text-white hover:bg-indigo-700 focus:outline-none focus:ring"
		>
			Go Back Home
		</a>
	</div>
</div>
