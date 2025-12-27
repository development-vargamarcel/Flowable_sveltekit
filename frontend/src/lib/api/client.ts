import type { User, Task, TaskDetails, ProcessDefinition, ProcessInstance, LoginRequest } from '$lib/types';

// In production, use relative URLs (empty string) so nginx can proxy /api/* to backend
// In development, use localhost:8080 for direct backend access
const API_BASE = import.meta.env.VITE_API_URL ?? (import.meta.env.DEV ? 'http://localhost:8080' : '');

/**
 * Custom API error with detailed information
 */
export class ApiError extends Error {
	public readonly status: number;
	public readonly statusText: string;
	public readonly details?: string;
	public readonly fieldErrors?: Record<string, string>;
	public readonly timestamp?: string;

	constructor(
		message: string,
		status: number,
		statusText: string,
		details?: string,
		fieldErrors?: Record<string, string>,
		timestamp?: string
	) {
		super(message);
		this.name = 'ApiError';
		this.status = status;
		this.statusText = statusText;
		this.details = details;
		this.fieldErrors = fieldErrors;
		this.timestamp = timestamp;
	}

	/**
	 * Get a user-friendly error message with details
	 */
	getFullMessage(): string {
		if (this.details && this.details !== this.message) {
			return `${this.message}: ${this.details}`;
		}
		return this.message;
	}

	/**
	 * Check if this is a validation error
	 */
	isValidationError(): boolean {
		return this.status === 400 && !!this.fieldErrors && Object.keys(this.fieldErrors).length > 0;
	}
}

/**
 * Parse error response body and extract meaningful error information
 */
function parseErrorResponse(
	status: number,
	statusText: string,
	errorBody: Record<string, unknown> | null
): ApiError {
	// Default error messages by status code
	const defaultMessages: Record<number, string> = {
		400: 'Bad request',
		401: 'Invalid credentials',
		403: 'Access forbidden',
		404: 'Resource not found',
		405: 'Method not allowed',
		415: 'Unsupported media type',
		422: 'Validation error',
		500: 'Internal server error',
		502: 'Backend unavailable',
		503: 'Service unavailable',
		504: 'Gateway timeout'
	};

	if (!errorBody) {
		return new ApiError(
			defaultMessages[status] || `Request failed (${status})`,
			status,
			statusText
		);
	}

	// Extract error information from various response formats
	const error = errorBody.error as string | undefined;
	const message = errorBody.message as string | undefined;
	const details = errorBody.details as string | undefined;
	const fieldErrors = errorBody.fieldErrors as Record<string, string> | undefined;
	const timestamp = errorBody.timestamp as string | undefined;

	// Build the error message
	let errorMessage: string;
	if (error && message && error !== message) {
		// Backend uses separate error and message fields
		errorMessage = error;
	} else if (error) {
		errorMessage = error;
	} else if (message) {
		errorMessage = message;
	} else {
		errorMessage = defaultMessages[status] || `Request failed (${status})`;
	}

	// Use message as details if details not provided
	const errorDetails = details || (error && message && error !== message ? message : undefined);

	return new ApiError(errorMessage, status, statusText, errorDetails, fieldErrors, timestamp);
}

async function fetchApi<T>(endpoint: string, options: RequestInit = {}): Promise<T> {
	const url = `${API_BASE}${endpoint}`;

	try {
		const response = await fetch(url, {
			...options,
			credentials: 'include',
			headers: {
				'Content-Type': 'application/json',
				...options.headers
			}
		});

		if (!response.ok) {
			// Try to parse error response body
			let errorBody: Record<string, unknown> | null = null;
			try {
				const text = await response.text();
				if (text) {
					errorBody = JSON.parse(text);
				}
			} catch {
				// Response body is not valid JSON, that's ok
			}

			throw parseErrorResponse(response.status, response.statusText, errorBody);
		}

		// Handle empty responses (204 No Content)
		const contentLength = response.headers.get('content-length');
		if (response.status === 204 || contentLength === '0') {
			return {} as T;
		}

		return response.json();
	} catch (error) {
		// Re-throw ApiErrors as-is
		if (error instanceof ApiError) {
			throw error;
		}

		// Handle network errors
		if (error instanceof TypeError) {
			throw new ApiError(
				'Network error',
				0,
				'Network Error',
				'Unable to connect to the server. Please check your internet connection and try again.'
			);
		}

		// Handle other errors
		throw new ApiError(
			error instanceof Error ? error.message : 'Unknown error',
			0,
			'Unknown Error',
			'An unexpected error occurred'
		);
	}
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
