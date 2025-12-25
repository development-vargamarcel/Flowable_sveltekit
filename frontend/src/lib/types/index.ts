export interface User {
	username: string;
	displayName: string;
	email: string | null;
	roles: string[];
}

export interface Task {
	id: string;
	name: string;
	description: string | null;
	processInstanceId: string;
	processDefinitionKey: string;
	processName: string;
	assignee: string | null;
	owner: string | null;
	createTime: string;
	dueDate: string | null;
	priority: number;
	formKey: string | null;
	variables: Record<string, unknown>;
	businessKey: string | null;
}

export interface TaskDetails {
	task: Task;
	variables: Record<string, unknown>;
}

export interface ProcessDefinition {
	key: string;
	name: string;
	description: string | null;
	version: number;
}

export interface ProcessInstance {
	id: string;
	processDefinitionKey: string;
	processDefinitionName: string | null;
	businessKey: string;
	startTime: string;
	startUserId: string | null;
	variables: Record<string, unknown>;
	ended: boolean;
}

export interface LoginRequest {
	username: string;
	password: string;
}

export interface ApiResponse<T> {
	data?: T;
	error?: string;
	message?: string;
}
