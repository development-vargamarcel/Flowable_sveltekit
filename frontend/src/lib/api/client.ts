import { authApi } from './auth';
import { tasksApi } from './tasks';
import { processesApi } from './processes';
import { analyticsApi } from './analytics';
import { documentsApi } from './documents';
import { notificationsApi } from './notifications';
import { ApiError } from './core';

// Export everything from core/sub-modules
export * from './core';
export * from './auth';
export * from './tasks';
export * from './processes';
export * from './analytics';
export * from './documents';
export * from './notifications';

// Aggregate API object for backward compatibility
export const api = {
  ...authApi,
  ...tasksApi,
  ...processesApi,
  ...analyticsApi,
  ...documentsApi,
  ...notificationsApi
};

export { ApiError };
