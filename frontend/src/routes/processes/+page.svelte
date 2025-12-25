<script lang="ts">
	import ProcessCard from '$lib/components/ProcessCard.svelte';
</script>

<svelte:head>
	<title>Start Process - BPM Demo</title>
</svelte:head>

<div class="max-w-4xl mx-auto px-4 py-8">
	<div class="mb-8">
		<h1 class="text-2xl font-bold text-gray-900">Start a New Process</h1>
		<p class="text-gray-600 mt-1">Select a workflow to begin</p>
	</div>

	<div class="grid gap-4">
		<ProcessCard
			title="Expense Request"
			description="Submit an expense for reimbursement. Amounts over $500 require executive approval."
			icon="$"
			color="green"
			href="/processes/expense"
		/>

		<ProcessCard
			title="Leave Request"
			description="Request time off from work. Leaves over 5 days require executive approval."
			icon="📅"
			color="blue"
			href="/processes/leave"
		/>

		<ProcessCard
			title="Create Task"
			description="Create and assign a task to yourself or team members."
			icon="📋"
			color="amber"
			href="/processes/task"
		/>
	</div>
</div>
