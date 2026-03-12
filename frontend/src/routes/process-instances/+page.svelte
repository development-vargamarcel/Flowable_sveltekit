<script lang="ts">
	import { onMount } from 'svelte';
	import type { ProcessInstance } from '$lib/types';
	import { api } from '$lib/api/client';
	import { Button } from '$lib/components/ui/button';
	import { Card, CardContent, CardHeader, CardTitle } from '$lib/components/ui/card';
	import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '$lib/components/ui/table';
	import { toast } from 'svelte-sonner';
	import { formatDate } from '$lib/utils';
	import { getErrorMessage } from '$lib/utils/error-message';

	let instances = $state<ProcessInstance[]>([]);
	let isLoading = $state(false);
	let loadError = $state('');

	async function loadProcesses() {
		isLoading = true;
		loadError = '';
		try {
			const page = await api.getMyProcesses();
			instances = page.content;
		} catch (error: unknown) {
			loadError = getErrorMessage(error, 'Failed to fetch process instances.');
			toast.error(loadError);
			console.error(error);
		} finally {
			isLoading = false;
		}
	}

	onMount(loadProcesses);

	async function handleCancel(id: string) {
		if (!confirm('Are you sure you want to cancel this process instance?')) return;

		try {
			await api.cancelProcessInstance(id, 'User cancelled via My Processes');
			toast.success('Process instance cancelled.');
			await loadProcesses();
		} catch (error: unknown) {
			toast.error(`Failed to cancel process instance: ${getErrorMessage(error, 'Unknown error')}`);
			console.error(error);
		}
	}

	async function handleSuspend(id: string) {
		try {
			await api.suspendProcessInstance(id);
			toast.success('Process instance suspended.');
			await loadProcesses();
		} catch (error: unknown) {
			toast.error(`Failed to suspend process instance: ${getErrorMessage(error, 'Unknown error')}`);
			console.error(error);
		}
	}

	async function handleActivate(id: string) {
		try {
			await api.activateProcessInstance(id);
			toast.success('Process instance activated.');
			await loadProcesses();
		} catch (error: unknown) {
			toast.error(`Failed to activate process instance: ${getErrorMessage(error, 'Unknown error')}`);
			console.error(error);
		}
	}
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
			{#if isLoading}
				<div class="py-8 text-center text-sm text-muted-foreground">Loading process instances...</div>
			{:else if loadError}
				<div class="flex flex-col items-center gap-3 py-6 text-center">
					<p class="text-sm text-destructive">{loadError}</p>
					<Button variant="outline" size="sm" onclick={loadProcesses}>Retry</Button>
				</div>
			{:else if instances.length === 0}
				<div class="py-8 text-center text-sm text-muted-foreground">
					No process instances were found for your account.
				</div>
			{:else}
				<Table>
					<TableHeader>
						<TableRow>
							<TableHead>ID</TableHead>
							<TableHead>Process Definition ID</TableHead>
							<TableHead>Start Time</TableHead>
							<TableHead>Status</TableHead>
							<TableHead>Actions</TableHead>
						</TableRow>
					</TableHeader>
					<TableBody>
						{#each instances as instance}
							<TableRow>
								<TableCell>{instance.id}</TableCell>
								<TableCell>{instance.processDefinitionId}</TableCell>
								<TableCell>{formatDate(instance.startTime)}</TableCell>
								<TableCell>
									{#if instance.ended}
										Ended
									{:else if instance.suspended}
										Suspended
									{:else}
										Active
									{/if}
								</TableCell>
								<TableCell>
									{#if !instance.ended}
										{#if instance.suspended}
											<Button variant="outline" size="sm" onclick={() => handleActivate(instance.id)} class="mr-2">
												Activate
											</Button>
										{:else}
											<Button variant="outline" size="sm" onclick={() => handleSuspend(instance.id)} class="mr-2">
												Suspend
											</Button>
										{/if}
										<Button variant="destructive" size="sm" onclick={() => handleCancel(instance.id)}>
											Cancel
										</Button>
									{/if}
								</TableCell>
							</TableRow>
						{/each}
					</TableBody>
				</Table>
			{/if}
		</CardContent>
	</Card>
</div>
