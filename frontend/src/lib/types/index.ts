/**
 * Core types for the Flowable BPM Frontend Application
 * @module types
 */

// ============================================
// Utility Types
// ============================================

/**
 * Generic variable value that can be stored in a process
 */
export type ProcessVariableValue = string | number | boolean | null | Date | ProcessVariableValue[] | { [key: string]: ProcessVariableValue };

/**
 * Record of process variables
 */
export type ProcessVariables = Record<string, ProcessVariableValue>;

/**
 * Sort direction for pagination
 */
export interface SortInfo {
    sorted: boolean;
    unsorted: boolean;
    empty: boolean;
}

// ============================================
// Process Definition Types
// ============================================

/**
 * Simplified process data transfer object
 */
export interface ProcessDTO {
    id: string;
    key: string;
    name: string;
    description?: string;
    version: number;
    category?: string;
    suspended: boolean;
}

/**
 * Full process definition with all metadata
 */
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

/**
 * Running process instance
 */
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
    variables?: ProcessVariables;
}

// ============================================
// Task Types
// ============================================

/**
 * Task assigned to a user or group
 */
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
    /** Optional convenience field for process definition key */
    processDefinitionKey?: string;
    /** Optional convenience field for process name */
    processName?: string;
    /** Optional convenience field for business key */
    businessKey?: string;
    variables?: ProcessVariables;
    formKey?: string;
}

/**
 * Task with its associated form data
 */
export interface TaskDetails {
    task: Task;
    formKey?: string;
    formData?: FormDefinition;
    variables?: ProcessVariables;
}

/**
 * Action to perform on a task
 */
export interface TaskAction {
    action: string;
    variables?: ProcessVariables;
}

// ============================================
// User Types
// ============================================

/**
 * Authenticated user information
 */
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

// ============================================
// History Types
// ============================================

/**
 * Historical task completion record
 */
export interface TaskHistory {
    id: string;
    name: string;
    assignee?: string;
    startTime: string;
    endTime?: string;
    /** Duration in milliseconds */
    duration?: number;
}

/**
 * Escalation/de-escalation event
 */
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

/**
 * Approval decision record
 */
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

/**
 * Comment on a task or process
 */
export interface Comment {
    id: string;
    message: string;
    authorId: string;
    timestamp: string;
}

/**
 * Activity in the workflow execution
 */
export interface WorkflowActivity {
    id: string;
    activityId: string;
    activityName: string;
    activityType: string;
    startTime: string;
    endTime?: string;
    durationInMillis?: number;
    assignee?: string;
}

/**
 * Complete workflow history including all events
 */
export interface WorkflowHistory {
    processInstanceId: string;
    processDefinitionId: string;
    processDefinitionKey: string;
    processDefinitionName?: string;
    startTime: string;
    endTime?: string;
    startUserId?: string;
    variables?: ProcessVariables;
    tasks: Task[];
    activities?: WorkflowActivity[];
    /** Derived field: current active task ID */
    currentTaskId?: string;
    /** Derived field: current active task name */
    currentTaskName?: string;
    /** Derived field: current task assignee */
    currentAssignee?: string;
    /** Derived field: workflow status */
    status?: string;
    /** Derived field: number of escalations */
    escalationCount?: number;
    /** Derived field: current escalation level */
    currentLevel?: string;
    /** Derived field: name of process initiator */
    initiatorName?: string;
    /** Derived field: ID of process initiator */
    initiatorId?: string;
    /** Derived field: business key */
    businessKey?: string;
    /** Derived field: total duration in milliseconds */
    durationInMillis?: number;

    /** Detailed task history */
    taskHistory: TaskHistory[];
    /** Escalation history */
    escalationHistory: Escalation[];
    /** Approval history */
    approvals: Approval[];
    /** Comments */
    comments: Comment[];
}

// ============================================
// Dashboard Types
// ============================================

/**
 * Dashboard statistics summary
 */
export interface DashboardStats {
    totalActive: number;
    totalCompleted: number;
    totalPending: number;
    myTasks: number;
    myProcesses: number;
    pendingEscalations: number;
    avgCompletionTimeHours: number;
}

/**
 * Escalation metrics for dashboard
 */
export interface EscalationMetrics {
    totalEscalations: number;
    totalDeEscalations: number;
    activeEscalatedProcesses: number;
    escalationsByLevel: Record<string, number>;
}

/**
 * Full dashboard data structure
 */
export interface Dashboard {
    stats: DashboardStats;
    activeByType: Record<string, number>;
    byStatus: Record<string, number>;
    recentCompleted: Page<WorkflowHistory>;
    activeProcesses: Page<WorkflowHistory>;
    myPendingApprovals: Page<WorkflowHistory>;
    escalationMetrics: EscalationMetrics;
}

// ============================================
// Form Types
// ============================================

/**
 * Field validation rules
 */
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
    /** Maximum file size in bytes */
    maxFileSize?: number;
}

/**
 * Field type enum for better type safety
 */
export type FormFieldType =
    | 'text'
    | 'textarea'
    | 'number'
    | 'currency'
    | 'percentage'
    | 'email'
    | 'phone'
    | 'date'
    | 'datetime'
    | 'select'
    | 'multiselect'
    | 'radio'
    | 'checkbox'
    | 'file'
    | 'signature'
    | 'expression'
    | 'header'
    | 'userPicker'
    | 'groupPicker';

/**
 * Option for select/radio/checkbox fields
 */
export interface FieldOption {
    value: string;
    label: string;
}

/**
 * Form field definition
 */
export interface FormField {
    id: string;
    name: string;
    label: string;
    type: FormFieldType | string;
    required: boolean;
    placeholder?: string;
    defaultValue?: ProcessVariableValue;
    /** Options for select/radio fields - can be string[] or FieldOption[] */
    options?: (string | FieldOption)[];
    validation?: FormFieldValidation;
    /** Expression to control visibility */
    visibilityExpression?: string;
    /** Expression to determine if field is hidden */
    hiddenExpression?: string;
    /** Expression to determine if field is readonly */
    readonlyExpression?: string;
    /** Expression to determine if field is required */
    requiredExpression?: string;
    /** Expression to calculate field value */
    calculationExpression?: string;
    /** Custom validation expression */
    validationExpression?: string;
    /** Custom validation message */
    validationMessage?: string;
    /** If field belongs to a grid */
    gridId?: string;
    /** Help text tooltip */
    tooltip?: string;
    /** Static readonly state */
    readonly?: boolean;
    /** Static hidden state */
    hidden?: boolean;
    /** Enable rich text editor for textarea */
    richText?: boolean;
    /** Enable signature pad */
    signature?: boolean;
    /** Picker type for user/group pickers */
    pickerType?: string;
    /** Default expression for expression fields */
    defaultExpression?: string;
    /** CSS class for styling */
    cssClass?: string;
    /** Grid row position */
    gridRow: number;
    /** Grid column position */
    gridColumn: number;
    /** Grid width (columns to span) */
    gridWidth: number;
}

/**
 * Grid column types
 */
export type GridColumnType = 'text' | 'number' | 'date' | 'select' | 'textarea' | 'checkbox' | 'currency';

/**
 * Grid column definition
 */
export interface GridColumn {
    id: string;
    name: string;
    label: string;
    type: GridColumnType;
    required: boolean;
    placeholder?: string;
    /** Options for select columns */
    options?: (string | FieldOption)[];
    validation?: FormFieldValidation;
    hiddenExpression?: string;
    readonlyExpression?: string;
    requiredExpression?: string;
    calculationExpression?: string;
    /** Column width in pixels or percentage */
    width?: string | number;
}

/**
 * Grid definition for tabular data entry
 */
export interface GridDefinition {
    id: string;
    name: string;
    label: string;
    description?: string;
    minRows?: number;
    maxRows?: number;
    columns: GridColumn[];
    /** Layout grid column position */
    gridColumn: number;
    /** Layout grid row position */
    gridRow: number;
    /** Layout width (columns to span) */
    gridWidth: number;
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

/**
 * Grid layout configuration
 */
export interface GridConfig {
    /** Number of columns in the layout grid */
    columns: number;
    /** Gap between grid items in pixels */
    gap: number;
}

/**
 * Layout definition for form rendering
 */
export interface FormLayout {
    columns: number;
    gap: number;
    rows?: FormLayoutRow[];
}

/**
 * Row in form layout
 */
export interface FormLayoutRow {
    fields: string[];
    height?: string;
}

/**
 * Complete form definition
 */
export interface FormDefinition {
    id: string;
    name: string;
    processDefinitionKey: string;
    fields: FormField[];
    grids?: GridDefinition[];
    gridConfig?: GridConfig;
    layout?: FormLayout;
}

/** Legacy alias for GridDefinition */
export type FormGrid = GridDefinition;

// ============================================
// Condition Rule Types
// ============================================

/**
 * Effect to apply when condition is met
 */
export type ConditionEffect = 'hidden' | 'visible' | 'readonly' | 'editable';

/**
 * Target type for condition rules
 */
export type ConditionTargetType = 'all' | 'field' | 'grid' | 'column';

/**
 * Target specification for condition rules
 */
export type ConditionTarget =
    | { type: 'all' }
    | { type: 'field'; fieldNames: string[] }
    | { type: 'grid'; gridNames: string[] }
    | { type: 'column'; columnTargets: { gridName: string; columnNames: string[] }[] };

/**
 * Condition rule for dynamic form behavior
 */
export interface FieldConditionRule {
    id: string;
    name: string;
    description?: string;
    /** Expression to evaluate */
    condition: string;
    /** Effect to apply when condition is true */
    effect: ConditionEffect;
    /** Target fields/grids/columns */
    target: ConditionTarget;
    /** Rule priority (higher = evaluated first) */
    priority: number;
    /** Whether rule is active */
    enabled: boolean;
}

/**
 * Computed state for a field based on condition rules
 */
export interface ComputedFieldState {
    isHidden: boolean;
    isReadonly: boolean;
    isRequired?: boolean;
    appliedRules: string[];
}

/**
 * Computed state for a grid based on condition rules
 */
export interface ComputedGridState {
    isHidden: boolean;
    isReadonly: boolean;
    columnStates: Record<string, ComputedFieldState>;
    appliedRules: string[];
}

/**
 * Task form with associated process configuration
 */
export interface TaskFormWithConfig {
    taskForm: FormDefinition;
    processConfig: ProcessConfig;
}

/**
 * Process-level field library
 */
export interface ProcessFieldLibrary {
    fields: FormField[];
    grids: GridDefinition[];
}

/**
 * Process-level configuration
 */
export interface ProcessConfig {
    processDefinitionId?: string;
    fieldLibrary?: ProcessFieldLibrary;
    globalConditions?: FieldConditionRule[];
    defaultGridConfig?: GridConfig;

    // Legacy fields
    conditionRules?: FieldConditionRule[];
    taskConditionRules?: Record<string, FieldConditionRule[]>;
}

/**
 * Field definition in process field library
 */
export interface ProcessFieldDefinition {
    id: string;
    name: string;
    label: string;
    type: FormFieldType | string;
    defaultValue?: ProcessVariableValue;
    validation?: FormFieldValidation;
}

// ============================================
// Pagination Types
// ============================================

/**
 * Paginated response wrapper
 */
export interface Page<T> {
    content: T[];
    totalPages: number;
    totalElements: number;
    last: boolean;
    size: number;
    /** Current page number (0-indexed) */
    number: number;
    sort?: SortInfo;
    numberOfElements: number;
    first: boolean;
    empty: boolean;
}

// ============================================
// Table/Database Types
// ============================================

/**
 * Response from table data query
 */
export interface TableDataResponse {
    tableName: string;
    columns: string[];
    rows: Record<string, ProcessVariableValue>[];
    total: number;
    page: number;
    totalPages: number;
    size: number;
    totalRows: number;
}

/**
 * Database table column metadata
 */
export interface TableColumn {
    name: string;
    type: string;
    primaryKey: boolean;
    nullable: boolean;
}

// ============================================
// Document Types
// ============================================

/**
 * Document data transfer object
 */
export interface DocumentDTO {
    id: string;
    name: string;
    type: string;
    content: string;
    mimeType: string;
    /** Size in bytes */
    size: number;
    createdAt: string;
    createdBy: string;
}

/**
 * Grid row data transfer object
 */
export interface GridRowDTO {
    id: string;
    data: Record<string, ProcessVariableValue>;
    createdAt: string;
    createdBy: string;
}

/**
 * Request to save a document
 */
export interface SaveDocumentRequest {
    name: string;
    type: string;
    content: string;
}

/**
 * Request to save grid rows
 */
export interface SaveGridRowsRequest {
    rows: Record<string, ProcessVariableValue>[];
}

// ============================================
// Notification Types
// ============================================

/**
 * Notification type enum
 */
export type NotificationType =
    | 'TASK_ASSIGNED'
    | 'TASK_DUE_SOON'
    | 'TASK_OVERDUE'
    | 'PROCESS_COMPLETED'
    | 'PROCESS_REJECTED'
    | 'SLA_WARNING'
    | 'SLA_BREACH'
    | 'INFO';

/**
 * User notification
 */
export interface Notification {
    id: string;
    userId: string;
    title: string;
    message: string;
    type: NotificationType;
    link?: string;
    read: boolean;
    createdAt: string;
}

// ============================================
// SLA Types
// ============================================

/**
 * SLA statistics
 */
export interface SlaStats {
    totalActiveSlas: number;
    breachedSlas: number;
    atRiskSlas: number;
    compliantSlas: number;
    /** Breach rate as percentage (0-100) */
    breachRate: number;
    /** Average resolution time in hours */
    averageResolutionTime: number;
}

// ============================================
// Request/Response Types
// ============================================

/**
 * Login credentials
 */
export interface LoginRequest {
    username: string;
    password: string;
}

/**
 * Registration request data
 */
export interface RegisterRequest {
    username: string;
    password: string;
    email: string;
    firstName: string;
    lastName: string;
}

/**
 * Handoff request to transfer task
 */
export interface HandoffRequest {
    toUserId: string;
    reason: string;
}

/**
 * Escalation options for a task
 */
export interface EscalationOptions {
    levels: string[];
    canEscalate: boolean;
    canDeEscalate: boolean;
    currentLevel: string;
    escalateTo: string[];
    deEscalateTo: string[];
}

/**
 * Request to escalate/de-escalate a task
 */
export interface EscalationRequest {
    reason: string;
    targetLevel?: string;
    targetUserId?: string;
}

// ============================================
// API Response Types
// ============================================

/**
 * Generic API error response
 */
export interface ApiErrorResponse {
    timestamp: string;
    status: number;
    error: string;
    message: string;
    path: string;
    traceId?: string;
    fieldErrors?: Record<string, string>;
}

/**
 * Generic success response with message
 */
export interface ApiSuccessResponse {
    message: string;
}

/**
 * Process start response
 */
export interface ProcessStartResponse {
    message: string;
    processInstance?: ProcessInstance;
}
