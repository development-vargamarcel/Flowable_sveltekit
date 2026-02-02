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
  async getProcesses(): Promise<ProcessDefinition[]> {
    return fetchApi('/api/processes');
  },

  async getAllProcessDefinitions(): Promise<ProcessDefinition[]> {
    return fetchApi('/api/processes/definitions');
  },

  async startProcess(
    processKey: string,
    variables: Record<string, unknown> = {},
    businessKey?: string
  ): Promise<{ message: string; processInstance: ProcessInstance }> {
    const payload: { variables: Record<string, unknown>; businessKey?: string } = { variables };
    if (businessKey) {
      payload.businessKey = businessKey;
    }

    return fetchApi(`/api/processes/${processKey}/start`, {
      method: 'POST',
      body: JSON.stringify(payload)
    });
  },

  async getProcessInstance(processInstanceId: string): Promise<ProcessInstance> {
    return fetchApi(`/api/processes/instance/${processInstanceId}`);
  },

  async exportProcessInstance(processInstanceId: string): Promise<Blob> {
    return fetchApi(`/api/processes/instance/${processInstanceId}/export`, {
      responseType: 'blob'
    });
  },

  async getMyProcesses(page: number = 0, size: number = 10): Promise<Page<ProcessInstance>> {
    const params = new URLSearchParams();
    params.append('page', page.toString());
    params.append('size', size.toString());
    return fetchApi(`/api/processes/my-processes?${params.toString()}`);
  },

  async getUsers(): Promise<User[]> {
    return fetchApi('/api/processes/users');
  },

  // Workflow History & Processes
  async getAllWorkflowProcesses(
    status?: string,
    processType?: string,
    page: number = 0,
    size: number = 20
  ): Promise<WorkflowHistory[]> {
    const params = new URLSearchParams();
    if (status) params.append('status', status);
    if (processType) params.append('processType', processType);
    params.append('page', page.toString());
    params.append('size', size.toString());
    return fetchApi(`/api/workflow/processes?${params.toString()}`);
  },

  async getWorkflowHistory(processInstanceId: string): Promise<WorkflowHistory> {
    return fetchApi(`/api/workflow/processes/${processInstanceId}`);
  },

  async addComment(
    processInstanceId: string,
    message: string
  ): Promise<{ message: string; comment: Comment }> {
    return fetchApi(`/api/workflow/processes/${processInstanceId}/comments`, {
      method: 'POST',
      body: JSON.stringify({ message })
    });
  },

  // Process Management
  async deployProcess(
    processName: string,
    bpmnXml: string
  ): Promise<{ message: string; process: ProcessDefinition }> {
    return fetchApi('/api/processes/deploy', {
      method: 'POST',
      body: JSON.stringify({ processName, bpmnXml })
    });
  },

  async getProcessBpmn(processDefinitionId: string): Promise<{ bpmn: string }> {
    return fetchApi(`/api/processes/${processDefinitionId}/bpmn`);
  },

  async deleteProcess(
    processDefinitionId: string,
    cascade: boolean = false
  ): Promise<{ message: string }> {
    return fetchApi(`/api/processes/${processDefinitionId}?cascade=${cascade}`, {
      method: 'DELETE'
    });
  },

  async suspendProcess(processDefinitionId: string): Promise<{ message: string }> {
    return fetchApi(`/api/processes/${processDefinitionId}/suspend`, { method: 'PUT' });
  },

  async activateProcess(processDefinitionId: string): Promise<{ message: string }> {
    return fetchApi(`/api/processes/${processDefinitionId}/activate`, { method: 'PUT' });
  },

  async updateProcessCategory(
    processDefinitionId: string,
    category: string
  ): Promise<{ message: string }> {
    return fetchApi(`/api/processes/${processDefinitionId}/category`, {
      method: 'PUT',
      body: JSON.stringify({ category })
    });
  },

  // Form Definitions
  async getStartFormDefinition(processDefinitionId: string): Promise<FormDefinition> {
    return fetchApi(`/api/processes/${processDefinitionId}/start-form`);
  },

  async getAllFormDefinitions(
    processDefinitionId: string
  ): Promise<Record<string, FormDefinition>> {
    return fetchApi(`/api/processes/${processDefinitionId}/forms`);
  },

  async getElementFormDefinition(
    processDefinitionId: string,
    elementId: string
  ): Promise<FormDefinition> {
    return fetchApi(`/api/processes/${processDefinitionId}/forms/${elementId}`);
  }
};
