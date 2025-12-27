import type { User, Task, TaskDetails, ProcessDefinition, ProcessInstance, LoginRequest } from '$lib/types';

// In production, use relative URLs (empty string) so nginx can proxy /api/* to backend
// In development, use localhost:8080 for direct backend access
const API_BASE = import.meta.env.VITE_API_URL ?? (import.meta.env.DEV ? 'http://localhost:8080' : '');

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
		// Try to get a more specific error message from the response body
		const errorBody = await response.json().catch(() => null);

		if (response.status === 401) {
			const errorMessage = errorBody?.error || errorBody?.message || 'Invalid credentials';
			throw new Error(errorMessage);
		}

		if (response.status === 403) {
			// 403 can be CORS issue or authorization issue
			const errorMessage = errorBody?.error || errorBody?.message ||
				'Access forbidden - this may be a CORS or authorization issue. Please check the server logs.';
			throw new Error(errorMessage);
		}

		if (response.status === 502 || response.status === 503 || response.status === 504) {
			const errorMessage = errorBody?.error || errorBody?.message ||
				`Backend service unavailable (${response.status}). Please try again later.`;
			throw new Error(errorMessage);
		}

		// Handle different error response formats: { error: "..." }, { message: "..." }, or validation errors
		const errorMessage = errorBody?.error || errorBody?.message || `Request failed (${response.status})`;
		throw new Error(errorMessage);
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
