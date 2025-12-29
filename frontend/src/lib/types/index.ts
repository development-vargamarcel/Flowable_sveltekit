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
  id: string;
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

// Escalation Types
export interface Escalation {
  id: string;
  taskId: string;
  processInstanceId: string;
  fromUserId: string;
  fromUserName: string | null;
  toUserId: string | null;
  toUserName: string | null;
  fromLevel: string;
  toLevel: string;
  reason: string;
  type: 'ESCALATE' | 'DE_ESCALATE';
  timestamp: string;
}

export interface EscalationRequest {
  reason: string;
  targetUserId?: string;
  targetLevel?: string;
  comments?: string;
}

export interface EscalationOptions {
  escalateTo: string[];
  deEscalateTo: string[];
}

// Approval Types
export interface Approval {
  id: string;
  processInstanceId: string;
  taskId: string;
  taskName: string;
  approverId: string;
  approverName: string | null;
  approverLevel: string;
  decision: 'APPROVED' | 'REJECTED' | 'ESCALATED' | 'DE_ESCALATED' | 'REQUEST_CHANGES';
  comments: string | null;
  timestamp: string;
  stepOrder: number;
  isRequired: boolean;
}

// Task History Types
export interface TaskHistory {
  id: string;
  taskDefinitionKey: string;
  name: string;
  description: string | null;
  processInstanceId: string;
  assignee: string | null;
  owner: string | null;
  createTime: string;
  claimTime: string | null;
  endTime: string | null;
  durationInMillis: number | null;
  deleteReason: string | null;
  variables: Record<string, unknown>;
}

// Workflow History Types
export interface WorkflowHistory {
  processInstanceId: string;
  processDefinitionKey: string;
  processDefinitionName: string | null;
  businessKey: string;
  status: 'ACTIVE' | 'COMPLETED' | 'SUSPENDED' | 'TERMINATED';
  initiatorId: string | null;
  initiatorName: string | null;
  startTime: string;
  endTime: string | null;
  durationInMillis: number | null;
  currentTaskId: string | null;
  currentTaskName: string | null;
  currentAssignee: string | null;
  currentLevel: string;
  escalationCount: number;
  variables: Record<string, unknown>;
  taskHistory: TaskHistory[];
  escalationHistory: Escalation[];
  approvals: Approval[];
}

// Dashboard Types
export interface DashboardStats {
  totalActive: number;
  totalCompleted: number;
  totalPending: number;
  myTasks: number;
  myProcesses: number;
  pendingEscalations: number;
  avgCompletionTimeHours: number;
}

export interface EscalationMetrics {
  totalEscalations: number;
  totalDeEscalations: number;
  activeEscalatedProcesses: number;
  escalationsByLevel: Record<string, number>;
}

export interface Dashboard {
  stats: DashboardStats;
  activeByType: Record<string, number>;
  byStatus: Record<string, number>;
  recentCompleted: WorkflowHistory[];
  activeProcesses: WorkflowHistory[];
  myPendingApprovals: WorkflowHistory[];
  escalationMetrics: EscalationMetrics;
}

// Handoff Types
export interface HandoffRequest {
  toUserId: string;
  reason: string;
}

// Form Definition Types
export interface FormFieldValidation {
  minLength?: number;
  maxLength?: number;
  min?: number;
  max?: number;
  pattern?: string;
  patternMessage?: string;
  customExpression?: string;
  customMessage?: string;
}

export interface FormFieldOption {
  value: string;
  label: string;
}

export interface FormField {
  id: string;
  name: string;
  label: string;
  type: string;
  required: boolean;
  validation: FormFieldValidation | null;
  options: FormFieldOption[] | null;
  placeholder: string;
  defaultValue: string;
  defaultExpression: string;
  tooltip: string;
  readonly: boolean;
  hidden: boolean;
  hiddenExpression: string;
  readonlyExpression: string;
  requiredExpression: string;
  gridColumn: number;
  gridRow: number;
  gridWidth: number;
  cssClass: string;
  onChange: string;
  onBlur: string;
}

export interface GridColumn {
  id: string;
  name: string;
  label: string;
  type: string;
  required: boolean;
  placeholder: string;
  options: string[] | null;
  min?: number;
  max?: number;
  step?: number;
  validation: FormFieldValidation | null;
}

export interface FormGrid {
  id: string;
  name: string;
  label: string;
  description: string;
  minRows: number;
  maxRows: number;
  columns: GridColumn[];
  gridColumn: number;
  gridRow: number;
  gridWidth: number;
  cssClass: string;
}

export interface GridConfig {
  columns: number;
  gap: number;
}

export interface FormDefinition {
  elementId: string;
  elementName: string;
  elementType: string;
  fields: FormField[];
  grids: FormGrid[];
  gridConfig: GridConfig;
}
