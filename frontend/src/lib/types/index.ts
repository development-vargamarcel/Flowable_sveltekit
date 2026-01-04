export interface User {
  id?: string;
  username: string;
  displayName: string;
  email: string | null;
  roles: string[];
  groups?: string[];
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
  description?: string | null;
  version: number;
  category?: string | null;
  suspended?: boolean;
  deploymentId?: string;
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
  message?: string; // General validation message
  customExpression?: string;
  customMessage?: string;
}

export interface FormElementLogic {
  type: 'JS' | 'SQL' | 'Dependency' | 'Visibility';
  content?: string;
  dependencies: string[];
  autoCalculate?: boolean;
}

export interface FormFieldOption {
  value: string;
  label: string;
}

export interface FormField {
  id?: string;
  name: string;
  label: string;
  type: string;
  required?: boolean;
  validation?: FormFieldValidation | null;
  options?: FormFieldOption[] | null;
  placeholder?: string;
  defaultValue?: string;
  defaultExpression?: string;
  tooltip?: string;
  readonly?: boolean;
  hidden?: boolean;
  logic?: FormElementLogic;
  hiddenExpression?: string;
  readonlyExpression?: string;
  requiredExpression?: string;
  gridColumn?: number;
  gridRow?: number;
  gridWidth?: number;
  cssClass?: string;
  onChange?: string;
  onBlur?: string;
}

export interface GridColumn {
  id?: string;
  name: string;
  label: string;
  type: string;
  required?: boolean;
  placeholder?: string;
  options?: FormFieldOption[] | null;
  min?: number;
  max?: number;
  step?: number;
  validation?: FormFieldValidation | null;
  logic?: FormElementLogic;
}

export interface FormGrid {
  id?: string;
  name: string;
  label: string;
  description?: string;
  minRows?: number;
  maxRows?: number;
  columns: GridColumn[];
  gridColumn?: number;
  gridRow?: number;
  gridWidth?: number;
  cssClass?: string;
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

// ============================================
// Conditional Field Settings Types
// ============================================

/**
 * Effect types for condition rules
 * - hidden: Hide the field/grid/column
 * - visible: Show the field/grid/column (can be overridden by hidden)
 * - readonly: Make the field/grid/column read-only
 * - editable: Make the field/grid/column editable (can be overridden by readonly)
 */
export type ConditionEffect = 'hidden' | 'visible' | 'readonly' | 'editable';

/**
 * Target type for condition rules
 * - all: Apply to all fields and grids
 * - field: Apply to specific fields
 * - grid: Apply to specific grids
 * - column: Apply to specific columns within grids
 */
export type ConditionTargetType = 'all' | 'field' | 'grid' | 'column';

/**
 * Target specification for a condition rule
 */
export interface ConditionTarget {
  type: ConditionTargetType;
  fieldNames?: string[]; // For type 'field': specific field names
  gridNames?: string[]; // For type 'grid': specific grid names
  columnTargets?: {
    // For type 'column': grid name + column names
    gridName: string;
    columnNames: string[];
  }[];
}

/**
 * A condition rule that determines field/grid visibility and editability
 */
export interface FieldConditionRule {
  id: string;
  name: string; // Human-readable name
  description?: string; // Optional description
  condition: string; // Expression: "${amount > 1000}" or "amount > 1000"
  effect: ConditionEffect; // What happens when condition is true
  target: ConditionTarget; // What this rule affects
  priority: number; // Evaluation order (higher = first, but least access wins)
  enabled: boolean; // Whether the rule is active
}

/**
 * Process-level field library - defines all fields/grids once per process
 */
export interface ProcessFieldLibrary {
  fields: FormField[]; // All fields available in this process
  grids: FormGrid[]; // All grids available in this process
}

/**
 * Reference to a field from the library with optional task-specific overrides
 */
export interface TaskFieldReference {
  fieldName: string; // Reference to field.name in library
  overrides?: Partial<FormField>; // Task-specific property overrides
}

/**
 * Reference to a grid from the library with optional task-specific overrides
 */
export interface TaskGridReference {
  gridName: string; // Reference to grid.name in library
  overrides?: Partial<FormGrid>; // Task-specific property overrides
  columnOverrides?: {
    // Per-column overrides
    columnName: string;
    overrides: Partial<GridColumn>;
  }[];
}

/**
 * Task-specific form configuration
 */
export interface TaskFormConfig {
  taskId: string; // BPMN task element ID
  fieldRefs: TaskFieldReference[];
  gridRefs: TaskGridReference[];
  taskConditions?: FieldConditionRule[]; // Task-level conditions (in addition to global)
  layout?: {
    gridConfig?: GridConfig;
    fieldPositions?: {
      // Override positions for this task
      fieldName: string;
      gridColumn: number;
      gridRow: number;
      gridWidth: number;
    }[];
  };
}

/**
 * Complete process form definition with field library and conditions
 */
export interface ProcessFormDefinition {
  processDefinitionId: string;
  fieldLibrary: ProcessFieldLibrary;
  globalConditions: FieldConditionRule[];
  tasks: TaskFormConfig[];
  defaultGridConfig: GridConfig;
}

/**
 * Enhanced task form response that includes both task-specific and process-level configuration
 */
export interface TaskFormWithConfig {
  taskForm: FormDefinition;
  processConfig: {
    processDefinitionId: string;
    fieldLibrary: ProcessFieldLibrary;
    globalConditions: FieldConditionRule[];
    defaultGridConfig: GridConfig;
  };
}

/**
 * Computed field state after evaluating all conditions
 */
export interface ComputedFieldState {
  isHidden: boolean;
  isReadonly: boolean;
  appliedRules: string[]; // IDs of rules that affected this field
}

/**
 * Computed grid state after evaluating all conditions
 */
export interface ComputedGridState {
  isHidden: boolean;
  isReadonly: boolean;
  columnStates: Record<string, ComputedFieldState>; // Column name -> state
  appliedRules: string[];
}

/**
 * Extended FormField with computed runtime state
 */
export interface RuntimeFormField extends FormField {
  computedState?: ComputedFieldState;
}

/**
 * Extended FormGrid with computed runtime state
 */
export interface RuntimeFormGrid extends FormGrid {
  computedState?: ComputedGridState;
}

/**
 * Extended GridColumn with computed runtime state
 */
export interface RuntimeGridColumn extends GridColumn {
  computedState?: ComputedFieldState;
}

// ============================================
// Document Types (Process Data Storage)
// ============================================

/**
 * Document DTO - represents stored process data
 * Multiple documents per process (one per type)
 */
export interface DocumentDTO {
  id: number;
  processInstanceId: string;
  businessKey: string | null;
  processDefinitionKey: string | null;
  processDefinitionName: string | null;
  type: string; // Document type: "main", "contract", "invoice", etc.
  fields: Record<string, unknown>;
  grids: Record<string, Array<Record<string, unknown>>>;
  createdAt: string;
  updatedAt: string;
  createdBy: string | null;
  updatedBy: string | null;
}

/**
 * Grid row DTO - represents a single row in a grid
 */
export interface GridRowDTO {
  id: number;
  documentId: number;
  processInstanceId: string;
  gridName: string;
  rowIndex: number;
  fields: Record<string, unknown>;
}

/**
 * Request to save document data
 */
export interface SaveDocumentRequest {
  processInstanceId: string;
  businessKey?: string;
  processDefinitionKey?: string;
  processDefinitionName?: string;
  documentType?: string;
  variables: Record<string, unknown>;
  userId?: string;
}

/**
 * Request to save grid rows
 */
export interface SaveGridRowsRequest {
  processDefinitionKey?: string;
  documentType?: string;
  rows: Array<Record<string, unknown>>;
}

// ============================================
// Database Table Viewer Types
// ============================================

/**
 * Column metadata for a database table
 */
export interface TableColumn {
  name: string;
  type: string;
  size: number;
  nullable: boolean;
}

/**
 * Paginated table data response
 */
export interface TableDataResponse {
  tableName: string;
  columns: string[];
  rows: Record<string, unknown>[];
  page: number;
  size: number;
  totalRows: number;
  totalPages: number;
}

// ============================================
// Pagination Types
// ============================================

/**
 * Generic page response for paginated API results
 */
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

// ============================================
// SLA Types
// ============================================

/**
 * SLA statistics and metrics
 */
export interface SlaStats {
  totalProcesses: number;
  onTrack: number;
  atRisk: number;
  breached: number;
  avgCompletionPercentage: number;
  processesByStatus: {
    status: string;
    count: number;
  }[];
}
