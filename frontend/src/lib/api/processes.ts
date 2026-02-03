/* eslint-disable no-console */
import { fetchApi } from './core';
import type {
  ProcessDefinition,
  ProcessInstance,
  Page,
  User,
  WorkflowHistory,
  Comment,
  FormDefinition
} from '$lib/types';

export const processesApi = {
  /**
   * Fetch all active process definitions.
   * @returns A promise that resolves to an array of process definitions.
   */
  async getProcesses(): Promise<ProcessDefinition[]> {
    console.log('[processesApi] getProcesses called');
    return fetchApi('/api/processes');
  },

  /**
   * Fetch all process definitions, including suspended ones.
   * @returns A promise that resolves to an array of all process definitions.
   */
  async getAllProcessDefinitions(): Promise<ProcessDefinition[]> {
    console.log('[processesApi] getAllProcessDefinitions called');
    return fetchApi('/api/processes/definitions');
  },

  /**
   * Start a new process instance.
   * @param processKey - The key of the process definition.
   * @param variables - Initial variables for the process instance.
   * @param businessKey - Optional business key for the process instance.
   * @returns A promise that resolves to the start message and new process instance.
   */
  async startProcess(
    processKey: string,
    variables: Record<string, unknown> = {},
    businessKey?: string
  ): Promise<{ message: string; processInstance: ProcessInstance }> {
    console.log(
      '[processesApi] startProcess called with key:',
      processKey,
      'variables:',
      variables,
      'businessKey:',
      businessKey
    );
    const payload: { variables: Record<string, unknown>; businessKey?: string } = { variables };
    if (businessKey) {
      payload.businessKey = businessKey;
    }

    return fetchApi(`/api/processes/${processKey}/start`, {
      method: 'POST',
      body: JSON.stringify(payload)
    });
  },

  /**
   * Fetch a specific process instance by ID.
   * @param processInstanceId - The ID of the process instance.
   * @returns A promise that resolves to the process instance details.
   */
  async getProcessInstance(processInstanceId: string): Promise<ProcessInstance> {
    console.log('[processesApi] getProcessInstance called with id:', processInstanceId);
    return fetchApi(`/api/processes/instance/${processInstanceId}`);
  },

  /**
   * Export a process instance's data as a blob.
   * @param processInstanceId - The ID of the process instance to export.
   * @returns A promise that resolves to a Blob containing the exported data.
   */
  async exportProcessInstance(processInstanceId: string): Promise<Blob> {
    console.log('[processesApi] exportProcessInstance called with id:', processInstanceId);
    return fetchApi(`/api/processes/instance/${processInstanceId}/export`, {
      responseType: 'blob'
    });
  },

  /**
   * Cancel (delete) a process instance.
   * @param processInstanceId - The ID of the process instance.
   * @param reason - The reason for cancellation.
   * @returns A promise that resolves to a success message.
   */
  async cancelProcessInstance(
    processInstanceId: string,
    reason: string = 'User cancellation'
  ): Promise<{ message: string }> {
    console.log(
      '[processesApi] cancelProcessInstance called with id:',
      processInstanceId,
      'reason:',
      reason
    );
    const params = new URLSearchParams();
    if (reason) params.append('reason', reason);
    return fetchApi(`/api/processes/instance/${processInstanceId}?${params.toString()}`, {
      method: 'DELETE'
    });
  },

  /**
   * Fetch processes initiated by the current user.
   * @param page - Page number (default 0).
   * @param size - Page size (default 10).
   * @returns A promise that resolves to a page of process instances.
   */
  async getMyProcesses(page: number = 0, size: number = 10): Promise<Page<ProcessInstance>> {
    console.log('[processesApi] getMyProcesses called with page:', page, 'size:', size);
    const params = new URLSearchParams();
    params.append('page', page.toString());
    params.append('size', size.toString());
    return fetchApi(`/api/processes/my-processes?${params.toString()}`);
  },

  /**
   * Fetch a list of all users.
   * @returns A promise that resolves to an array of users.
   */
  async getUsers(): Promise<User[]> {
    console.log('[processesApi] getUsers called');
    return fetchApi('/api/processes/users');
  },

  // Workflow History & Processes
  /**
   * Fetch all workflow processes with optional filtering.
   * @param status - Filter by process status.
   * @param processType - Filter by process definition key.
   * @param page - Page number (default 0).
   * @param size - Page size (default 20).
   * @returns A promise that resolves to an array of workflow histories.
   */
  async getAllWorkflowProcesses(
    status?: string,
    processType?: string,
    page: number = 0,
    size: number = 20
  ): Promise<WorkflowHistory[]> {
    console.log(
      '[processesApi] getAllWorkflowProcesses called with status:',
      status,
      'type:',
      processType,
      'page:',
      page
    );
    const params = new URLSearchParams();
    if (status) params.append('status', status);
    if (processType) params.append('processType', processType);
    params.append('page', page.toString());
    params.append('size', size.toString());
    return fetchApi(`/api/workflow/processes?${params.toString()}`);
  },

  /**
   * Fetch the full history of a specific workflow process.
   * @param processInstanceId - The ID of the process instance.
   * @returns A promise that resolves to the workflow history.
   */
  async getWorkflowHistory(processInstanceId: string): Promise<WorkflowHistory> {
    console.log('[processesApi] getWorkflowHistory called with id:', processInstanceId);
    return fetchApi(`/api/workflow/processes/${processInstanceId}`);
  },

  /**
   * Add a comment to a process instance.
   * @param processInstanceId - The ID of the process instance.
   * @param message - The comment message.
   * @returns A promise that resolves to the created comment.
   */
  async addComment(
    processInstanceId: string,
    message: string
  ): Promise<{ message: string; comment: Comment }> {
    console.log(
      '[processesApi] addComment called with id:',
      processInstanceId,
      'message:',
      message
    );
    return fetchApi(`/api/workflow/processes/${processInstanceId}/comments`, {
      method: 'POST',
      body: JSON.stringify({ message })
    });
  },

  // Process Management
  /**
   * Deploy a new process definition.
   * @param processName - The name of the process.
   * @param bpmnXml - The BPMN 2.0 XML content.
   * @returns A promise that resolves to the deployment result.
   */
  async deployProcess(
    processName: string,
    bpmnXml: string
  ): Promise<{ message: string; process: ProcessDefinition }> {
    console.log('[processesApi] deployProcess called with name:', processName);
    return fetchApi('/api/processes/deploy', {
      method: 'POST',
      body: JSON.stringify({ processName, bpmnXml })
    });
  },

  /**
   * Get the BPMN XML for a process definition.
   * @param processDefinitionId - The ID of the process definition.
   * @returns A promise that resolves to an object containing the BPMN XML.
   */
  async getProcessBpmn(processDefinitionId: string): Promise<{ bpmn: string }> {
    console.log('[processesApi] getProcessBpmn called with id:', processDefinitionId);
    return fetchApi(`/api/processes/${processDefinitionId}/bpmn`);
  },

  /**
   * Delete a process definition.
   * @param processDefinitionId - The ID of the process definition.
   * @param cascade - Whether to cascade delete running instances (default false).
   * @returns A promise that resolves to a success message.
   */
  async deleteProcess(
    processDefinitionId: string,
    cascade: boolean = false
  ): Promise<{ message: string }> {
    console.log(
      '[processesApi] deleteProcess called with id:',
      processDefinitionId,
      'cascade:',
      cascade
    );
    return fetchApi(`/api/processes/${processDefinitionId}?cascade=${cascade}`, {
      method: 'DELETE'
    });
  },

  /**
   * Suspend a process definition.
   * @param processDefinitionId - The ID of the process definition.
   * @returns A promise that resolves to a success message.
   */
  async suspendProcess(processDefinitionId: string): Promise<{ message: string }> {
    console.log('[processesApi] suspendProcess called with id:', processDefinitionId);
    return fetchApi(`/api/processes/${processDefinitionId}/suspend`, { method: 'PUT' });
  },

  /**
   * Activate a suspended process definition.
   * @param processDefinitionId - The ID of the process definition.
   * @returns A promise that resolves to a success message.
   */
  async activateProcess(processDefinitionId: string): Promise<{ message: string }> {
    console.log('[processesApi] activateProcess called with id:', processDefinitionId);
    return fetchApi(`/api/processes/${processDefinitionId}/activate`, { method: 'PUT' });
  },

  /**
   * Update the category of a process definition.
   * @param processDefinitionId - The ID of the process definition.
   * @param category - The new category.
   * @returns A promise that resolves to a success message.
   */
  async updateProcessCategory(
    processDefinitionId: string,
    category: string
  ): Promise<{ message: string }> {
    console.log(
      '[processesApi] updateProcessCategory called with id:',
      processDefinitionId,
      'category:',
      category
    );
    return fetchApi(`/api/processes/${processDefinitionId}/category`, {
      method: 'PUT',
      body: JSON.stringify({ category })
    });
  },

  // Form Definitions
  /**
   * Get the start form definition for a process.
   * @param processDefinitionId - The ID of the process definition.
   * @returns A promise that resolves to the form definition.
   */
  async getStartFormDefinition(processDefinitionId: string): Promise<FormDefinition> {
    console.log('[processesApi] getStartFormDefinition called with id:', processDefinitionId);
    return fetchApi(`/api/processes/${processDefinitionId}/start-form`);
  },

  /**
   * Get all form definitions associated with a process.
   * @param processDefinitionId - The ID of the process definition.
   * @returns A promise that resolves to a map of form definitions.
   */
  async getAllFormDefinitions(
    processDefinitionId: string
  ): Promise<Record<string, FormDefinition>> {
    console.log('[processesApi] getAllFormDefinitions called with id:', processDefinitionId);
    return fetchApi(`/api/processes/${processDefinitionId}/forms`);
  },

  /**
   * Get the form definition for a specific element within a process.
   * @param processDefinitionId - The ID of the process definition.
   * @param elementId - The ID of the BPMN element (e.g., user task).
   * @returns A promise that resolves to the form definition.
   */
  async getElementFormDefinition(
    processDefinitionId: string,
    elementId: string
  ): Promise<FormDefinition> {
    console.log(
      '[processesApi] getElementFormDefinition called with processId:',
      processDefinitionId,
      'elementId:',
      elementId
    );
    return fetchApi(`/api/processes/${processDefinitionId}/forms/${elementId}`);
  }
};