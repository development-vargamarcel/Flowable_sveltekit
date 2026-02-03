/* eslint-disable no-console */
import { fetchApi } from './core';
import type {
  Task,
  TaskDetails,
  TaskFormWithConfig,
  EscalationOptions,
  EscalationRequest,
  Escalation,
  HandoffRequest,
  Approval,
  Comment,
  DocumentDTO,
  TaskHistoryEvent
} from '$lib/types';

export const tasksApi = {
  /**
   * Fetch a list of tasks with optional filtering.
   * @param filters - Optional filters for text, assignee, and priority.
   * @returns A promise that resolves to an array of tasks.
   */
  async getTasks(filters?: {
    text?: string;
    assignee?: string;
    priority?: number;
  }): Promise<Task[]> {
    console.log('[tasksApi] getTasks called with:', filters);
    const params = new URLSearchParams();
    if (filters?.text) params.append('text', filters.text);
    if (filters?.assignee) params.append('assignee', filters.assignee);
    if (filters?.priority) params.append('priority', filters.priority.toString());

    const queryString = params.toString() ? `?${params.toString()}` : '';
    const result = await fetchApi<{ content: Task[] } | Task[]>(`/api/tasks${queryString}`);
    if (Array.isArray(result)) return result;
    return (result as any).content || [];
  },

  /**
   * Fetch tasks assigned to the current user.
   * @returns A promise that resolves to an array of assigned tasks.
   */
  async getAssignedTasks(): Promise<Task[]> {
    console.log('[tasksApi] getAssignedTasks called');
    return fetchApi('/api/tasks/assigned');
  },

  /**
   * Fetch tasks that are available to be claimed by the current user.
   * @returns A promise that resolves to an array of claimable tasks.
   */
  async getClaimableTasks(): Promise<Task[]> {
    console.log('[tasksApi] getClaimableTasks called');
    return fetchApi('/api/tasks/claimable');
  },

  /**
   * Fetch detailed information for a specific task.
   * @param taskId - The ID of the task to fetch.
   * @returns A promise that resolves to the task details.
   */
  async getTaskDetails(taskId: string): Promise<TaskDetails> {
    console.log('[tasksApi] getTaskDetails called with taskId:', taskId);
    return fetchApi(`/api/tasks/${taskId}`);
  },

  /**
   * Claim a task for the current user.
   * @param taskId - The ID of the task to claim.
   */
  async claimTask(taskId: string): Promise<void> {
    console.log('[tasksApi] claimTask called with taskId:', taskId);
    await fetchApi(`/api/tasks/${taskId}/claim`, { method: 'POST' });
  },

  /**
   * Unclaim a task, making it available for others.
   * @param taskId - The ID of the task to unclaim.
   */
  async unclaimTask(taskId: string): Promise<void> {
    console.log('[tasksApi] unclaimTask called with taskId:', taskId);
    await fetchApi(`/api/tasks/${taskId}/unclaim`, { method: 'POST' });
  },

  /**
   * Delegate a task to another user.
   * @param taskId - The ID of the task to delegate.
   * @param targetUserId - The ID of the user to delegate the task to.
   */
  async delegateTask(taskId: string, targetUserId: string): Promise<void> {
    console.log(
      '[tasksApi] delegateTask called with taskId:',
      taskId,
      'targetUserId:',
      targetUserId
    );
    await fetchApi(`/api/tasks/${taskId}/delegate`, {
      method: 'POST',
      body: JSON.stringify({ targetUserId })
    });
  },

  /**
   * Complete a task with the provided variables.
   * @param taskId - The ID of the task to complete.
   * @param variables - Key-value pairs of data to submit with the task completion.
   */
  async completeTask(taskId: string, variables: Record<string, unknown>): Promise<void> {
    console.log('[tasksApi] completeTask called with taskId:', taskId, 'variables:', variables);
    await fetchApi(`/api/tasks/${taskId}/complete`, {
      method: 'POST',
      body: JSON.stringify({ variables })
    });
  },

  /**
   * Update task properties.
   * @param taskId - The ID of the task to update.
   * @param data - Partial task object containing fields to update.
   * @returns A promise that resolves to the updated task.
   */
  async updateTask(taskId: string, data: Partial<Task>): Promise<Task> {
    console.log('[tasksApi] updateTask called with taskId:', taskId, 'data:', data);
    return fetchApi(`/api/tasks/${taskId}`, {
      method: 'PUT',
      body: JSON.stringify(data)
    });
  },

  /**
   * Fetch comments for a specific task.
   * @param taskId - The ID of the task.
   * @returns A promise that resolves to an array of comments.
   */
  async getTaskComments(taskId: string): Promise<Comment[]> {
    console.log('[tasksApi] getTaskComments called with taskId:', taskId);
    return fetchApi(`/api/tasks/${taskId}/comments`);
  },

  /**
   * Add a comment to a task.
   * @param taskId - The ID of the task.
   * @param message - The comment message.
   * @returns A promise that resolves to the created comment.
   */
  async addTaskComment(taskId: string, message: string): Promise<Comment> {
    console.log('[tasksApi] addTaskComment called with taskId:', taskId, 'message:', message);
    return fetchApi(`/api/tasks/${taskId}/comments`, {
      method: 'POST',
      body: JSON.stringify({ message })
    });
  },

  /**
   * Fetch documents associated with a task.
   * @param taskId - The ID of the task.
   * @returns A promise that resolves to an array of document DTOs.
   */
  async getTaskDocuments(taskId: string): Promise<DocumentDTO[]> {
    console.log('[tasksApi] getTaskDocuments called with taskId:', taskId);
    return fetchApi(`/api/tasks/${taskId}/documents`);
  },

  /**
   * Upload a document to a task (Mock implementation).
   * @param taskId - The ID of the task.
   * @param file - The file to upload.
   * @returns A promise that resolves to the uploaded document DTO.
   */
  async uploadTaskDocument(taskId: string, file: File): Promise<DocumentDTO> {
    console.log(
      '[tasksApi] uploadTaskDocument called with taskId:',
      taskId,
      'fileName:',
      file.name
    );
    // Mock payload for the mock server
    return fetchApi(`/api/tasks/${taskId}/documents`, {
      method: 'POST',
      body: JSON.stringify({ name: file.name, type: file.type, size: file.size })
    });
  },

  /**
   * Delete a document from a task.
   * @param taskId - The ID of the task.
   * @param documentId - The ID of the document to delete.
   */
  async deleteTaskDocument(taskId: string, documentId: string): Promise<void> {
    console.log(
      '[tasksApi] deleteTaskDocument called with taskId:',
      taskId,
      'documentId:',
      documentId
    );
    await fetchApi(`/api/tasks/${taskId}/documents/${documentId}`, {
      method: 'DELETE'
    });
  },

  /**
   * Fetch the audit log/history for a specific task.
   * @param taskId - The ID of the task.
   * @returns A promise that resolves to an array of history events.
   */
  async getTaskHistory(taskId: string): Promise<TaskHistoryEvent[]> {
    console.log('[tasksApi] getTaskHistory called with taskId:', taskId);
    return fetchApi(`/api/tasks/${taskId}/history`);
  },

  // Escalation
  /**
   * Get available escalation options for a task.
   * @param taskId - The ID of the task.
   * @returns A promise that resolves to escalation options.
   */
  async getEscalationOptions(taskId: string): Promise<EscalationOptions> {
    console.log('[tasksApi] getEscalationOptions called with taskId:', taskId);
    return fetchApi(`/api/workflow/tasks/${taskId}/escalation-options`);
  },

  /**
   * Escalate a task.
   * @param taskId - The ID of the task.
   * @param request - Escalation request details.
   * @returns A promise that resolves to the escalation result.
   */
  async escalateTask(
    taskId: string,
    request: EscalationRequest
  ): Promise<{ message: string; escalation: Escalation }> {
    console.log('[tasksApi] escalateTask called with taskId:', taskId, 'request:', request);
    return fetchApi(`/api/workflow/tasks/${taskId}/escalate`, {
      method: 'POST',
      body: JSON.stringify(request)
    });
  },

  /**
   * De-escalate a task.
   * @param taskId - The ID of the task.
   * @param request - De-escalation request details.
   * @returns A promise that resolves to the de-escalation result.
   */
  async deEscalateTask(
    taskId: string,
    request: EscalationRequest
  ): Promise<{ message: string; deEscalation: Escalation }> {
    console.log('[tasksApi] deEscalateTask called with taskId:', taskId, 'request:', request);
    return fetchApi(`/api/workflow/tasks/${taskId}/de-escalate`, {
      method: 'POST',
      body: JSON.stringify(request)
    });
  },

  // Handoff
  /**
   * Handoff a task to another user.
   * @param taskId - The ID of the task.
   * @param request - Handoff request details.
   * @returns A promise that resolves to a message.
   */
  async handoffTask(taskId: string, request: HandoffRequest): Promise<{ message: string }> {
    console.log('[tasksApi] handoffTask called with taskId:', taskId, 'request:', request);
    return fetchApi(`/api/workflow/tasks/${taskId}/handoff`, {
      method: 'POST',
      body: JSON.stringify(request)
    });
  },

  // Approvals
  /**
   * Approve a task.
   * @param taskId - The ID of the task.
   * @param comments - Optional comments.
   * @returns A promise that resolves to the approval result.
   */
  async approveTask(
    taskId: string,
    comments?: string
  ): Promise<{ message: string; approval: Approval }> {
    console.log('[tasksApi] approveTask called with taskId:', taskId);
    return fetchApi(`/api/workflow/tasks/${taskId}/approve`, {
      method: 'POST',
      body: JSON.stringify({ comments })
    });
  },

  /**
   * Reject a task.
   * @param taskId - The ID of the task.
   * @param comments - Reason for rejection.
   * @returns A promise that resolves to the rejection result.
   */
  async rejectTask(
    taskId: string,
    comments: string
  ): Promise<{ message: string; approval: Approval }> {
    console.log('[tasksApi] rejectTask called with taskId:', taskId);
    return fetchApi(`/api/workflow/tasks/${taskId}/reject`, {
      method: 'POST',
      body: JSON.stringify({ comments })
    });
  },

  /**
   * Request changes for a task.
   * @param taskId - The ID of the task.
   * @param comments - Reason for requesting changes.
   * @returns A promise that resolves to the request result.
   */
  async requestChanges(
    taskId: string,
    comments: string
  ): Promise<{ message: string; approval: Approval }> {
    console.log('[tasksApi] requestChanges called with taskId:', taskId);
    return fetchApi(`/api/workflow/tasks/${taskId}/request-changes`, {
      method: 'POST',
      body: JSON.stringify({ comments })
    });
  },

  // Task Forms
  /**
   * Get the form definition for a task.
   * @param taskId - The ID of the task.
   * @returns A promise that resolves to the task form configuration.
   */
  async getTaskFormDefinition(taskId: string): Promise<TaskFormWithConfig> {
    console.log('[tasksApi] getTaskFormDefinition called with taskId:', taskId);
    return fetchApi(`/api/tasks/${taskId}/form`);
  }
};
