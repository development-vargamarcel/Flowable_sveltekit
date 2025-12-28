/**
 * BPMN Designer utilities and types
 * Extracted from the designer page to improve maintainability
 */

// ============= Types =============

export interface ElementProperties {
  id: string;
  name: string;
  type: string;
  assignee: string;
  candidateGroups: string;
  candidateUsers: string;
  formKey: string;
  documentation: string;
  scriptFormat: string;
  script: string;
  conditionExpression: string;
  dueDate: string;
  priority: string;
  category: string;
  asyncBefore: boolean;
  asyncAfter: boolean;
  exclusive: boolean;
  skipExpression: string;
  implementation: string;
  expression: string;
  delegateExpression: string;
  resultVariable: string;
  class: string;
  multiInstanceType: string;
  loopCardinality: string;
  collection: string;
  elementVariable: string;
  completionCondition: string;
}

export interface FormField {
  id: string;
  name: string;
  label: string;
  type: FormFieldType;
  required: boolean;
  validation: FieldValidation;
  options: Array<{ value: string; label: string }>;
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

export interface FieldValidation {
  minLength?: number;
  maxLength?: number;
  min?: number;
  max?: number;
  pattern?: string;
  patternMessage?: string;
  customExpression?: string;
  customMessage?: string;
}

export interface GridColumn {
  id: string;
  name: string;
  label: string;
  type: 'text' | 'number' | 'date' | 'select' | 'textarea';
  required: boolean;
  placeholder: string;
  options: string[];
  min?: number;
  max?: number;
  step?: number;
  validation: FieldValidation;
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

export type FormFieldType =
  | 'text'
  | 'number'
  | 'date'
  | 'datetime'
  | 'select'
  | 'multiselect'
  | 'textarea'
  | 'checkbox'
  | 'radio'
  | 'file'
  | 'email'
  | 'phone'
  | 'currency'
  | 'percentage'
  | 'expression';

// ============= Constants =============

export const DEFAULT_ELEMENT_PROPERTIES: ElementProperties = {
  id: '',
  name: '',
  type: '',
  assignee: '',
  candidateGroups: '',
  candidateUsers: '',
  formKey: '',
  documentation: '',
  scriptFormat: 'javascript',
  script: '',
  conditionExpression: '',
  dueDate: '',
  priority: '',
  category: '',
  asyncBefore: false,
  asyncAfter: false,
  exclusive: true,
  skipExpression: '',
  implementation: 'class',
  expression: '',
  delegateExpression: '',
  resultVariable: '',
  class: '',
  multiInstanceType: 'none',
  loopCardinality: '',
  collection: '',
  elementVariable: '',
  completionCondition: ''
};

export const FIELD_TYPE_OPTIONS: Array<{ value: FormFieldType; label: string }> = [
  { value: 'text', label: 'Text' },
  { value: 'number', label: 'Number' },
  { value: 'date', label: 'Date' },
  { value: 'datetime', label: 'Date & Time' },
  { value: 'select', label: 'Dropdown' },
  { value: 'multiselect', label: 'Multi-Select' },
  { value: 'textarea', label: 'Text Area' },
  { value: 'checkbox', label: 'Checkbox' },
  { value: 'radio', label: 'Radio Buttons' },
  { value: 'file', label: 'File Upload' },
  { value: 'email', label: 'Email' },
  { value: 'phone', label: 'Phone' },
  { value: 'currency', label: 'Currency' },
  { value: 'percentage', label: 'Percentage' },
  { value: 'expression', label: 'Expression (Read-only)' }
];

export const GRID_COLUMN_TYPE_OPTIONS: Array<{ value: GridColumn['type']; label: string }> = [
  { value: 'text', label: 'Text' },
  { value: 'number', label: 'Number' },
  { value: 'date', label: 'Date' },
  { value: 'select', label: 'Dropdown' },
  { value: 'textarea', label: 'Text Area' }
];

export const ELEMENT_TYPE_LABELS: Record<string, string> = {
  'bpmn:StartEvent': 'Start Event',
  'bpmn:EndEvent': 'End Event',
  'bpmn:UserTask': 'User Task',
  'bpmn:ServiceTask': 'Service Task',
  'bpmn:ScriptTask': 'Script Task',
  'bpmn:ExclusiveGateway': 'Exclusive Gateway (XOR)',
  'bpmn:ParallelGateway': 'Parallel Gateway (AND)',
  'bpmn:InclusiveGateway': 'Inclusive Gateway (OR)',
  'bpmn:SequenceFlow': 'Sequence Flow',
  'bpmn:Process': 'Process',
  'bpmn:SubProcess': 'Sub Process',
  'bpmn:CallActivity': 'Call Activity',
  'bpmn:BoundaryEvent': 'Boundary Event',
  'bpmn:IntermediateCatchEvent': 'Intermediate Catch Event',
  'bpmn:IntermediateThrowEvent': 'Intermediate Throw Event',
  'bpmn:Task': 'Task',
  'bpmn:SendTask': 'Send Task',
  'bpmn:ReceiveTask': 'Receive Task',
  'bpmn:ManualTask': 'Manual Task',
  'bpmn:BusinessRuleTask': 'Business Rule Task'
};

export const ELEMENT_TYPE_ICONS: Record<string, string> = {
  'bpmn:StartEvent': '▶',
  'bpmn:EndEvent': '⬛',
  'bpmn:UserTask': '👤',
  'bpmn:ServiceTask': '⚙️',
  'bpmn:ScriptTask': '📜',
  'bpmn:ExclusiveGateway': '◇',
  'bpmn:ParallelGateway': '⊕',
  'bpmn:InclusiveGateway': '◎',
  'bpmn:SequenceFlow': '→',
  'bpmn:SubProcess': '▣',
  'bpmn:CallActivity': '↗'
};

// Default BPMN template
export const DEFAULT_BPMN = `<?xml version="1.0" encoding="UTF-8"?>
<bpmn:definitions xmlns:bpmn="http://www.omg.org/spec/BPMN/20100524/MODEL"
                  xmlns:bpmndi="http://www.omg.org/spec/BPMN/20100524/DI"
                  xmlns:dc="http://www.omg.org/spec/DD/20100524/DC"
                  xmlns:di="http://www.omg.org/spec/DD/20100524/DI"
                  xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                  xmlns:flowable="http://flowable.org/bpmn"
                  id="Definitions_1"
                  targetNamespace="http://bpmn.io/schema/bpmn">
  <bpmn:process id="process_1" name="New Process" isExecutable="true">
    <bpmn:startEvent id="startEvent" name="Start">
      <bpmn:outgoing>Flow_1</bpmn:outgoing>
    </bpmn:startEvent>
    <bpmn:userTask id="userTask1" name="Review Task" flowable:assignee="\${initiator}">
      <bpmn:incoming>Flow_1</bpmn:incoming>
      <bpmn:outgoing>Flow_2</bpmn:outgoing>
    </bpmn:userTask>
    <bpmn:endEvent id="endEvent" name="End">
      <bpmn:incoming>Flow_2</bpmn:incoming>
    </bpmn:endEvent>
    <bpmn:sequenceFlow id="Flow_1" sourceRef="startEvent" targetRef="userTask1"/>
    <bpmn:sequenceFlow id="Flow_2" sourceRef="userTask1" targetRef="endEvent"/>
  </bpmn:process>
  <bpmndi:BPMNDiagram id="BPMNDiagram_1">
    <bpmndi:BPMNPlane id="BPMNPlane_1" bpmnElement="process_1">
      <bpmndi:BPMNShape id="startEvent_di" bpmnElement="startEvent">
        <dc:Bounds x="152" y="102" width="36" height="36"/>
        <bpmndi:BPMNLabel>
          <dc:Bounds x="158" y="145" width="24" height="14"/>
        </bpmndi:BPMNLabel>
      </bpmndi:BPMNShape>
      <bpmndi:BPMNShape id="userTask1_di" bpmnElement="userTask1">
        <dc:Bounds x="240" y="80" width="100" height="80"/>
      </bpmndi:BPMNShape>
      <bpmndi:BPMNShape id="endEvent_di" bpmnElement="endEvent">
        <dc:Bounds x="392" y="102" width="36" height="36"/>
        <bpmndi:BPMNLabel>
          <dc:Bounds x="400" y="145" width="20" height="14"/>
        </bpmndi:BPMNLabel>
      </bpmndi:BPMNShape>
      <bpmndi:BPMNEdge id="Flow_1_di" bpmnElement="Flow_1">
        <di:waypoint x="188" y="120"/>
        <di:waypoint x="240" y="120"/>
      </bpmndi:BPMNEdge>
      <bpmndi:BPMNEdge id="Flow_2_di" bpmnElement="Flow_2">
        <di:waypoint x="340" y="120"/>
        <di:waypoint x="392" y="120"/>
      </bpmndi:BPMNEdge>
    </bpmndi:BPMNPlane>
  </bpmndi:BPMNDiagram>
</bpmn:definitions>`;

// ============= Helper Functions =============

/**
 * Get the display label for a BPMN element type
 */
export function getElementTypeLabel(type: string): string {
  return ELEMENT_TYPE_LABELS[type] || type.replace('bpmn:', '');
}

/**
 * Get the icon for a BPMN element type
 */
export function getElementTypeIcon(type: string): string {
  return ELEMENT_TYPE_ICONS[type] || '○';
}

/**
 * Create a new empty form field
 */
export function createFormField(index: number): FormField {
  return {
    id: `field_${Date.now()}`,
    name: '',
    label: '',
    type: 'text',
    required: false,
    validation: {},
    options: [],
    placeholder: '',
    defaultValue: '',
    defaultExpression: '',
    tooltip: '',
    readonly: false,
    hidden: false,
    hiddenExpression: '',
    readonlyExpression: '',
    requiredExpression: '',
    gridColumn: 1,
    gridRow: index + 1,
    gridWidth: 1,
    cssClass: '',
    onChange: '',
    onBlur: ''
  };
}

/**
 * Create a new empty grid
 */
export function createFormGrid(index: number, gridColumns: number): FormGrid {
  return {
    id: `grid_${Date.now()}`,
    name: '',
    label: '',
    description: '',
    minRows: 0,
    maxRows: 0,
    columns: [],
    gridColumn: 1,
    gridRow: index + 1,
    gridWidth: gridColumns,
    cssClass: ''
  };
}

/**
 * Create a new grid column
 */
export function createGridColumn(): GridColumn {
  return {
    id: `col_${Date.now()}`,
    name: '',
    label: '',
    type: 'text',
    required: false,
    placeholder: '',
    options: [],
    validation: {}
  };
}

/**
 * Validate script syntax (basic)
 */
export function validateScript(code: string, format: string): string | null {
  if (!code.trim()) return null;

  if (format === 'javascript') {
    try {
      if (code.includes('eval(')) {
        return 'Warning: eval() is not recommended for security reasons';
      }

      const brackets: Record<string, number> = { '(': 0, '[': 0, '{': 0 };
      for (const char of code) {
        if (char === '(') brackets['(']++;
        if (char === ')') brackets['(']--;
        if (char === '[') brackets['[']++;
        if (char === ']') brackets['[']--;
        if (char === '{') brackets['{']++;
        if (char === '}') brackets['{']--;
      }

      if (brackets['('] !== 0) return 'Unmatched parentheses';
      if (brackets['['] !== 0) return 'Unmatched square brackets';
      if (brackets['{'] !== 0) return 'Unmatched curly braces';
    } catch (e) {
      return e instanceof Error ? e.message : 'Syntax error';
    }
  }

  return null;
}

/**
 * Validate form fields configuration
 */
export function validateFormFields(fields: FormField[]): string[] {
  const errors: string[] = [];
  const names = new Set<string>();

  fields.forEach((field, index) => {
    if (!field.name) {
      errors.push(`Field ${index + 1}: Name is required`);
    } else if (!/^[a-zA-Z_][a-zA-Z0-9_]*$/.test(field.name)) {
      errors.push(`Field ${index + 1}: Name must be a valid identifier`);
    } else if (names.has(field.name)) {
      errors.push(`Field ${index + 1}: Duplicate name '${field.name}'`);
    } else {
      names.add(field.name);
    }

    if (!field.label) {
      errors.push(`Field ${index + 1}: Label is required`);
    }

    if (
      (field.type === 'select' || field.type === 'multiselect' || field.type === 'radio') &&
      field.options.length === 0
    ) {
      errors.push(`Field ${index + 1}: Options are required for ${field.type} type`);
    }
  });

  return errors;
}

/**
 * Validate form grids configuration
 */
export function validateFormGrids(grids: FormGrid[]): string[] {
  const errors: string[] = [];
  const names = new Set<string>();

  grids.forEach((grid, gridIndex) => {
    if (!grid.name) {
      errors.push(`Grid ${gridIndex + 1}: Name is required`);
    } else if (!/^[a-zA-Z_][a-zA-Z0-9_]*$/.test(grid.name)) {
      errors.push(`Grid ${gridIndex + 1}: Name must be a valid identifier`);
    } else if (names.has(grid.name)) {
      errors.push(`Grid ${gridIndex + 1}: Duplicate name '${grid.name}'`);
    } else {
      names.add(grid.name);
    }

    if (!grid.label) {
      errors.push(`Grid ${gridIndex + 1}: Label is required`);
    }

    if (grid.columns.length === 0) {
      errors.push(`Grid ${gridIndex + 1}: At least one column is required`);
    }

    const columnNames = new Set<string>();
    grid.columns.forEach((column, colIndex) => {
      if (!column.name) {
        errors.push(`Grid ${gridIndex + 1}, Column ${colIndex + 1}: Name is required`);
      } else if (!/^[a-zA-Z_][a-zA-Z0-9_]*$/.test(column.name)) {
        errors.push(
          `Grid ${gridIndex + 1}, Column ${colIndex + 1}: Name must be a valid identifier`
        );
      } else if (columnNames.has(column.name)) {
        errors.push(
          `Grid ${gridIndex + 1}, Column ${colIndex + 1}: Duplicate column name '${column.name}'`
        );
      } else {
        columnNames.add(column.name);
      }

      if (!column.label) {
        errors.push(`Grid ${gridIndex + 1}, Column ${colIndex + 1}: Label is required`);
      }

      if (column.type === 'select' && column.options.length === 0) {
        errors.push(
          `Grid ${gridIndex + 1}, Column ${colIndex + 1}: Options are required for select type`
        );
      }
    });
  });

  return errors;
}

/**
 * Delay helper for retry logic
 */
export function delay(ms: number): Promise<void> {
  return new Promise((resolve) => setTimeout(resolve, ms));
}

/**
 * Retry function with exponential backoff
 */
export async function retryWithBackoff<T>(
  fn: () => Promise<T>,
  maxRetries: number,
  baseDelay = 1000,
  onRetry?: (attempt: number) => void
): Promise<T> {
  let lastError: Error | null = null;

  for (let i = 0; i < maxRetries; i++) {
    try {
      onRetry?.(i + 1);
      return await fn();
    } catch (err) {
      lastError = err instanceof Error ? err : new Error(String(err));
      if (i < maxRetries - 1) {
        const delayTime = baseDelay * Math.pow(2, i);
        await delay(delayTime);
      }
    }
  }

  throw lastError;
}
