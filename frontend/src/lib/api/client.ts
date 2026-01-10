import type {
  User,
  Task,
  TaskDetails,
  ProcessDefinition,
  ProcessInstance,
  LoginRequest,
  Dashboard,
  WorkflowHistory,
  EscalationRequest,
  Escalation,
  EscalationOptions,
  Approval,
  HandoffRequest,
  FormDefinition,
  TaskFormWithConfig,
  TableColumn,
  TableDataResponse,
  DocumentDTO,
  GridRowDTO,
  SaveDocumentRequest,
  SaveGridRowsRequest,
  Page,
  SlaStats
} from '$lib/types';
import { createLogger } from '$lib/utils/logger';
import { backendStatus } from '$lib/stores/backendStatus';

// In production, use relative URLs (empty string) so nginx can proxy /api/* to backend
// In development, use localhost:8080 for direct backend access
const API_BASE =
  import.meta.env.VITE_API_URL ?? (import.meta.env.DEV ? 'http://localhost:8080' : '');

const log = createLogger('api');

// Retry configuration for backend startup
const STARTUP_RETRY_CONFIG = {
  maxRetries: 10,
  initialDelayMs: 1000,
  maxDelayMs: 5000,
  backoffMultiplier: 1.5
};

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
  errorBody: Record<string, unknown> | null,
  rawResponse?: string
): ApiError {
  // Default error messages by status code with more helpful descriptions
  const defaultMessages: Record<number, { error: string; details: string }> = {
    400: { error: 'Bad request', details: 'The request was malformed or contained invalid data' },
    401: { error: 'Authentication failed', details: 'Invalid credentials or session expired' },
    403: {
      error: 'Access forbidden',
      details: 'You do not have permission to access this resource'
    },
    404: { error: 'Resource not found', details: 'The requested resource does not exist' },
    405: {
      error: 'Method not allowed',
      details: 'This HTTP method is not supported for this endpoint'
    },
    415: {
      error: 'Unsupported media type',
      details: 'The Content-Type header is missing or unsupported'
    },
    422: { error: 'Validation error', details: 'The submitted data failed validation' },
    500: { error: 'Internal server error', details: 'An unexpected error occurred on the server' },
    502: { error: 'Backend unavailable', details: 'The backend server is not responding' },
    503: { error: 'Service unavailable', details: 'The service is temporarily unavailable' },
    504: { error: 'Gateway timeout', details: 'The backend server took too long to respond' }
  };

  const defaultInfo = defaultMessages[status] || {
    error: `Request failed (${status})`,
    details: statusText
  };

  if (!errorBody) {
    // If we have a raw response that's not JSON, include it in details
    const detailsWithRaw = rawResponse
      ? `${defaultInfo.details}. Server response: ${rawResponse.substring(0, 200)}${rawResponse.length > 200 ? '...' : ''}`
      : defaultInfo.details;

    return new ApiError(defaultInfo.error, status, statusText, detailsWithRaw);
  }

  // Extract error information from various response formats
  // Handle both Spring Boot and custom error formats
  const error = (errorBody.error as string) ?? undefined;
  const message = (errorBody.message as string) ?? undefined;
  const details = (errorBody.details as string) ?? undefined;
  const fieldErrors = (errorBody.fieldErrors as Record<string, string>) ?? undefined;
  const timestamp = (errorBody.timestamp as string) ?? undefined;
  // Spring Boot specific fields
  const path = (errorBody.path as string) ?? undefined;
  // Note: trace is available but not used to avoid exposing stack traces to users
  const _trace = (errorBody.trace as string) ?? undefined;
  void _trace; // Suppress unused variable warning

  // Build the error message - prefer specific message over generic error
  let errorMessage: string;
  let errorDetails: string | undefined;

  if (message && message.length > 0 && message !== error) {
    // Use message as primary (usually more descriptive)
    if (error && error.length > 0) {
      errorMessage = error;
      errorDetails = message;
    } else {
      errorMessage = message;
      errorDetails = details;
    }
  } else if (error && error.length > 0) {
    errorMessage = error;
    errorDetails = details || message;
  } else if (details && details.length > 0) {
    errorMessage = defaultInfo.error;
    errorDetails = details;
  } else {
    errorMessage = defaultInfo.error;
    errorDetails = defaultInfo.details;
  }

  // Format field errors into details if present
  if (fieldErrors && Object.keys(fieldErrors).length > 0) {
    const fieldErrorMessages = Object.entries(fieldErrors)
      .map(([field, msg]) => `${field}: ${msg}`)
      .join('; ');
    errorDetails = errorDetails
      ? `${errorDetails}. Field errors: ${fieldErrorMessages}`
      : `Field errors: ${fieldErrorMessages}`;
  }

  // Add path info for debugging if available
  if (path && !errorDetails?.includes(path)) {
    errorDetails = errorDetails ? `${errorDetails} (path: ${path})` : `Path: ${path}`;
  }

  return new ApiError(errorMessage, status, statusText, errorDetails, fieldErrors, timestamp);
}

/**
 * Check if an error response indicates the backend is still starting up or unavailable
 */
function isBackendStartingError(
  errorBody: Record<string, unknown> | null,
  status?: number
): boolean {
  // 502 errors from the proxy indicate backend is unavailable - should retry
  if (status === 502) return true;

  if (!errorBody) return false;

  const error = errorBody.error as string | undefined;
  const message = errorBody.message as string | undefined;

  // Check for the specific startup message from Railway
  if (error === 'Service starting') return true;
  if (message?.toLowerCase().includes('backend is initializing')) return true;
  if (message?.toLowerCase().includes('service starting')) return true;

  // Check for proxy error messages from hooks.server.ts
  if (error?.toLowerCase().includes('backend unavailable')) return true;
  if (error?.toLowerCase().includes('connection refused')) return true;
  if (error?.toLowerCase().includes('connection timed out')) return true;
  if (message?.toLowerCase().includes('could not connect')) return true;

  return false;
}

/**
 * Sleep for a specified number of milliseconds
 */
function sleep(ms: number): Promise<void> {
  return new Promise((resolve) => setTimeout(resolve, ms));
}

/**
 * Calculate delay for next retry using exponential backoff
 */
function getRetryDelay(attempt: number): number {
  const delay =
    STARTUP_RETRY_CONFIG.initialDelayMs * Math.pow(STARTUP_RETRY_CONFIG.backoffMultiplier, attempt);
  return Math.min(delay, STARTUP_RETRY_CONFIG.maxDelayMs);
}

async function fetchApi<T>(endpoint: string, options: RequestInit = {}): Promise<T> {
  const url = `${API_BASE}${endpoint}`;
  const method = options.method || 'GET';

  log.debug(`${method} ${url}`, {
    body: options.body ? JSON.parse(options.body as string) : undefined
  });

  for (let attempt = 0; attempt <= STARTUP_RETRY_CONFIG.maxRetries; attempt++) {
    try {
      const response = await fetch(url, {
        ...options,
        credentials: 'include',
        headers: {
          'Content-Type': 'application/json',
          Accept: 'application/json',
          ...options.headers
        }
      });

      if (!response.ok) {
        // Read the raw response text first
        let rawText = '';
        let errorBody: Record<string, unknown> | null = null;

        try {
          rawText = await response.text();
          if (rawText && rawText.trim()) {
            // Check if it looks like JSON before parsing
            const trimmed = rawText.trim();
            if (trimmed.startsWith('{') || trimmed.startsWith('[')) {
              errorBody = JSON.parse(rawText);
            }
          }
        } catch {
          // Response body is not valid JSON
          log.warn('Failed to parse error response as JSON', {
            rawText: rawText.substring(0, 200)
          });
        }

        // Check if backend is still starting up or unavailable - retry with exponential backoff
        if (isBackendStartingError(errorBody, response.status)) {
          const delay = getRetryDelay(attempt);
          log.info(
            `Backend is starting, retrying in ${delay}ms (attempt ${attempt + 1}/${STARTUP_RETRY_CONFIG.maxRetries + 1})`,
            {
              url,
              method,
              attempt
            }
          );

          // Update backend status store
          backendStatus.setStarting(attempt + 1, STARTUP_RETRY_CONFIG.maxRetries + 1);

          if (attempt < STARTUP_RETRY_CONFIG.maxRetries) {
            await sleep(delay);
            continue; // Retry the request
          }
          // Max retries reached, fall through to throw error
        }

        log.error(`${method} ${url} failed`, undefined, {
          status: response.status,
          statusText: response.statusText,
          errorBody,
          rawText: rawText.substring(0, 500)
        });

        // Mark backend as ready if we got a response (even an error)
        backendStatus.setReady();

        throw parseErrorResponse(response.status, response.statusText, errorBody, rawText);
      }

      // Success - mark backend as ready
      backendStatus.setReady();

      // Handle empty responses (204 No Content)
      const contentLength = response.headers.get('content-length');
      if (response.status === 204 || contentLength === '0') {
        return {} as T;
      }

      const data = await response.json();
      log.debug(`${method} ${url} success`, { data });
      return data;
    } catch (error) {
      // Re-throw ApiErrors as-is (they're already processed)
      if (error instanceof ApiError) {
        throw error;
      }

      // For network errors during startup, retry
      if (error instanceof TypeError && attempt < STARTUP_RETRY_CONFIG.maxRetries) {
        const delay = getRetryDelay(attempt);
        log.info(
          `Network error, backend may be starting. Retrying in ${delay}ms (attempt ${attempt + 1}/${STARTUP_RETRY_CONFIG.maxRetries + 1})`,
          {
            url,
            method,
            error: error.message,
            attempt
          }
        );
        backendStatus.setStarting(attempt + 1, STARTUP_RETRY_CONFIG.maxRetries + 1);
        await sleep(delay);
        continue;
      }

      // Handle network errors with more specific messages
      if (error instanceof TypeError) {
        const isConnectionRefused =
          error.message.includes('fetch') || error.message.includes('network');
        log.error('Network error occurred', error, { url, method });
        backendStatus.setError('Connection failed');
        throw new ApiError(
          'Connection failed',
          0,
          'Network Error',
          isConnectionRefused
            ? `Unable to connect to ${url}. The server may be down or there may be a network issue.`
            : `Network error: ${error.message}`
        );
      }

      // Handle other errors
      const errorMessage = error instanceof Error ? error.message : 'Unknown error';
      log.error('Unexpected error', error, { url, method });
      throw new ApiError(
        'Request failed',
        0,
        'Unknown Error',
        `An unexpected error occurred: ${errorMessage}`
      );
    }
  }

  // If we've exhausted all retries
  backendStatus.setError('Backend failed to start');
  throw new ApiError(
    'Backend unavailable',
    503,
    'Service Unavailable',
    `Backend is still starting after ${STARTUP_RETRY_CONFIG.maxRetries + 1} attempts. Please try again in a moment.`
  );
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

  /**
   * Clear all session cookies including HttpOnly cookies.
   * Used to recover from "Request Header Or Cookie Too Large" errors.
   */
  async clearSession(): Promise<{ message: string; details: string }> {
    return fetchApi('/api/auth/clear-session', { method: 'POST' });
  },

  async getCurrentUser(): Promise<User> {
    return fetchApi('/api/auth/me');
  },

  // SLA Endpoints
  async getSlaStats(): Promise<SlaStats> {
    return fetchApi<SlaStats>('/api/sla/stats');
  },

  async refreshSlaCheck(): Promise<void> {
    return fetchApi<void>('/api/sla/check', { method: 'POST' });
  },

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

  async getAllProcessDefinitions(): Promise<ProcessDefinition[]> {
    return fetchApi('/api/processes/definitions');
  },

  async startProcess(
    processKey: string,
    variables: Record<string, unknown>
  ): Promise<{ message: string; processInstance: ProcessInstance }> {
    return fetchApi(`/api/processes/${processKey}/start`, {
      method: 'POST',
      body: JSON.stringify({ variables })
    });
  },

  async getProcessInstance(processInstanceId: string): Promise<ProcessInstance> {
    return fetchApi(`/api/processes/instance/${processInstanceId}`);
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
  async getTaskFormDefinition(taskId: string): Promise<TaskFormWithConfig> {
    return fetchApi(`/api/tasks/${taskId}/form`);
  },

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
  },

  // Database Table Viewer
  async getDatabaseTables(): Promise<string[]> {
    return fetchApi('/api/database/tables');
  },

  async getTableColumns(tableName: string): Promise<TableColumn[]> {
    return fetchApi(`/api/database/tables/${tableName}/columns`);
  },

  async getTableData(
    tableName: string,
    page: number = 0,
    size: number = 20
  ): Promise<TableDataResponse> {
    const params = new URLSearchParams();
    params.append('page', page.toString());
    params.append('size', size.toString());
    return fetchApi(`/api/database/tables/${tableName}/data?${params.toString()}`);
  },

  // Save Draft
  async saveDraft(
    processDefinitionKey: string,
    processDefinitionName: string,
    variables: Record<string, unknown>,
    userId: string,
    processInstanceId?: string,
    businessKey?: string,
    documentType?: string
  ): Promise<{ message: string; processInstanceId: string }> {
    return fetchApi('/api/business/save-draft', {
      method: 'POST',
      body: JSON.stringify({
        processInstanceId,
        businessKey,
        processDefinitionKey,
        processDefinitionName,
        documentType,
        variables,
        userId
      })
    });
  },

  // ==================== Document Operations ====================

  /**
   * Get all documents for a process instance
   */
  async getDocuments(
    processInstanceId: string,
    page: number = 0,
    size: number = 10
  ): Promise<Page<DocumentDTO>> {
    const params = new URLSearchParams();
    params.append('page', page.toString());
    params.append('size', size.toString());
    return fetchApi(`/api/business/processes/${processInstanceId}/document-types?${params.toString()}`);
  },

  /**
   * Get a specific document by type
   */
  async getDocument(processInstanceId: string, documentType: string): Promise<DocumentDTO> {
    return fetchApi(`/api/business/processes/${processInstanceId}/document-types/${documentType}`);
  },

  /**
   * Save a document with specific type
   */
  async saveDocument(
    processInstanceId: string,
    documentType: string,
    request: SaveDocumentRequest
  ): Promise<DocumentDTO> {
    return fetchApi(`/api/business/processes/${processInstanceId}/document-types/${documentType}`, {
      method: 'POST',
      body: JSON.stringify(request)
    });
  },

  /**
   * Get grid rows for a document type
   */
  async getGridRows(
    processInstanceId: string,
    documentType: string,
    gridName: string,
    page: number = 0,
    size: number = 10
  ): Promise<Page<GridRowDTO>> {
    const params = new URLSearchParams();
    params.append('page', page.toString());
    params.append('size', size.toString());
    return fetchApi(
      `/api/business/processes/${processInstanceId}/document-types/${documentType}/grids/${gridName}?${params.toString()}`
    );
  },

  /**
   * Save grid rows for a document type
   */
  async saveGridRows(
    processInstanceId: string,
    documentType: string,
    gridName: string,
    request: SaveGridRowsRequest
  ): Promise<Page<GridRowDTO>> {
    return fetchApi(
      `/api/business/processes/${processInstanceId}/document-types/${documentType}/grids/${gridName}`,
      {
        method: 'POST',
        body: JSON.stringify(request)
      }
    );
  },

  /**
   * Delete grid rows for a document type
   */
  async deleteGridRows(
    processInstanceId: string,
    documentType: string,
    gridName: string
  ): Promise<void> {
    await fetchApi(
      `/api/business/processes/${processInstanceId}/document-types/${documentType}/grids/${gridName}`,
      {
        method: 'DELETE'
      }
    );
  },

  /**
   * Get all documents by business key
   */
  async getDocumentsByBusinessKey(
    businessKey: string,
    page: number = 0,
    size: number = 10
  ): Promise<Page<DocumentDTO>> {
    const params = new URLSearchParams();
    params.append('page', page.toString());
    params.append('size', size.toString());
    return fetchApi(
      `/api/business/document-types/all/by-business-key/${businessKey}?${params.toString()}`
    );
  },

  // ==================== Document Type Definitions ====================
  async getDocumentTypes(): Promise<any[]> {
    return fetchApi('/api/document-types');
  },

  async getDocumentType(key: string): Promise<any> {
    return fetchApi(`/api/document-types/${key}`);
  },

  async createDocumentType(data: any): Promise<any> {
    return fetchApi('/api/document-types', {
      method: 'POST',
      body: JSON.stringify(data)
    });
  },

  async updateDocumentType(key: string, data: any): Promise<any> {
    return fetchApi(`/api/document-types/${key}`, {
      method: 'PUT',
      body: JSON.stringify(data)
    });
  },

  async deleteDocumentType(key: string): Promise<void> {
    await fetchApi(`/api/document-types/${key}`, { method: 'DELETE' });
  },

  // ==================== SLA Operations ====================
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
  },

  // ==================== Notifications ====================
  async getNotifications(): Promise<any[]> {
    return fetchApi('/api/notifications');
  },

  async markNotificationAsRead(id: string): Promise<void> {
    await fetchApi(`/api/notifications/${id}/read`, { method: 'POST' });
  },

  async markAllNotificationsAsRead(): Promise<void> {
    await fetchApi('/api/notifications/read-all', { method: 'POST' });
  },

  async getUnreadNotificationCount(): Promise<number> {
    return fetchApi('/api/notifications/unread-count');
  }
};
