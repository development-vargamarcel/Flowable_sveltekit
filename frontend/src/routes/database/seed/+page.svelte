<svelte:head>
	<title>Seed Database</title>
</svelte:head>

<div class="p-8">
	<h1 class="text-2xl font-bold">Seed Database</h1>
	<p class="mt-4">Click the button below to seed the database with initial data.</p>
	<button
		class="mt-4 px-4 py-2 bg-blue-600 text-white rounded-md"
		on:click={async () => {
			const response = await fetch('/api/database/seed', {
				method: 'POST'
			});
			if (response.ok) {
				alert('Database seeded successfully!');
			} else {
				alert('Failed to seed database.');
			}
		}}
	>
		Seed Database
	</button>
</div>
