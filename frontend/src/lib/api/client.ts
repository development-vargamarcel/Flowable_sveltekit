import type { User, Task, TaskDetails, ProcessDefinition, ProcessInstance, LoginRequest } from '$lib/types';

const API_BASE = import.meta.env.VITE_API_URL || 'http://localhost:8080';

async function fetchApi<T>(endpoint: string, options: RequestInit = {}): Promise<T> {
	const response = await fetch(`${API_BASE}${endpoint}`, {
		...options,
		credentials: 'include',
		headers: {
			'Content-Type': 'application/json',
			...options.headers
		}
	});

	if (!response.ok) {
		if (response.status === 401) {
			throw new Error('Unauthorized');
		}
		const error = await response.json().catch(() => ({ error: 'Request failed' }));
		throw new Error(error.error || 'Request failed');
	}

	return response.json();
}

export const api = {
	// Auth
	async login(credentials: LoginRequest): Promise<{ message: string; user: User }> {
		return fetchApi('/api/auth/login', {
			method: 'POST',
			body: JSON.stringify(credentials)
		});
	},

	async logout(): Promise<void> {
		await fetchApi('/api/auth/logout', { method: 'POST' });
	},

	async getCurrentUser(): Promise<User> {
		return fetchApi('/api/auth/me');
	},

	// Tasks
	async getTasks(): Promise<Task[]> {
		return fetchApi('/api/tasks');
	},

	async getAssignedTasks(): Promise<Task[]> {
		return fetchApi('/api/tasks/assigned');
	},

	async getClaimableTasks(): Promise<Task[]> {
		return fetchApi('/api/tasks/claimable');
	},

	async getTaskDetails(taskId: string): Promise<TaskDetails> {
		return fetchApi(`/api/tasks/${taskId}`);
	},

	async claimTask(taskId: string): Promise<void> {
		await fetchApi(`/api/tasks/${taskId}/claim`, { method: 'POST' });
	},

	async completeTask(taskId: string, variables: Record<string, unknown>): Promise<void> {
		await fetchApi(`/api/tasks/${taskId}/complete`, {
			method: 'POST',
			body: JSON.stringify({ variables })
		});
	},

	// Processes
	async getProcesses(): Promise<ProcessDefinition[]> {
		return fetchApi('/api/processes');
	},

	async startProcess(processKey: string, variables: Record<string, unknown>): Promise<{ message: string; processInstance: ProcessInstance }> {
		return fetchApi(`/api/processes/${processKey}/start`, {
			method: 'POST',
			body: JSON.stringify({ variables })
		});
	},

	async getProcessInstance(processInstanceId: string): Promise<ProcessInstance> {
		return fetchApi(`/api/processes/instance/${processInstanceId}`);
	},

	async getMyProcesses(): Promise<ProcessInstance[]> {
		return fetchApi('/api/processes/my-processes');
	},

	async getUsers(): Promise<User[]> {
		return fetchApi('/api/processes/users');
	}
};
