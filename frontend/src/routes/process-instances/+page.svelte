<script lang="ts">
	import { onMount } from 'svelte';
	import type { ProcessInstance } from '$lib/types';
	import { api } from '$lib/api/client';
	import { Button } from '$lib/components/ui/button';
	import { Card, CardContent, CardHeader, CardTitle } from '$lib/components/ui/card';
	import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '$lib/components/ui/table';
	import { toast } from 'svelte-sonner';

	let instances = $state<ProcessInstance[]>([]);

	onMount(async () => {
		try {
			const page = await api.getMyProcesses();
			instances = page.content;
		} catch (error) {
			toast.error('Failed to fetch process instances.');
			console.error(error);
		}
	});
</script>

<svelte:head>
	<title>Process Instances - BPM Demo</title>
</svelte:head>

<div class="p-4">
	<Card>
		<CardHeader>
			<CardTitle>Process Instances</CardTitle>
		</CardHeader>
		<CardContent>
			<Table>
				<TableHeader>
					<TableRow>
						<TableHead>ID</TableHead>
						<TableHead>Process Definition ID</TableHead>
						<TableHead>Start Time</TableHead>
						<TableHead>Status</TableHead>
					</TableRow>
				</TableHeader>
				<TableBody>
					{#each instances as instance}
						<TableRow>
							<TableCell>{instance.id}</TableCell>
							<TableCell>{instance.processDefinitionId}</TableCell>
							<TableCell>{new Date(instance.startTime).toLocaleString()}</TableCell>
							<TableCell>{instance.ended ? 'Ended' : 'Active'}</TableCell>
						</TableRow>
					{/each}
				</TableBody>
			</Table>
		</CardContent>
	</Card>
</div>
