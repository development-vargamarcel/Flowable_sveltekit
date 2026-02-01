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
  async getTasks(filters?: {
    text?: string;
    assignee?: string;
    priority?: number;
  }): Promise<Task[]> {
    const params = new URLSearchParams();
    if (filters?.text) params.append('text', filters.text);
    if (filters?.assignee) params.append('assignee', filters.assignee);
    if (filters?.priority) params.append('priority', filters.priority.toString());

    const queryString = params.toString() ? `?${params.toString()}` : '';
    const result = await fetchApi<{ content: Task[] } | Task[]>(`/api/tasks${queryString}`);
    if (Array.isArray(result)) return result;
    return (result as any).content || [];
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

  async unclaimTask(taskId: string): Promise<void> {
    await fetchApi(`/api/tasks/${taskId}/unclaim`, { method: 'POST' });
  },

  async delegateTask(taskId: string, targetUserId: string): Promise<void> {
    await fetchApi(`/api/tasks/${taskId}/delegate`, {
      method: 'POST',
      body: JSON.stringify({ targetUserId })
    });
  },

  async completeTask(taskId: string, variables: Record<string, unknown>): Promise<void> {
    await fetchApi(`/api/tasks/${taskId}/complete`, {
      method: 'POST',
      body: JSON.stringify({ variables })
    });
  },

  async updateTask(taskId: string, data: Partial<Task>): Promise<Task> {
    return fetchApi(`/api/tasks/${taskId}`, {
      method: 'PUT',
      body: JSON.stringify(data)
    });
  },

  async getTaskComments(taskId: string): Promise<Comment[]> {
    return fetchApi(`/api/tasks/${taskId}/comments`);
  },

  async addTaskComment(taskId: string, message: string): Promise<Comment> {
    return fetchApi(`/api/tasks/${taskId}/comments`, {
      method: 'POST',
      body: JSON.stringify({ message })
    });
  },

  /**
   * Fetch documents associated with a task.
   */
  async getTaskDocuments(taskId: string): Promise<DocumentDTO[]> {
    return fetchApi(`/api/tasks/${taskId}/documents`);
  },

  /**
   * Upload a document to a task (Mock implementation).
   */
  async uploadTaskDocument(taskId: string, file: File): Promise<DocumentDTO> {
    // Mock payload for the mock server
    return fetchApi(`/api/tasks/${taskId}/documents`, {
      method: 'POST',
      body: JSON.stringify({ name: file.name, type: file.type, size: file.size })
    });
  },

  /**
   * Delete a document from a task.
   */
  async deleteTaskDocument(taskId: string, documentId: string): Promise<void> {
    await fetchApi(`/api/tasks/${taskId}/documents/${documentId}`, {
      method: 'DELETE'
    });
  },

  /**
   * Fetch the audit log/history for a specific task.
   */
  async getTaskHistory(taskId: string): Promise<TaskHistoryEvent[]> {
    return fetchApi(`/api/tasks/${taskId}/history`);
  },

  // Escalation
  async getEscalationOptions(taskId: string): Promise<EscalationOptions> {
    return fetchApi(`/api/workflow/tasks/${taskId}/escalation-options`);
  },

  async escalateTask(
    taskId: string,
    request: EscalationRequest
  ): Promise<{ message: string; escalation: Escalation }> {
    return fetchApi(`/api/workflow/tasks/${taskId}/escalate`, {
      method: 'POST',
      body: JSON.stringify(request)
    });
  },

  async deEscalateTask(
    taskId: string,
    request: EscalationRequest
  ): Promise<{ message: string; deEscalation: Escalation }> {
    return fetchApi(`/api/workflow/tasks/${taskId}/de-escalate`, {
      method: 'POST',
      body: JSON.stringify(request)
    });
  },

  // Handoff
  async handoffTask(taskId: string, request: HandoffRequest): Promise<{ message: string }> {
    return fetchApi(`/api/workflow/tasks/${taskId}/handoff`, {
      method: 'POST',
      body: JSON.stringify(request)
    });
  },

  // Approvals
  async approveTask(
    taskId: string,
    comments?: string
  ): Promise<{ message: string; approval: Approval }> {
    return fetchApi(`/api/workflow/tasks/${taskId}/approve`, {
      method: 'POST',
      body: JSON.stringify({ comments })
    });
  },

  async rejectTask(
    taskId: string,
    comments: string
  ): Promise<{ message: string; approval: Approval }> {
    return fetchApi(`/api/workflow/tasks/${taskId}/reject`, {
      method: 'POST',
      body: JSON.stringify({ comments })
    });
  },

  async requestChanges(
    taskId: string,
    comments: string
  ): Promise<{ message: string; approval: Approval }> {
    return fetchApi(`/api/workflow/tasks/${taskId}/request-changes`, {
      method: 'POST',
      body: JSON.stringify({ comments })
    });
  },

  // Task Forms
  async getTaskFormDefinition(taskId: string): Promise<TaskFormWithConfig> {
    return fetchApi(`/api/tasks/${taskId}/form`);
  }
};
