export interface Page<T> {
  content: T[];
  pageable: {
    pageNumber: number;
    pageSize: number;
    sort: {
      sorted: boolean;
      unsorted: boolean;
      empty: boolean;
    };
    offset: number;
    paged: boolean;
    unpaged: boolean;
  };
  last: boolean;
  totalPages: number;
  totalElements: number;
  size: number;
  number: number;
  sort: {
    sorted: boolean;
    unsorted: boolean;
    empty: boolean;
  };
  first: boolean;
  numberOfElements: number;
  empty: boolean;
}

export interface ProcessDefinition {
  id: string;
  key: string;
  name: string;
  version: number;
  deploymentId: string;
}

export interface ProcessInstance {
  id: string;
  processDefinitionId: string;
  processDefinitionKey: string;
  processDefinitionName: string;
  businessKey: string;
  startTime: string;
  endTime: string | null;
  durationInMillis: number | null;
  state: string;
  variables: Record<string, any>;
  tasks: Task[];
}

export interface Task {
  id: string;
  name: string;
  assignee: string;
  created: string;
  dueDate: string | null;
  followUpDate: string | null;
  delegationState: string;
  description: string;
  executionId: string;
  owner: string;
  parentTaskId: string;
  priority: number;
  processDefinitionId: string;
  processInstanceId: string;
  taskDefinitionKey: string;
  caseExecutionId: string;
  caseInstanceId: string;
  caseDefinitionId: string;
  suspended: boolean;
  formKey: string;
  tenantId: string;
}

export interface Dashboard {
  stats: {
    totalActive: number;
    totalCompleted: number;
    totalPending: number;
    myTasks: number;
    myProcesses: number;
    pendingEscalations: number;
    avgCompletionTimeHours: number;
  };
  activeProcesses: Page<WorkflowHistory>;
  recentCompleted: Page<WorkflowHistory>;
  myPendingApprovals: Page<WorkflowHistory>;
  activeByType: Record<string, number>;
  escalationMetrics: {
    totalEscalations: number;
    totalDeEscalations: number;
    activeEscalatedProcesses: number;
    escalationsByLevel: Record<string, number>;
  };
}

export interface WorkflowHistory {
  id: string;
  processDefinitionId: string;
  processDefinitionKey: string;
  processDefinitionName: string;
  businessKey: string;
  startTime: string;
  endTime: string | null;
  durationInMillis: number | null;
  status: string;
  initiatorId: string;
  initiatorName: string;
  currentTaskName: string | null;
  currentTaskId: string | null;
  currentAssignee: string | null;
  currentLevel: string;
  escalationCount: number;
  taskHistory: TaskHistory[];
  escalationHistory: EscalationHistory[];
  approvals: Approval[];
  variables: Record<string, any>;
}

export interface TaskHistory {
  taskId: string;
  taskName: string;
  assignee: string | null;
  startTime: string;
  endTime: string | null;
  duration: number | null;
  decision: string | null;
}

export interface EscalationHistory {
  id: string;
  type: 'ESCALATE' | 'DE_ESCALATE';
  fromLevel: string;
  toLevel: string;
  reason: string;
  fromUserId: string;
  toUserId: string;
  timestamp: string;
}

export interface Approval {
  id: string;
  approverId: string;
  approverLevel: string;
  stepOrder: number;
  taskName: string;
  decision: 'APPROVED' | 'REJECTED' | 'PENDING';
  comments: string | null;
  timestamp: string;
}

export interface FormField {
  id?: string;
  name: string;
  label: string;
  type: string;
  required?: boolean;
  readonly?: boolean;
  placeholder?: string;
  defaultValue?: string;
  tooltip?: string;
  options?: Array<{ label: string; value: string }>;
  validation?: {
    min?: number;
    max?: number;
    minLength?: number;
    maxLength?: number;
    pattern?: string;
    message?: string;
  };
  gridColumn?: number;
  gridWidth?: number;
  cssClass?: string;
}

export interface GridColumn {
  id?: string;
  name: string;
  label: string;
  type: string;
  width?: number;
  required?: boolean;
  options?: Array<{ label: string; value: string }>;
  validation?: any;
}

export interface FormGrid {
  id?: string;
  name: string;
  label: string;
  columns: GridColumn[];
}

export interface ProcessFieldLibrary {
  fields: FormField[];
  grids: FormGrid[];
}

export interface FieldConditionRule {
  id: string;
  fieldId: string;
  operator: string;
  value: string;
  action: 'show' | 'hide' | 'enable' | 'disable' | 'require';
  targetFieldId: string;
}
