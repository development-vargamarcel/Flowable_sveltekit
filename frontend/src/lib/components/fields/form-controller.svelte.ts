import type {
  FormField,
  FormGrid,
  FieldConditionRule,
  ComputedFieldState,
  ComputedGridState
} from '$lib/types';
import type { UserContext, GridContext } from '$lib/utils/expression-evaluator';
import { ConditionStateComputer } from '$lib/utils/condition-state-computer';
import { createSafeEvaluator, SafeExpressionEvaluator } from '$lib/utils/expression-evaluator';

export interface FormControllerOptions {
  fields: FormField[];
  grids: FormGrid[];
  initialValues: Record<string, unknown>;
  readonly: boolean;
  conditionRules: FieldConditionRule[];
  taskConditionRules: FieldConditionRule[];
  processVariables: Record<string, unknown>;
  userContext: UserContext;
  task?: { id: string; name: string; taskDefinitionKey: string };
  onValuesChange?: (values: Record<string, unknown>) => void;
}

interface GridComponent {
  validate(): boolean;
  getData(): any[];
}

export class FormController {
  // State
  formValues = $state<Record<string, unknown>>({});
  fieldErrors = $state<Record<string, string>>({});
  gridSelections = $state<Record<string, Record<string, unknown>[]>>({});
  formInitialized = $state(false);
  userHasMadeChanges = $state(false);

  // Config getter
  private getOptions: () => FormControllerOptions;

  constructor(getOptions: () => FormControllerOptions) {
    this.getOptions = getOptions;
    const options = getOptions();
    this.formValues = { ...options.initialValues };
  }

  private get options() {
    return this.getOptions();
  }

  // Derived Contexts
  get gridsContext() {
    return Object.fromEntries(
      this.options.grids.map((g) => [
        g.name,
        {
          rows: this.tryParseJson(this.formValues[g.name]),
          selectedRows: this.gridSelections[g.name] || [],
          selectedRow: (this.gridSelections[g.name] || [])[0] || null,
          sum: (col: string) => {
            const rows = this.tryParseJson(this.formValues[g.name]);
            return rows.reduce((sum, row) => sum + (Number(row[col]) || 0), 0);
          }
        }
      ])
    );
  }

  // Computed States
  get computedStates() {
    const context = {
      form: this.formValues,
      process: this.options.processVariables,
      user: this.options.userContext
    };
    const stateComputer = new ConditionStateComputer(context, {
      formReadonly: this.options.readonly
    });
    return stateComputer.computeFormState(
      this.options.fields,
      this.options.grids,
      this.options.conditionRules,
      this.options.taskConditionRules
    );
  }

  // Dependency Mapping
  get dependencyMap() {
    const map: Record<string, (FormField | FormGrid)[]> = {};
    for (const field of this.options.fields) {
      const expressions = [
        field.visibilityExpression,
        field.calculationExpression,
        field.validationExpression,
        field.hiddenExpression,
        field.readonlyExpression,
        field.requiredExpression
      ].filter(Boolean) as string[];

      for (const expression of expressions) {
        const deps = this.parseDependencies(expression);
        for (const dep of deps) {
          if (!map[dep]) map[dep] = [];
          map[dep].push(field);
        }
      }
    }
    for (const grid of this.options.grids) {
      if (grid.visibilityExpression) {
        const deps = this.parseDependencies(grid.visibilityExpression);
        for (const dep of deps) {
          if (!map[dep]) map[dep] = [];
          map[dep].push(grid);
        }
      }
    }
    return map;
  }

  private parseDependencies(expression: string): string[] {
    if (!expression) return [];
    const regex = /form\.([a-zA-Z0-9_]+)|grids\.([a-zA-Z0-9_]+)/g;
    const matches = [...expression.matchAll(regex)];
    const deps = new Set<string>();
    for (const match of matches) {
      if (match[1]) deps.add(match[1]);
      if (match[2]) deps.add(match[2]);
    }
    return Array.from(deps);
  }

  // Logic Execution
  private createContextEvaluator(): SafeExpressionEvaluator {
    return createSafeEvaluator({
      form: this.formValues,
      process: this.options.processVariables,
      user: this.options.userContext,
      grids: this.gridsContext as unknown as Record<string, GridContext>,
      task: this.options.task
    });
  }

  async executeFieldLogic(field: FormField) {
    if (field.calculationExpression) {
      const evaluator = this.createContextEvaluator();
      evaluator.updateExtendedContext({ value: this.formValues[field.name] });
      const result = evaluator.evaluateCalculation(field.calculationExpression);
      if (result !== undefined && this.formValues[field.name] !== result) {
        this.handleFieldChange(field.name, result, true);
      }
    }
  }

  // Visibility
  evaluateVisibility(field: FormField): boolean {
    if (!field.visibilityExpression) return !field.hidden;
    const evaluator = this.createContextEvaluator();
    evaluator.updateExtendedContext({ value: this.formValues[field.name] });
    return evaluator.evaluateVisibility(field.visibilityExpression);
  }

  evaluateGridVisibility(grid: FormGrid): boolean {
    if (!grid.visibilityExpression) return true;
    const evaluator = this.createContextEvaluator();
    return evaluator.evaluateVisibility(grid.visibilityExpression);
  }

  // Handlers
  handleFieldChange(fieldName: string, value: unknown, isAutomated = false) {
    this.formValues = { ...this.formValues, [fieldName]: value };
    this.userHasMadeChanges = true;
    if (this.fieldErrors[fieldName]) {
      const { [fieldName]: _, ...rest } = this.fieldErrors;
      this.fieldErrors = rest;
    }

    if (!isAutomated) {
      const dependents = this.dependencyMap[fieldName] || [];
      for (const dep of dependents) {
        if ('calculationExpression' in dep) {
          setTimeout(() => this.executeFieldLogic(dep), 0);
        }
      }
    }
  }

  handleGridChange(gridName: string, data: Record<string, unknown>[]) {
    this.formValues = { ...this.formValues, [gridName]: data };
    this.userHasMadeChanges = true;
  }

  handleGridSelectionChange(gridName: string, selectedRows: Record<string, unknown>[]) {
    this.gridSelections = { ...this.gridSelections, [gridName]: selectedRows };
  }

  // State Accessors
  getFieldState(field: FormField): ComputedFieldState {
    let state = this.computedStates.fields[field.name] || {
      isHidden: !!field.hidden,
      isReadonly: field.readonly || this.options.readonly,
      appliedRules: []
    };

    if (state.appliedRules.length === 0 && field.visibilityExpression) {
      const isVisible = this.evaluateVisibility(field);
      state = { ...state, isHidden: !isVisible };
    } else if (field.visibilityExpression) {
      const baseVisible = this.evaluateVisibility(field);
      if (!state.appliedRules.some((r) => r.includes('hide') || r.includes('show'))) {
        state.isHidden = !baseVisible;
      }
    }
    return state;
  }

  getGridState(grid: FormGrid): ComputedGridState {
    const state = this.computedStates.grids[grid.name] || {
      isHidden: false,
      isReadonly: this.options.readonly,
      columnStates: {},
      appliedRules: []
    };

    if (grid.visibilityExpression) {
      const isVisible = this.evaluateGridVisibility(grid);
      if (!state.appliedRules.some((r) => r.includes('hide'))) {
        state.isHidden = !isVisible;
      }
    }
    return state;
  }

  // Initialization
  initialize() {
    if (this.formInitialized || this.userHasMadeChanges) return;

    const newValues = { ...this.formValues };
    for (const field of this.options.fields) {
      if (field.hidden) continue;
      const key = field.name;
      if (newValues[key] === undefined || newValues[key] === null || newValues[key] === '') {
        if (field.defaultValue) {
          newValues[key] = field.defaultValue;
        } else if (field.type === 'checkbox') {
          newValues[key] = false;
        }
      }
    }

    this.formValues = newValues;
    this.formInitialized = true;
  }

  // Validation
  private validateFieldValue(field: FormField, value: unknown): string | null {
    if (field.required && (value === undefined || value === null || value === '')) {
      return `${field.label} is required`;
    }

    if (value === undefined || value === null || value === '') {
      return null;
    }

    if (field.validationExpression) {
      const evaluator = this.createContextEvaluator();
      evaluator.updateExtendedContext({ value: value });
      const isValidOrMsg = evaluator.evaluateValidation(field.validationExpression);
      if (isValidOrMsg === false) return field.validationMessage || `${field.label} is invalid`;
      if (typeof isValidOrMsg === 'string') return isValidOrMsg;
    }

    const validation = field.validation;
    if (!validation) return null;

    const strValue = String(value);
    if (validation.minLength && strValue.length < validation.minLength)
      return `${field.label} must be at least ${validation.minLength} characters`;
    if (validation.maxLength && strValue.length > validation.maxLength)
      return `${field.label} must not exceed ${validation.maxLength} characters`;

    if (validation.pattern) {
      try {
        const regex = new RegExp(validation.pattern);
        if (!regex.test(strValue))
          return validation.patternMessage || `${field.label} format is invalid`;
      } catch {
        /* ignore */
      }
    }

    if (field.type === 'number' || field.type === 'currency' || field.type === 'percentage') {
      const numValue = Number(value);
      if (!isNaN(numValue)) {
        if (validation.min !== undefined && numValue < validation.min)
          return `${field.label} must be at least ${validation.min}`;
        if (validation.max !== undefined && numValue > validation.max)
          return `${field.label} must not exceed ${validation.max}`;
      }
    }

    return null;
  }

  validate(gridRefs: Record<string, unknown>): boolean {
    let isValid = true;
    const newErrors: Record<string, string> = {};

    for (const field of this.options.fields) {
      const fieldState = this.getFieldState(field);
      if (fieldState.isHidden) continue;
      const value = this.formValues[field.name];
      const error = this.validateFieldValue(field, value);
      if (error) {
        newErrors[field.name] = error;
        isValid = false;
      }
    }

    for (const grid of this.options.grids) {
      const gridState = this.getGridState(grid);
      if (gridState.isHidden) continue;
      const gridRef = gridRefs[grid.name] as GridComponent;
      if (gridRef && !gridRef.validate()) isValid = false;
    }

    this.fieldErrors = newErrors;
    return isValid;
  }

  // Public API
  getValues(gridRefs: Record<string, unknown>): Record<string, unknown> {
    const result = { ...this.formValues };
    for (const grid of this.options.grids) {
      const gridRef = gridRefs[grid.name] as GridComponent;
      if (gridRef) result[grid.name] = gridRef.getData();
    }
    return result;
  }

  reset() {
    this.formValues = { ...this.options.initialValues };
    this.fieldErrors = {};
    this.userHasMadeChanges = false;
    this.formInitialized = false;
  }

  getSortedItems(): Array<{ type: 'field' | 'grid'; item: FormField | FormGrid }> {
    const items: Array<{
      type: 'field' | 'grid';
      item: FormField | FormGrid;
      row: number;
      col: number;
    }> = [];

    for (const field of this.options.fields) {
      const fieldState = this.getFieldState(field);
      if (!fieldState.isHidden)
        items.push({ type: 'field', item: field, row: field.gridRow, col: field.gridColumn });
    }

    for (const grid of this.options.grids) {
      const gridState = this.getGridState(grid);
      if (!gridState.isHidden)
        items.push({ type: 'grid', item: grid, row: grid.gridRow, col: grid.gridColumn });
    }

    items.sort((a, b) => {
      if (a.row !== b.row) return a.row - b.row;
      return a.col - b.col;
    });

    return items.map(({ type, item }) => ({ type, item }));
  }

  private tryParseJson(value: unknown): Record<string, unknown>[] {
    if (!value) return [];
    if (Array.isArray(value)) return value;
    if (typeof value === 'string') {
      try {
        const parsed = JSON.parse(value);
        if (Array.isArray(parsed)) return parsed;
      } catch {
        /* ignore */
      }
    }
    return [];
  }
}
