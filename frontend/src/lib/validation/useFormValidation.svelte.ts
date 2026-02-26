/**
 * Svelte 5 Runes-based Form Validation Hook
 *
 * Provides reactive form validation using Zod schemas.
 * Works with the dynamic validator to validate form data in real-time.
 */

import { createDocumentFormSchema, validateFieldValue as validateField } from './dynamic-validator';
import type { FieldDefinition, GridDefinition, ComputedFieldState } from './schemas';

/**
 * Form validation state and methods using Svelte 5 runes
 */
export function useFormValidation(
  initialFields: FieldDefinition[],
  initialGrids: GridDefinition[]
) {
  // State using $state rune
  let errors = $state<Record<string, string>>({});
  let touched = $state<Set<string>>(new Set());
  let isValidating = $state(false);

  // Field and grid definitions (can be updated)
  let fields = $state(initialFields);
  let grids = $state(initialGrids);

  // Computed states (from condition evaluation)
  let fieldStates = $state<Map<string, ComputedFieldState>>(new Map());
  let gridColumnStates = $state<Map<string, Map<string, ComputedFieldState>>>(new Map());

  // Derived state for checking if form is valid
  const isValid = $derived(Object.keys(errors).length === 0);

  // Derived state for checking if any field has been touched
  const isDirty = $derived(touched.size > 0);

  /**
   * Update field definitions
   */
  function setFields(newFields: FieldDefinition[]) {
    fields = newFields;
  }

  /**
   * Update grid definitions
   */
  function setGrids(newGrids: GridDefinition[]) {
    grids = newGrids;
  }

  /**
   * Update computed states from condition evaluation
   */
  function setComputedStates(
    newFieldStates: Map<string, ComputedFieldState>,
    newGridColumnStates: Map<string, Map<string, ComputedFieldState>>
  ) {
    fieldStates = newFieldStates;
    gridColumnStates = newGridColumnStates;
  }

  /**
   * Validate entire form
   */
  function validate(data: Record<string, unknown>): boolean {
    isValidating = true;

    const schema = createDocumentFormSchema(fields, grids, fieldStates, gridColumnStates);
    const result = schema.safeParse(data);

    if (result.success) {
      errors = {};
      isValidating = false;
      return true;
    }

    // Convert Zod errors to field-keyed error messages
    const newErrors: Record<string, string> = {};
    for (const issue of result.error.issues) {
      const path = issue.path.join('.');
      if (!newErrors[path]) {
        newErrors[path] = issue.message;
      }
    }
    errors = newErrors;
    isValidating = false;
    return false;
  }

  /**
   * Validate a single field
   */
  function validateFieldByName(name: string, value: unknown): string | null {
    // Mark field as touched
    touched.add(name);
    touched = new Set(touched);

    // Find the field definition
    const field = fields.find((f) => f.name === name);
    if (!field) {
      // Check if it's a grid field (format: gridName.rowIndex.fieldName)
      const parts = name.split('.');
      if (parts.length === 3) {
        const [gridName, , fieldName] = parts;
        const grid = grids.find((g) => g.name === gridName);
        if (grid) {
          const column = grid.columns.find((c) => c.name === fieldName);
          if (column) {
            const columnState = gridColumnStates.get(grid.id)?.get(column.id);
            const error = validateField(column, value, columnState);
            if (error) {
              errors = { ...errors, [name]: error };
            } else {
              const { [name]: _, ...rest } = errors;
              errors = rest;
            }
            return error;
          }
        }
      }
      return null;
    }

    const fieldState = fieldStates.get(field.id);
    const error = validateField(field, value, fieldState);

    if (error) {
      errors = { ...errors, [name]: error };
    } else {
      const { [name]: _, ...rest } = errors;
      errors = rest;
    }

    return error;
  }

  /**
   * Clear all errors
   */
  function clearErrors() {
    errors = {};
  }

  /**
   * Clear error for a specific field
   */
  function clearFieldError(name: string) {
    const { [name]: _, ...rest } = errors;
    errors = rest;
  }

  /**
   * Reset touched state
   */
  function resetTouched() {
    touched = new Set();
  }

  /**
   * Mark a field as touched
   */
  function touchField(name: string) {
    touched.add(name);
    touched = new Set(touched);
  }

  /**
   * Check if a field has been touched
   */
  function isFieldTouched(name: string): boolean {
    return touched.has(name);
  }

  /**
   * Get error for a specific field
   */
  function getError(name: string): string | undefined {
    return errors[name];
  }

  /**
   * Check if a field has an error
   */
  function hasError(name: string): boolean {
    return name in errors;
  }

  return {
    // State getters (reactive)
    get errors() {
      return errors;
    },
    get isValid() {
      return isValid;
    },
    get isDirty() {
      return isDirty;
    },
    get isValidating() {
      return isValidating;
    },

    // Methods
    validate,
    validateField: validateFieldByName,
    clearErrors,
    clearFieldError,
    resetTouched,
    touchField,
    isFieldTouched,
    getError,
    hasError,
    setFields,
    setGrids,
    setComputedStates
  };
}
