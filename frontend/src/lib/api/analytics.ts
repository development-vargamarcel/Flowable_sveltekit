/* eslint-disable no-console */
import { fetchApi } from './core';
import type { Dashboard, SlaStats } from '$lib/types';

export const analyticsApi = {
  // Analytics Endpoints
  /**
   * Get process duration analytics.
   * @param processDefinitionKey - Optional process definition key to filter by.
   * @returns A promise that resolves to an array of duration stats.
   */
  async getProcessDurationAnalytics(
    processDefinitionKey?: string
  ): Promise<{ label: string; count: number }[]> {
    console.log(
      '[analyticsApi] getProcessDurationAnalytics called with key:',
      processDefinitionKey
    );
    const query = processDefinitionKey ? `?processDefinitionKey=${processDefinitionKey}` : '';
    return fetchApi(`api/analytics/process-duration${query}`);
  },

  /**
   * Get user performance analytics.
   * @returns A promise that resolves to an array of user performance stats.
   */
  async getUserPerformanceAnalytics(): Promise<
    { userId: string; tasksCompleted: number; avgDurationHours: number }[]
  > {
    console.log('[analyticsApi] getUserPerformanceAnalytics called');
    return fetchApi('api/analytics/user-performance');
  },

  /**
   * Get bottleneck analytics.
   * @returns A promise that resolves to an array of bottleneck stats.
   */
  async getBottlenecks(): Promise<
    Array<{
      processDefinitionKey: string;
      taskName: string;
      avgDurationHours: number;
      slowInstanceCount: number;
      totalInstances: number;
    }>
  > {
    console.log('[analyticsApi] getBottlenecks called');
    return fetchApi('/api/analytics/bottlenecks');
  },

  /**
   * Get process completion trend.
   * @param days - Number of days to look back (default 7).
   * @returns A promise that resolves to an array of trend metrics.
   */
  async getProcessCompletionTrend(
    days: number = 7
  ): Promise<{ date: string; count: number }[]> {
    console.log('[analyticsApi] getProcessCompletionTrend called with days:', days);
    return fetchApi(`/api/analytics/completion-trend?days=${days}`);
  },

  // Workflow Dashboard
  /**
   * Get the workflow dashboard data.
   * @param page - Page number (default 0).
   * @param size - Page size (default 10).
   * @param status - Optional status filter.
   * @param type - Optional process type filter.
   * @returns A promise that resolves to the dashboard data.
   */
  async getDashboard(
    page: number = 0,
    size: number = 10,
    status?: string,
    type?: string
  ): Promise<Dashboard> {
    console.log(
      '[analyticsApi] getDashboard called with page:',
      page,
      'size:',
      size,
      'status:',
      status,
      'type:',
      type
    );
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
  /**
   * Get SLA statistics.
   * @returns A promise that resolves to the SLA statistics.
   */
  async getSlaStats(): Promise<SlaStats> {
    console.log('[analyticsApi] getSlaStats called');
    return fetchApi<SlaStats>('/api/slas/stats');
  },

  /**
   * Create or update an SLA definition.
   * @param name - The name of the SLA.
   * @param targetKey - The key of the process or task.
   * @param targetType - The type of target (PROCESS or TASK).
   * @param duration - The duration string (e.g., "PT2H").
   * @param warningThreshold - Optional warning threshold (0-1).
   */
  async createOrUpdateSLA(
    name: string,
    targetKey: string,
    targetType: 'PROCESS' | 'TASK',
    duration: string,
    warningThreshold?: number
  ): Promise<void> {
    console.log(
      '[analyticsApi] createOrUpdateSLA called with name:',
      name,
      'targetKey:',
      targetKey
    );
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

  /**
   * Trigger an SLA breach check.
   */
  async checkSLABreaches(): Promise<void> {
    console.log('[analyticsApi] checkSLABreaches called');
    await fetchApi('/api/slas/check', { method: 'POST' });
  }
};
