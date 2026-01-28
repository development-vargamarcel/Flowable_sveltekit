export interface ProcessDTO {
    id: string;
    key: string;
    name: string;
    description?: string;
    version: number;
    category?: string;
    suspended: boolean;
}

export interface ProcessDefinition {
    id: string;
    key: string;
    name: string;
    description?: string;
    version: number;
    category?: string;
    suspended: boolean;
    resourceName?: string;
    deploymentId?: string;
    diagramResourceName?: string;
    startFormKey?: string;
    graphicNotationDefined?: boolean;
}

export interface ProcessInstance {
    id: string;
    processDefinitionId: string;
    processDefinitionKey: string;
    processDefinitionName?: string;
    businessKey?: string;
    startTime: string;
    endTime?: string;
    startUserId?: string;
    ended: boolean;
    variables?: Record<string, any>;
}

export interface Task {
    id: string;
    name: string;
    description?: string;
    assignee?: string;
    owner?: string;
    createTime: string;
    dueDate?: string;
    priority: number;
    processInstanceId: string;
    processDefinitionId: string;
    taskDefinitionKey: string;
    processDefinitionKey?: string; // Optional convenience field
    processName?: string; // Optional convenience field
    businessKey?: string; // Optional convenience field
    variables?: Record<string, any>;
    formKey?: string;
}

export interface TaskDetails {
    task: Task;
    formKey?: string;
    formData?: any;
    variables?: Record<string, any>;
}

export interface TaskAction {
    action: string;
    variables?: Record<string, any>;
}

export interface User {
    id: string;
    username: string;
    firstName?: string;
    lastName?: string;
    displayName?: string;
    email?: string;
    roles: string[];
    groups?: string[];
}

export interface TaskHistory {
    id: string;
    name: string;
    assignee?: string;
    startTime: string;
    endTime?: string;
    duration?: number;
}

export interface Escalation {
    id: string;
    taskId: string;
    fromLevel: string;
    toLevel: string;
    fromUserId: string;
    reason: string;
    type: 'ESCALATE' | 'DE_ESCALATE';
    timestamp: string;
}

export interface Approval {
    id: string;
    taskId: string;
    taskName: string;
    approverId: string;
    approverLevel: string;
    decision: string;
    comments?: string;
    timestamp: string;
    stepOrder: number;
}

export interface Comment {
    id: string;
    message: string;
    authorId: string;
    timestamp: string;
}

export interface WorkflowHistory {
    processInstanceId: string;
    processDefinitionKey: string;
    processDefinitionName?: string;
    startTime: string;
    endTime?: string;
    startUserId?: string;
    variables?: Record<string, any>;
    tasks: Task[];
    activities?: any[]; // Simplified for now
    currentTaskId?: string; // Derived field for convenience
    currentTaskName?: string; // Derived
    currentAssignee?: string; // Derived
    status?: string; // Derived
    escalationCount?: number; // Derived
    currentLevel?: string; // Derived
    initiatorName?: string; // Derived
    initiatorId?: string; // Derived
    businessKey?: string; // Derived
    durationInMillis?: number; // Derived

    // Detailed history lists
    taskHistory: TaskHistory[];
    escalationHistory: Escalation[];
    approvals: Approval[];
    comments: Comment[];
}

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
  recentCompleted: Page<WorkflowHistory>;
  activeProcesses: Page<WorkflowHistory>;
  myPendingApprovals: Page<WorkflowHistory>;
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
  allowedMimeTypes?: string[];
  maxFileSize?: number;
}

export interface FormField {
  id: string;
  name: string;
  label: string;
  type: string;
  required: boolean;
  placeholder?: string;
  defaultValue?: any;
  options?: string[];
  validation?: FormFieldValidation;
  hiddenExpression?: string;
  readonlyExpression?: string;
  requiredExpression?: string;
  calculationExpression?: string;
  gridId?: string; // If field belongs to a grid
  tooltip?: string;
  readonly?: boolean;
  hidden?: boolean;
  richText?: boolean;
  signature?: boolean;
  pickerType?: string;
}

export interface GridColumn {
  id: string;
  name: string;
  label: string;
  type: 'text' | 'number' | 'date' | 'select' | 'textarea';
  required: boolean;
  placeholder?: string;
  options?: string[]; // For select types
  validation?: FormFieldValidation;
  hiddenExpression?: string;
  readonlyExpression?: string;
  requiredExpression?: string;
  calculationExpression?: string;
}

export interface GridDefinition {
  id: string;
  name: string;
  label: string;
  description?: string;
  minRows?: number;
  maxRows?: number;
  columns: GridColumn[];
  gridColumn?: number; // Layout position
  gridRow?: number; // Layout position
  gridWidth?: number; // Layout width
  cssClass?: string;
  visibilityExpression?: string;
  enablePagination?: boolean;
  pageSize?: number;
  enableSorting?: boolean;
  enableRowActions?: boolean;
  enableImportExport?: boolean;
  enableGrouping?: boolean;
  groupByColumn?: string;
}

export interface GridConfig {
  columns?: number;
  gap?: number;
}

export interface FormDefinition {
  id: string;
  name: string;
  processDefinitionKey: string;
  fields: FormField[];
  grids?: GridDefinition[];
  gridConfig?: GridConfig;
  layout?: any; // Grid layout configuration
}

// Legacy aliases for compatibility
export type FormGrid = GridDefinition;
export type ProcessFieldLibrary = any;

export type ConditionEffect = 'hidden' | 'visible' | 'readonly' | 'editable';
export type ConditionTargetType = 'all' | 'field' | 'grid' | 'column';

export type ConditionTarget =
  | { type: 'all' }
  | { type: 'field'; fieldNames: string[] }
  | { type: 'grid'; gridNames: string[] }
  | { type: 'column'; columnTargets: { gridName: string; columnNames: string[] }[] };

export interface FieldConditionRule {
  id: string;
  name: string;
  description?: string;
  condition: string;
  effect: ConditionEffect;
  target: ConditionTarget;
  priority: number;
  enabled: boolean;
}

// Fix TaskFormWithConfig type
export interface TaskFormWithConfig {
    taskForm: FormDefinition;
    processConfig: any;
}

// Table Data Types
export interface TableDataResponse {
  tableName: string; // Add tableName
  columns: string[];
  rows: Record<string, any>[];
  total: number;
  page: number;
  totalPages: number;
  size: number; // Add size
  totalRows: number; // Add totalRows
}

// Add Page interface since it was referenced but missing in previous grep
export interface Page<T> {
    content: T[];
    totalPages: number;
    totalElements: number;
    last: boolean;
    size: number;
    number: number;
    sort?: any;
    numberOfElements: number;
    first: boolean;
    empty: boolean;
}

export interface Notification {
    id: string;
    userId: string;
    title: string;
    message: string;
    type: 'TASK_ASSIGNED' | 'TASK_DUE_SOON' | 'TASK_OVERDUE' | 'PROCESS_COMPLETED' | 'PROCESS_REJECTED' | 'SLA_WARNING' | 'SLA_BREACH' | 'INFO';
    link?: string;
    read: boolean;
    createdAt: string;
}

export interface SlaStats {
    totalActiveSlas: number;
    breachedSlas: number;
    atRiskSlas: number;
    compliantSlas: number;
    breachRate: number;
    averageResolutionTime: number; // in hours
}

export interface LoginRequest {
    username: string;
    password: string;
}

export interface EscalationOptions {
    levels: string[];
    canEscalate: boolean;
    canDeEscalate: boolean;
    currentLevel: string;
    escalateTo: string[];
    deEscalateTo: string[];
}

export interface EscalationRequest {
    reason: string;
    targetLevel?: string;
    targetUserId?: string;
}

export interface TableColumn {
    name: string;
    type: string;
    primaryKey: boolean;
    nullable: boolean;
}

export interface DocumentDTO {
    id: string;
    name: string;
    type: string;
    content: string;
    mimeType: string;
    size: number;
    createdAt: string;
    createdBy: string;
}

export interface GridRowDTO {
    id: string;
    data: Record<string, any>;
    createdAt: string;
    createdBy: string;
}

export interface SaveDocumentRequest {
    name: string;
    type: string;
    content: string;
}

export interface SaveGridRowsRequest {
    rows: Record<string, any>[];
}
