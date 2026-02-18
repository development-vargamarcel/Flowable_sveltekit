/**
 * Expression Evaluator for Conditional Field Settings
 *
 * Safely evaluates expressions for visibility/readonly conditions.
 * Supports form values, process variables, and user context.
 *
 * Expression formats:
 * - Simple: ${fieldName == "value"}
 * - Comparison: ${amount > 1000}
 * - Logical: ${status == "approved" && amount > 500}
 * - User context: ${user.role == "manager"}
 * - Process vars: ${process.initiator == "admin"}
 */

export interface EvaluationContext {
  form: Record<string, unknown>; // Form field values
  process: Record<string, unknown>; // Process variables
  user: UserContext; // Current user context
}

export interface UserContext {
  id: string;
  username: string;
  roles: string[];
  groups: string[];
}

export type ExpressionResult = boolean | string | number | null;

/**
 * Parses and evaluates expressions safely without using eval()
 */
export class ExpressionEvaluator {
  private context: EvaluationContext;

  constructor(context: EvaluationContext) {
    this.context = context;
  }

  /**
   * Update the evaluation context (e.g., when form values change)
   */
  updateContext(context: Partial<EvaluationContext>): void {
    this.context = { ...this.context, ...context };
  }

  /**
   * Evaluate an expression string and return the result
   * Expressions can be in format: ${expression} or just plain expression
   */
  evaluate(expression: string): ExpressionResult {
    if (!expression || expression.trim() === '') {
      return null;
    }

    // Extract expression from ${...} wrapper if present
    let expr = expression.trim();
    if (expr.startsWith('${') && expr.endsWith('}')) {
      expr = expr.slice(2, -1).trim();
    }

    try {
      return this.evaluateExpression(expr);
    } catch (error) {
      console.warn(`Failed to evaluate expression: ${expression}`, error);
      return null;
    }
  }

  /**
   * Evaluate expression and coerce result to boolean
   */
  evaluateBoolean(expression: string): boolean {
    const result = this.evaluate(expression);
    return this.toBoolean(result);
  }

  private toBoolean(value: ExpressionResult): boolean {
    if (value === null || value === undefined) return false;
    if (typeof value === 'boolean') return value;
    if (typeof value === 'string') return value !== '' && value.toLowerCase() !== 'false';
    if (typeof value === 'number') return value !== 0;
    return Boolean(value);
  }

  private evaluateExpression(expr: string): ExpressionResult {
    // Ternary operator support enables richer validation and visibility rules
    // without falling back to unsafe dynamic code execution.
    const ternaryParts = this.splitTernary(expr);
    if (ternaryParts) {
      const condition = this.toBoolean(this.evaluateExpression(ternaryParts.condition));
      return this.evaluateExpression(condition ? ternaryParts.whenTrue : ternaryParts.whenFalse);
    }

    // Nullish coalescing provides resilient fallback behavior for optional fields.
    const nullishParts = this.splitByOperator(expr, '??');
    if (nullishParts.length === 2) {
      const primary = this.evaluateExpression(nullishParts[0].trim());
      return primary === null || primary === undefined
        ? this.evaluateExpression(nullishParts[1].trim())
        : primary;
    }

    // Handle logical OR (lowest precedence)
    const orParts = this.splitByOperator(expr, '||');
    if (orParts.length > 1) {
      return orParts.some((part) => this.toBoolean(this.evaluateExpression(part.trim())));
    }

    // Handle logical AND
    const andParts = this.splitByOperator(expr, '&&');
    if (andParts.length > 1) {
      return andParts.every((part) => this.toBoolean(this.evaluateExpression(part.trim())));
    }

    // Handle NOT operator
    if (expr.startsWith('!') && !expr.startsWith('!=')) {
      const inner = expr.slice(1).trim();
      // Handle !(expression)
      if (inner.startsWith('(') && inner.endsWith(')')) {
        return !this.toBoolean(this.evaluateExpression(inner.slice(1, -1).trim()));
      }
      return !this.toBoolean(this.evaluateExpression(inner));
    }

    // Handle parentheses
    if (expr.startsWith('(') && expr.endsWith(')')) {
      return this.evaluateExpression(expr.slice(1, -1).trim());
    }

    // Handle comparison operators
    const comparisonResult = this.evaluateComparison(expr);
    if (comparisonResult !== undefined) {
      return comparisonResult;
    }

    // Handle 'in' operator for array membership
    const inMatch = expr.match(/^(.+?)\s+in\s+\[(.+)\]$/);
    if (inMatch) {
      const value = this.evaluateExpression(inMatch[1].trim());
      const arrayStr = inMatch[2];
      const arrayValues = this.parseArrayValues(arrayStr);
      return arrayValues.some((v) => this.looseEquals(value, v));
    }

    // Handle contains for checking if array contains value
    const containsMatch = expr.match(/^(.+?)\.contains\((.+)\)$/);
    if (containsMatch) {
      const arrayValue = this.resolveValue(containsMatch[1].trim());
      const searchValue = this.evaluateExpression(containsMatch[2].trim());
      if (Array.isArray(arrayValue)) {
        return arrayValue.some((v) => this.looseEquals(v, searchValue));
      }
      return false;
    }

    const functionResult = this.evaluateFunctionCall(expr);
    if (functionResult !== undefined) {
      return functionResult;
    }

    // Resolve as a value (variable reference or literal)
    return this.resolveValue(expr) as ExpressionResult;
  }

  private evaluateComparison(expr: string): boolean | undefined {
    // Order matters - check longer operators first
    const operators = ['===', '!==', '==', '!=', '>=', '<=', '>', '<'];

    for (const op of operators) {
      const parts = this.splitByOperator(expr, op);
      if (parts.length === 2) {
        const left = this.resolveValue(parts[0].trim());
        const right = this.resolveValue(parts[1].trim());
        return this.compareValues(left, right, op);
      }
    }

    return undefined;
  }

  private compareValues(left: unknown, right: unknown, operator: string): boolean {
    switch (operator) {
      case '===':
        return left === right;
      case '!==':
        return left !== right;
      case '==':
        return this.looseEquals(left, right);
      case '!=':
        return !this.looseEquals(left, right);
      case '>':
        return this.toNumber(left) > this.toNumber(right);
      case '<':
        return this.toNumber(left) < this.toNumber(right);
      case '>=':
        return this.toNumber(left) >= this.toNumber(right);
      case '<=':
        return this.toNumber(left) <= this.toNumber(right);
      default:
        return false;
    }
  }

  private looseEquals(a: unknown, b: unknown): boolean {
    if (a === b) return true;
    if (a === null || a === undefined) return b === null || b === undefined;
    if (b === null || b === undefined) return false;

    // String comparison (case-insensitive for strings)
    const strA = String(a);
    const strB = String(b);
    if (strA.toLowerCase() === strB.toLowerCase()) return true;

    // Number comparison
    const numA = Number(a);
    const numB = Number(b);
    if (!isNaN(numA) && !isNaN(numB) && numA === numB) return true;

    return false;
  }

  private toNumber(value: unknown): number {
    if (typeof value === 'number') return value;
    if (typeof value === 'string') {
      const num = parseFloat(value);
      return isNaN(num) ? 0 : num;
    }
    return 0;
  }

  private isEmpty(value: unknown): boolean {
    if (value === null || value === undefined) return true;
    if (typeof value === 'string') return value.trim() === '';
    if (Array.isArray(value)) return value.length === 0;
    if (typeof value === 'object') return Object.keys(value).length === 0;
    return false;
  }

  private resolveValue(expr: string): unknown {
    expr = expr.trim();

    // Handle string literals
    if (
      (expr.startsWith('"') && expr.endsWith('"')) ||
      (expr.startsWith("'") && expr.endsWith("'"))
    ) {
      return expr.slice(1, -1);
    }

    // Handle number literals
    const num = parseFloat(expr);
    if (!isNaN(num) && expr === String(num)) {
      return num;
    }

    // Handle boolean literals
    if (expr === 'true') return true;
    if (expr === 'false') return false;
    if (expr === 'null') return null;

    // Handle array literals (e.g. ["admin", "user"])
    if (expr.startsWith('[') && expr.endsWith(']')) {
      return this.parseArrayValues(expr.slice(1, -1));
    }

    // Handle variable references
    return this.resolveVariable(expr);
  }

  private resolveVariable(path: string): unknown {
    const parts = path.split('.');

    // Check for context prefixes
    if (parts[0] === 'form') {
      return this.getNestedValue(this.context.form, parts.slice(1));
    }
    if (parts[0] === 'process') {
      return this.getNestedValue(this.context.process, parts.slice(1));
    }
    if (parts[0] === 'user') {
      return this.getNestedValue(
        this.context.user as unknown as Record<string, unknown>,
        parts.slice(1)
      );
    }

    // Default: try form first, then process variables
    let value = this.getNestedValue(this.context.form, parts);
    if (value === undefined) {
      value = this.getNestedValue(this.context.process, parts);
    }

    // Safe evaluator contexts can include additional top-level values
    // such as `value`, `task`, or future metadata objects used by
    // validation and visibility expressions. This fallback keeps
    // expression support extensible without introducing eval-like behavior.
    if (value === undefined) {
      value = this.getNestedValue(this.context as unknown as Record<string, unknown>, parts);
    }

    return value;
  }

  private getNestedValue(obj: Record<string, unknown> | undefined, parts: string[]): unknown {
    if (!obj || parts.length === 0) return obj;

    let current: unknown = obj;
    for (const part of parts) {
      if (current === null || current === undefined) return undefined;
      if (typeof current !== 'object') return undefined;
      current = (current as Record<string, unknown>)[part];
    }
    return current;
  }

  private splitByOperator(expr: string, operator: string): string[] {
    const result: string[] = [];
    let current = '';
    let depth = 0;
    let inString = false;
    let stringChar = '';

    for (let i = 0; i < expr.length; i++) {
      const char = expr[i];

      // Handle string literals
      if ((char === '"' || char === "'") && expr[i - 1] !== '\\') {
        if (!inString) {
          inString = true;
          stringChar = char;
        } else if (char === stringChar) {
          inString = false;
        }
      }

      // Handle parentheses depth
      if (!inString) {
        if (char === '(' || char === '[') depth++;
        if (char === ')' || char === ']') depth--;
      }

      // Check for operator at depth 0
      if (depth === 0 && !inString && expr.slice(i, i + operator.length) === operator) {
        result.push(current);
        current = '';
        i += operator.length - 1;
        continue;
      }

      current += char;
    }

    result.push(current);
    return result.length > 1 ? result : [expr];
  }

  private parseArrayValues(str: string): unknown[] {
    const values: unknown[] = [];
    let current = '';
    let inString = false;
    let stringChar = '';

    for (let i = 0; i < str.length; i++) {
      const char = str[i];

      if ((char === '"' || char === "'") && str[i - 1] !== '\\') {
        if (!inString) {
          inString = true;
          stringChar = char;
        } else if (char === stringChar) {
          inString = false;
        }
        current += char;
      } else if (char === ',' && !inString) {
        const trimmed = current.trim();
        if (trimmed) {
          values.push(this.resolveValue(trimmed));
        }
        current = '';
      } else {
        current += char;
      }
    }

    const trimmed = current.trim();
    if (trimmed) {
      values.push(this.resolveValue(trimmed));
    }

    return values;
  }

  private parseStringLiteral(str: string): string {
    str = str.trim();
    if ((str.startsWith('"') && str.endsWith('"')) || (str.startsWith("'") && str.endsWith("'"))) {
      return str.slice(1, -1);
    }
    return str;
  }

  /**
   * Parse and execute helper-style function expressions.
   *
   * This central parser avoids brittle regex matching for every helper
   * and makes nested function calls and comma-containing string arguments
   * safe to support.
   */
  private evaluateFunctionCall(expr: string): ExpressionResult | undefined {
    const parsed = this.parseFunctionCall(expr);
    if (!parsed) return undefined;

    const { name, args } = parsed;
    const values = args.map((arg) => this.evaluateExpression(arg));

    switch (name) {
      case 'hasRole':
        return this.context.user.roles.includes(String(values[0] ?? ''));
      case 'hasGroup':
        return this.context.user.groups.includes(String(values[0] ?? ''));
      case 'hasAnyRole':
        return this.normalizeRoleGroupArgs(values).some((role) =>
          this.context.user.roles.includes(role)
        );
      case 'hasAnyGroup':
        return this.normalizeRoleGroupArgs(values).some((group) =>
          this.context.user.groups.includes(group)
        );
      case 'hasAllRoles':
        return this.normalizeRoleGroupArgs(values).every((role) =>
          this.context.user.roles.includes(role)
        );
      case 'hasAllGroups':
        return this.normalizeRoleGroupArgs(values).every((group) =>
          this.context.user.groups.includes(group)
        );
      case 'isEmpty':
        return this.isEmpty(values[0]);
      case 'isNotEmpty':
        return !this.isEmpty(values[0]);
      case 'startsWith':
        return String(values[0] ?? '').startsWith(String(values[1] ?? ''));
      case 'endsWith':
        return String(values[0] ?? '').endsWith(String(values[1] ?? ''));
      case 'includes': {
        const container = values[0];
        const needle = values[1];
        if (Array.isArray(container)) {
          return container.some((item) => this.looseEquals(item, needle));
        }
        return String(container ?? '').includes(String(needle ?? ''));
      }
      case 'len':
        return this.getLength(values[0]);
      case 'lower':
        return String(values[0] ?? '').toLowerCase();
      case 'upper':
        return String(values[0] ?? '').toUpperCase();
      case 'trim':
        return String(values[0] ?? '').trim();
      case 'between': {
        const value = this.toNumber(values[0]);
        const min = this.toNumber(values[1]);
        const max = this.toNumber(values[2]);
        return value >= min && value <= max;
      }
      case 'matches': {
        const value = String(values[0] ?? '');
        const pattern = String(values[1] ?? '');
        const flags = String(values[2] ?? '');
        try {
          return new RegExp(pattern, flags).test(value);
        } catch {
          return false;
        }
      }
      case 'coalesce':
        return values.find((value) => value !== null && value !== undefined) ?? null;
      case 'defaultIfBlank': {
        const current = values[0];
        return typeof current === 'string' && current.trim() === '' ? (values[1] ?? null) : current;
      }
      case 'min':
        return Math.min(...values.map((value) => this.toNumber(value)));
      case 'max':
        return Math.max(...values.map((value) => this.toNumber(value)));
      case 'abs':
        return Math.abs(this.toNumber(values[0]));
      case 'round': {
        const precision = Math.max(0, Math.floor(this.toNumber(values[1])));
        const factor = 10 ** precision;
        return Math.round(this.toNumber(values[0]) * factor) / factor;
      }
      case 'ceil':
        return Math.ceil(this.toNumber(values[0]));
      case 'floor':
        return Math.floor(this.toNumber(values[0]));
      case 'concat':
        return values.map((value) => String(value ?? '')).join('');
      case 'replace':
        return String(values[0] ?? '').replace(String(values[1] ?? ''), String(values[2] ?? ''));
      case 'substring': {
        const text = String(values[0] ?? '');
        const start = Math.max(0, Math.floor(this.toNumber(values[1])));
        const endArg = values[2];
        return endArg === undefined
          ? text.substring(start)
          : text.substring(start, Math.floor(this.toNumber(endArg)));
      }
      case 'at': {
        const collection = values[0];
        const index = Math.floor(this.toNumber(values[1]));
        if (Array.isArray(collection) || typeof collection === 'string') {
          return collection.at(index) ?? null;
        }
        return null;
      }
      default:
        return undefined;
    }
  }

  private parseFunctionCall(expr: string): { name: string; args: string[] } | null {
    const openParen = expr.indexOf('(');
    if (openParen <= 0 || !expr.endsWith(')')) {
      return null;
    }

    const name = expr.slice(0, openParen).trim();
    if (!/^[a-zA-Z_][a-zA-Z0-9_]*$/.test(name)) {
      return null;
    }

    const argsString = expr.slice(openParen + 1, -1).trim();
    return {
      name,
      args: this.splitArguments(argsString)
    };
  }

  private splitArguments(argsString: string): string[] {
    if (!argsString) return [];

    const args: string[] = [];
    let current = '';
    let depth = 0;
    let inString = false;
    let stringChar = '';

    for (let i = 0; i < argsString.length; i++) {
      const char = argsString[i];
      if ((char === '"' || char === "'") && argsString[i - 1] !== '\\') {
        if (!inString) {
          inString = true;
          stringChar = char;
        } else if (char === stringChar) {
          inString = false;
        }
      }

      if (!inString) {
        if (char === '(' || char === '[') depth++;
        if (char === ')' || char === ']') depth--;
      }

      if (char === ',' && depth === 0 && !inString) {
        args.push(current.trim());
        current = '';
        continue;
      }

      current += char;
    }

    if (current.trim() !== '') {
      args.push(current.trim());
    }

    return args;
  }

  private normalizeRoleGroupArgs(values: unknown[]): string[] {
    if (values.length === 1 && Array.isArray(values[0])) {
      return values[0].map((value) => String(value));
    }
    return values.map((value) => String(value));
  }

  private getLength(value: unknown): number {
    if (typeof value === 'string' || Array.isArray(value)) {
      return value.length;
    }
    if (value && typeof value === 'object') {
      return Object.keys(value).length;
    }
    return 0;
  }

  private splitTernary(
    expr: string
  ): { condition: string; whenTrue: string; whenFalse: string } | null {
    let depth = 0;
    let inString = false;
    let stringChar = '';
    let questionIndex = -1;
    let nestedTernary = 0;

    for (let i = 0; i < expr.length; i++) {
      const char = expr[i];
      if ((char === '"' || char === "'") && expr[i - 1] !== '\\') {
        if (!inString) {
          inString = true;
          stringChar = char;
        } else if (char === stringChar) {
          inString = false;
        }
      }
      if (inString) continue;

      if (char === '(' || char === '[') depth++;
      if (char === ')' || char === ']') depth--;

      if (depth !== 0) continue;

      if (char === '?' && questionIndex === -1) {
        questionIndex = i;
      } else if (char === '?' && questionIndex !== -1) {
        nestedTernary++;
      } else if (char === ':' && questionIndex !== -1) {
        if (nestedTernary > 0) {
          nestedTernary--;
          continue;
        }
        return {
          condition: expr.slice(0, questionIndex).trim(),
          whenTrue: expr.slice(questionIndex + 1, i).trim(),
          whenFalse: expr.slice(i + 1).trim()
        };
      }
    }

    return null;
  }

  /**
   * Evaluate arithmetic expressions safely
   * Supports: +, -, *, /, %, ()
   */
  evaluateArithmetic(expr: string): number {
    try {
      if (!this.isValidArithmeticExpression(expr)) {
        return 0;
      }

      // First resolve all variables in the expression
      const resolvedExpr = this.resolveVariablesInExpression(expr);
      return this.parseArithmeticExpression(resolvedExpr);
    } catch (error) {
      console.warn(`Failed to evaluate arithmetic expression: ${expr}`, error);
      return 0;
    }
  }

  private isValidArithmeticExpression(expr: string): boolean {
    const trimmed = expr.trim();
    if (trimmed === '') return false;
    if (/[^0-9a-zA-Z_.\s+\-*/%()]/.test(trimmed)) return false;
    if (/[*+/%-]\s*$/.test(trimmed)) return false;
    if (/^[*+/%]/.test(trimmed)) return false;
    if (/\([^()]*$/.test(trimmed)) return false;
    return true;
  }

  private resolveVariablesInExpression(expr: string): string {
    // Replace variable references with their values
    // Match patterns like form.fieldName, process.var, or just fieldName
    return expr.replace(/[a-zA-Z_][a-zA-Z0-9_.]*(?![a-zA-Z0-9_(])/g, (match) => {
      // Skip keywords
      if (['true', 'false', 'null', 'undefined'].includes(match)) {
        return match;
      }
      const value = this.resolveVariable(match);
      if (typeof value === 'number') return String(value);
      if (typeof value === 'string') {
        const num = parseFloat(value);
        return isNaN(num) ? '0' : String(num);
      }
      return '0';
    });
  }

  private parseArithmeticExpression(expr: string): number {
    expr = expr.trim();

    // Handle addition/subtraction (lowest precedence)
    let depth = 0;
    for (let i = expr.length - 1; i >= 0; i--) {
      const char = expr[i];
      if (char === ')') depth++;
      if (char === '(') depth--;
      if (depth === 0 && (char === '+' || char === '-') && i > 0) {
        const left = this.parseArithmeticExpression(expr.slice(0, i));
        const right = this.parseArithmeticExpression(expr.slice(i + 1));
        return char === '+' ? left + right : left - right;
      }
    }

    // Handle multiplication/division/modulo
    depth = 0;
    for (let i = expr.length - 1; i >= 0; i--) {
      const char = expr[i];
      if (char === ')') depth++;
      if (char === '(') depth--;
      if (depth === 0 && (char === '*' || char === '/' || char === '%')) {
        const left = this.parseArithmeticExpression(expr.slice(0, i));
        const right = this.parseArithmeticExpression(expr.slice(i + 1));
        if (char === '*') return left * right;
        if (char === '/') return left / right;
        return right !== 0 ? left % right : 0;
      }
    }

    // Handle parentheses
    if (expr.startsWith('(') && expr.endsWith(')')) {
      return this.parseArithmeticExpression(expr.slice(1, -1));
    }

    // Handle unary minus
    if (expr.startsWith('-')) {
      return -this.parseArithmeticExpression(expr.slice(1));
    }

    // Parse number
    const num = parseFloat(expr);
    return isNaN(num) ? 0 : num;
  }

  /**
   * Evaluate grid aggregate functions
   */
  evaluateGridFunction(expr: string, grids: Record<string, GridContext>): unknown {
    // Match: grids.gridName.sum('columnName') or grids.gridName.rows.length
    const sumMatch = expr.match(
      /^grids\.([a-zA-Z_][a-zA-Z0-9_]*)\.sum\(['"]([a-zA-Z_][a-zA-Z0-9_]*)['"]\)$/
    );
    if (sumMatch) {
      const gridName = sumMatch[1];
      const columnName = sumMatch[2];
      const grid = grids[gridName];
      if (grid && typeof grid.sum === 'function') {
        return grid.sum(columnName);
      }
      return 0;
    }

    const countMatch = expr.match(/^grids\.([a-zA-Z_][a-zA-Z0-9_]*)\.rows\.length$/);
    if (countMatch) {
      const gridName = countMatch[1];
      const grid = grids[gridName];
      if (grid && Array.isArray(grid.rows)) {
        return grid.rows.length;
      }
      return 0;
    }

    const countFnMatch = expr.match(/^grids\.([a-zA-Z_][a-zA-Z0-9_]*)\.count\(\)$/);
    if (countFnMatch) {
      const grid = grids[countFnMatch[1]];
      return Array.isArray(grid?.rows) ? grid.rows.length : 0;
    }

    const aggregateMatch = expr.match(
      /^grids\.([a-zA-Z_][a-zA-Z0-9_]*)\.(avg|min|max)\(['"]([a-zA-Z_][a-zA-Z0-9_]*)['"]\)$/
    );
    if (aggregateMatch) {
      const [, gridName, operator, columnName] = aggregateMatch;
      const grid = grids[gridName];
      const rows = grid?.rows ?? [];
      const values = rows
        .map((row) => Number(row[columnName]))
        .filter((value) => !Number.isNaN(value));

      if (values.length === 0) {
        return 0;
      }

      if (operator === 'avg') {
        return values.reduce((sum, value) => sum + value, 0) / values.length;
      }

      return operator === 'min' ? Math.min(...values) : Math.max(...values);
    }

    return undefined;
  }
}

/**
 * Grid context for expression evaluation
 */
export interface GridContext {
  rows: Record<string, unknown>[];
  selectedRows?: Record<string, unknown>[];
  selectedRow?: Record<string, unknown> | null;
  sum: (column: string) => number;
}

/**
 * Extended evaluation context with grids and task
 */
export interface ExtendedEvaluationContext extends EvaluationContext {
  grids?: Record<string, GridContext>;
  task?: { id: string; name: string; taskDefinitionKey: string };
  value?: unknown; // Current field value for validation expressions
}

/**
 * Safe expression evaluator for DynamicForm
 * Replaces unsafe new Function() calls with safe parsing
 */
export class SafeExpressionEvaluator extends ExpressionEvaluator {
  private extendedContext: ExtendedEvaluationContext;

  constructor(context: ExtendedEvaluationContext) {
    super(context);
    this.extendedContext = context;
  }

  /**
   * Evaluate a calculation expression safely
   * Returns the calculated value or undefined if evaluation fails
   */
  evaluateCalculation(expression: string): unknown {
    if (!expression || expression.trim() === '') {
      return undefined;
    }

    try {
      // Check for return statement (common in calculation expressions)
      let expr = expression.trim();
      if (expr.startsWith('return ')) {
        expr = expr.slice(7).trim();
      }
      // Remove trailing semicolon
      if (expr.endsWith(';')) {
        expr = expr.slice(0, -1).trim();
      }

      // Try grid functions first
      if (this.extendedContext.grids) {
        const gridResult = this.evaluateGridFunction(expr, this.extendedContext.grids);
        if (gridResult !== undefined) {
          return gridResult;
        }
      }

      // Try arithmetic expression
      if (this.isArithmeticExpression(expr)) {
        return this.evaluateArithmetic(expr);
      }

      // Try boolean expression
      const result = this.evaluate(expr);
      return result === undefined || result === null ? 0 : result;
    } catch (error) {
      console.warn(`SafeExpressionEvaluator: Failed to evaluate calculation: ${expression}`, error);
      return undefined;
    }
  }

  /**
   * Evaluate a visibility expression safely
   * Returns true if visible, false if hidden
   */
  evaluateVisibility(expression: string): boolean {
    if (!expression || expression.trim() === '') {
      return true; // Default to visible
    }

    try {
      let expr = expression.trim();
      if (expr.startsWith('return ')) {
        expr = expr.slice(7).trim();
      }
      if (expr.endsWith(';')) {
        expr = expr.slice(0, -1).trim();
      }

      const result = this.evaluate(expr);
      return this.toBool(result);
    } catch (error) {
      console.warn(`SafeExpressionEvaluator: Failed to evaluate visibility: ${expression}`, error);
      return true; // Default to visible on error
    }
  }

  /**
   * Evaluate a validation expression safely
   * Returns true if valid, false if invalid, or a string error message
   */
  evaluateValidation(expression: string): boolean | string {
    if (!expression || expression.trim() === '') {
      return true; // Default to valid
    }

    try {
      let expr = expression.trim();
      if (expr.startsWith('return ')) {
        expr = expr.slice(7).trim();
      }
      if (expr.endsWith(';')) {
        expr = expr.slice(0, -1).trim();
      }

      const result = this.evaluate(expr);

      // If result is a string, it's an error message
      if (typeof result === 'string' && result !== '') {
        return result;
      }

      return this.toBool(result);
    } catch (error) {
      console.warn(`SafeExpressionEvaluator: Failed to evaluate validation: ${expression}`, error);
      return true; // Default to valid on error
    }
  }

  private toBool(value: unknown): boolean {
    if (value === null || value === undefined) return false;
    if (typeof value === 'boolean') return value;
    if (typeof value === 'string') return value !== '' && value.toLowerCase() !== 'false';
    if (typeof value === 'number') return value !== 0;
    return Boolean(value);
  }

  private isArithmeticExpression(expr: string): boolean {
    // Check if expression contains arithmetic operators
    // but is not a comparison or logical expression
    return (
      /^[0-9a-zA-Z_.\s+\-*/%()]+$/.test(expr) && !/[=!<>&|]/.test(expr) && /[+\-*/%]/.test(expr)
    );
  }

  /**
   * Update context with new values
   */
  updateExtendedContext(updates: Partial<ExtendedEvaluationContext>): void {
    this.extendedContext = { ...this.extendedContext, ...updates };
    this.updateContext(updates);
  }
}

/**
 * Create a safe expression evaluator for DynamicForm
 */
export function createSafeEvaluator(
  context: Partial<ExtendedEvaluationContext> = {}
): SafeExpressionEvaluator {
  return new SafeExpressionEvaluator({
    form: context.form || {},
    process: context.process || {},
    user: context.user || { id: '', username: '', roles: [], groups: [] },
    grids: context.grids,
    task: context.task,
    value: context.value
  });
}

/**
 * Create a default evaluation context with empty values
 */
export function createDefaultContext(): EvaluationContext {
  return {
    form: {},
    process: {},
    user: {
      id: '',
      username: '',
      roles: [],
      groups: []
    }
  };
}

/**
 * Create an evaluator with the given context
 */
export function createEvaluator(context: Partial<EvaluationContext> = {}): ExpressionEvaluator {
  return new ExpressionEvaluator({
    ...createDefaultContext(),
    ...context
  });
}
