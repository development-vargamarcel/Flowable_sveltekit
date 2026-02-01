import { fetchApi } from './core';
import type { Dashboard, SlaStats } from '$lib/types';

export const analyticsApi = {
  // Analytics Endpoints
  async getProcessDurationAnalytics(
    processDefinitionKey?: string
  ): Promise<{ label: string; count: number }[]> {
    const query = processDefinitionKey ? `?processDefinitionKey=${processDefinitionKey}` : '';
    return fetchApi(`api/analytics/process-duration${query}`);
  },

  async getUserPerformanceAnalytics(): Promise<
    { userId: string; tasksCompleted: number; avgDurationHours: number }[]
  > {
    return fetchApi('api/analytics/user-performance');
  },

  async getBottlenecks(): Promise<
    Array<{
      processDefinitionKey: string;
      taskName: string;
      avgDurationHours: number;
      slowInstanceCount: number;
      totalInstances: number;
    }>
  > {
    return fetchApi('/api/analytics/bottlenecks');
  },

  // Workflow Dashboard
  async getDashboard(
    page: number = 0,
    size: number = 10,
    status?: string,
    type?: string
  ): Promise<Dashboard> {
    const params = new URLSearchParams();
    params.append('page', page.toString());
    params.append('size', size.toString());
    if (status) {
      params.append('status', status);
    }
    if (type) {
      params.append('type', type);
    }
    return fetchApi(`/api/workflow/dashboard?${params.toString()}`);
  },

  // SLA Operations
  async getSlaStats(): Promise<SlaStats> {
    return fetchApi<SlaStats>('/api/slas/stats');
  },

  async createOrUpdateSLA(
    name: string,
    targetKey: string,
    targetType: 'PROCESS' | 'TASK',
    duration: string,
    warningThreshold?: number
  ): Promise<void> {
    const params = new URLSearchParams();
    params.append('name', name);
    params.append('targetKey', targetKey);
    params.append('targetType', targetType);
    params.append('duration', duration);
    if (warningThreshold) {
      params.append('warningThreshold', warningThreshold.toString());
    }

    await fetchApi(`/api/slas?${params.toString()}`, {
      method: 'POST'
    });
  },

  async checkSLABreaches(): Promise<void> {
    await fetchApi('/api/slas/check', { method: 'POST' });
  }
};
