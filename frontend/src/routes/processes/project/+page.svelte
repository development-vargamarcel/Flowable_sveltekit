<script lang="ts">
	import { goto } from '$app/navigation';
	import { api } from '$lib/api/client';
	import Toast from '$lib/components/Toast.svelte';

	let projectName = $state('');
	let budget = $state<number | null>(null);
	let timeline = $state('');
	let department = $state('');
	let projectType = $state('standard');
	let expectedROI = $state('');
	let description = $state('');

	let loading = $state(false);
	let toast = $state<{ message: string; type: 'success' | 'error' } | null>(null);
	let errors = $state<Record<string, string>>({});

	const departments = [
		'Engineering',
		'Marketing',
		'Sales',
		'Operations',
		'Finance',
		'HR',
		'Legal',
		'IT',
		'Product'
	];

	const projectTypes = [
		{ value: 'standard', label: 'Standard', description: 'Regular business project' },
		{ value: 'strategic', label: 'Strategic', description: 'Company-wide strategic initiative' },
		{ value: 'compliance', label: 'Compliance', description: 'Regulatory or compliance requirement' },
		{ value: 'legal', label: 'Legal', description: 'Legal matter or contract' },
		{ value: 'enterprise', label: 'Enterprise', description: 'Large-scale enterprise system' }
	];

	const timelines = [
		'1 month',
		'2 months',
		'3 months',
		'6 months',
		'9 months',
		'12 months',
		'18 months',
		'24 months'
	];

	function validate(): boolean {
		const newErrors: Record<string, string> = {};

		if (!projectName || projectName.length < 5) {
			newErrors.projectName = 'Project name must be at least 5 characters';
		}
		if (!budget || budget <= 0) {
			newErrors.budget = 'Budget must be greater than 0';
		}
		if (!timeline) {
			newErrors.timeline = 'Timeline is required';
		}
		if (!department) {
			newErrors.department = 'Department is required';
		}
		if (!description || description.length < 20) {
			newErrors.description = 'Description must be at least 20 characters';
		}
		if (!expectedROI || expectedROI.length < 10) {
			newErrors.expectedROI = 'Expected ROI must be at least 10 characters';
		}

		errors = newErrors;
		return Object.keys(newErrors).length === 0;
	}

	function getApprovalInfo(bgt: number, type: string): { reviews: string[]; executive: boolean } {
		const reviews = ['Technical Review', 'Financial Review'];
		let executive = false;

		if (bgt > 25000 || type === 'legal' || type === 'compliance') {
			reviews.push('Legal Review');
		}

		if (bgt > 100000) {
			executive = true;
		}

		return { reviews, executive };
	}

	async function handleSubmit() {
		if (!validate()) return;

		loading = true;
		try {
			await api.startProcess('project-approval', {
				projectName,
				budget,
				timeline,
				department,
				projectType,
				expectedROI,
				description
			});

			toast = { message: 'Project proposal submitted successfully!', type: 'success' };
			setTimeout(() => goto('/dashboard'), 2000);
		} catch (err) {
			toast = {
				message: err instanceof Error ? err.message : 'Failed to submit project proposal',
				type: 'error'
			};
		} finally {
			loading = false;
		}
	}

	let approvalInfo = $derived(budget && budget > 0 ? getApprovalInfo(budget, projectType) : null);
</script>

<svelte:head>
	<title>Submit Project Proposal - BPM Demo</title>
</svelte:head>

{#if toast}
	<Toast message={toast.message} type={toast.type} onClose={() => (toast = null)} />
{/if}

<div class="max-w-2xl mx-auto px-4 py-8">
	<div class="mb-8">
		<a href="/processes" class="text-sm text-blue-600 hover:text-blue-700">← Back to Processes</a>
		<h1 class="text-2xl font-bold text-gray-900 mt-2">Submit Project Proposal</h1>
		<p class="text-gray-600 mt-1">
			Request multi-stakeholder approval for new projects with parallel reviews
		</p>
	</div>

	<form onsubmit={(e) => { e.preventDefault(); handleSubmit(); }} class="space-y-6">
		<div class="card">
			<h2 class="text-lg font-semibold text-gray-800 mb-4">Project Information</h2>

			<div class="grid gap-6">
				<!-- Project Name -->
				<div>
					<label for="projectName" class="block text-sm font-medium text-gray-700">
						Project Name *
					</label>
					<input
						type="text"
						id="projectName"
						bind:value={projectName}
						class="mt-1 block w-full px-3 py-2 border border-gray-300 rounded-lg shadow-sm focus:ring-blue-500 focus:border-blue-500"
						placeholder="Enter project name"
					/>
					{#if errors.projectName}
						<p class="mt-1 text-sm text-red-600">{errors.projectName}</p>
					{/if}
				</div>

				<div class="grid grid-cols-2 gap-4">
					<!-- Budget -->
					<div>
						<label for="budget" class="block text-sm font-medium text-gray-700">
							Budget ($) *
						</label>
						<input
							type="number"
							id="budget"
							bind:value={budget}
							min="0"
							step="1000"
							class="mt-1 block w-full px-3 py-2 border border-gray-300 rounded-lg shadow-sm focus:ring-blue-500 focus:border-blue-500"
							placeholder="0"
						/>
						{#if errors.budget}
							<p class="mt-1 text-sm text-red-600">{errors.budget}</p>
						{/if}
					</div>

					<!-- Timeline -->
					<div>
						<label for="timeline" class="block text-sm font-medium text-gray-700">
							Timeline *
						</label>
						<select
							id="timeline"
							bind:value={timeline}
							class="mt-1 block w-full px-3 py-2 border border-gray-300 rounded-lg shadow-sm focus:ring-blue-500 focus:border-blue-500"
						>
							<option value="">Select timeline...</option>
							{#each timelines as t}
								<option value={t}>{t}</option>
							{/each}
						</select>
						{#if errors.timeline}
							<p class="mt-1 text-sm text-red-600">{errors.timeline}</p>
						{/if}
					</div>
				</div>

				<!-- Department -->
				<div>
					<label for="department" class="block text-sm font-medium text-gray-700">
						Department *
					</label>
					<select
						id="department"
						bind:value={department}
						class="mt-1 block w-full px-3 py-2 border border-gray-300 rounded-lg shadow-sm focus:ring-blue-500 focus:border-blue-500"
					>
						<option value="">Select department...</option>
						{#each departments as dept}
							<option value={dept}>{dept}</option>
						{/each}
					</select>
					{#if errors.department}
						<p class="mt-1 text-sm text-red-600">{errors.department}</p>
					{/if}
				</div>

				<!-- Project Type -->
				<div>
					<label class="block text-sm font-medium text-gray-700 mb-2">Project Type *</label>
					<div class="grid grid-cols-1 gap-2">
						{#each projectTypes as type}
							<label
								class="flex items-start p-3 border rounded-lg cursor-pointer transition-colors
									{projectType === type.value ? 'border-blue-500 bg-blue-50' : 'border-gray-200 hover:bg-gray-50'}"
							>
								<input
									type="radio"
									bind:group={projectType}
									value={type.value}
									class="mt-0.5 mr-3"
								/>
								<div>
									<span class="font-medium text-gray-900">{type.label}</span>
									<p class="text-sm text-gray-500">{type.description}</p>
								</div>
							</label>
						{/each}
					</div>
				</div>

				<!-- Expected ROI -->
				<div>
					<label for="expectedROI" class="block text-sm font-medium text-gray-700">
						Expected ROI / Benefits *
					</label>
					<textarea
						id="expectedROI"
						bind:value={expectedROI}
						rows="2"
						class="mt-1 block w-full px-3 py-2 border border-gray-300 rounded-lg shadow-sm focus:ring-blue-500 focus:border-blue-500 resize-none"
						placeholder="Expected return on investment or key benefits..."
					></textarea>
					{#if errors.expectedROI}
						<p class="mt-1 text-sm text-red-600">{errors.expectedROI}</p>
					{/if}
				</div>

				<!-- Description -->
				<div>
					<label for="description" class="block text-sm font-medium text-gray-700">
						Project Description *
					</label>
					<textarea
						id="description"
						bind:value={description}
						rows="4"
						class="mt-1 block w-full px-3 py-2 border border-gray-300 rounded-lg shadow-sm focus:ring-blue-500 focus:border-blue-500 resize-none"
						placeholder="Detailed project description, objectives, and scope..."
					></textarea>
					{#if errors.description}
						<p class="mt-1 text-sm text-red-600">{errors.description}</p>
					{/if}
				</div>
			</div>
		</div>

		<!-- Approval Path Preview -->
		{#if approvalInfo}
			<div class="bg-purple-50 border border-purple-200 rounded-lg p-4">
				<h3 class="font-semibold text-purple-800 mb-3">Required Approvals</h3>
				<div class="flex flex-wrap gap-2 mb-3">
					{#each approvalInfo.reviews as review}
						<span class="px-3 py-1 bg-purple-100 text-purple-800 rounded-full text-sm">
							{review}
						</span>
					{/each}
					{#if approvalInfo.executive}
						<span class="px-3 py-1 bg-purple-600 text-white rounded-full text-sm font-medium">
							+ Executive Final Approval
						</span>
					{/if}
				</div>
				<p class="text-sm text-purple-700">
					These reviews will run in parallel. Final completion requires all stakeholders to approve.
				</p>
			</div>
		{/if}

		<!-- Info Box -->
		<div class="bg-blue-50 border border-blue-200 rounded-lg p-4">
			<h3 class="font-semibold text-blue-800 mb-2">Multi-Stakeholder Approval Process</h3>
			<div class="text-sm text-blue-700 space-y-1">
				<p><strong>Parallel Reviews:</strong> Technical and Financial reviews happen simultaneously</p>
				<p><strong>Legal Review:</strong> Required for budgets over $25,000 or legal/compliance projects</p>
				<p><strong>Executive Approval:</strong> Required for budgets over $100,000</p>
				<p><strong>Escalation:</strong> Any reviewer can escalate to their manager if needed</p>
			</div>
		</div>

		<button
			type="submit"
			disabled={loading}
			class="w-full flex justify-center py-3 px-4 border border-transparent rounded-lg shadow-sm text-sm font-medium text-white bg-blue-600 hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500 disabled:opacity-50"
		>
			{loading ? 'Submitting...' : 'Submit Project Proposal'}
		</button>
	</form>
</div>
