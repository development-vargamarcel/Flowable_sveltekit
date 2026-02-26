/**
 * Validation Module - Zod-based form validation
 *
 * This module provides:
 * - Zod schemas for field, grid, and document validation
 * - Dynamic validation that creates schemas from field definitions
 * - A Svelte 5 runes-based validation hook
 *
 * All schemas work both client-side and server-side in SvelteKit.
 */

// Export all schemas and types
export {
  // Field validation
  fieldStateSchema,
  fieldConstraintsSchema,
  fieldValidationSchema,
  fieldOptionSchema,
  fieldTypeEnum,
  fieldDefinitionSchema,

  // Grid validation
  gridStateSchema,
  gridColumnSchema,
  gridDefinitionSchema,

  // Document type
  documentTypeSchema,

  // Condition rules
  conditionTargetSchema,
  conditionEffectEnum,
  conditionRuleSchema,

  // Process form configuration
  processFormConfigSchema,

  // Validation results
  validationErrorSchema,
  validationResultSchema,

  // Computed states
  computedFieldStateSchema,
  computedGridStateSchema,

  // Defaults
  defaultFieldValidation,
  defaultGridValidation,

  // Types
  type FieldState,
  type FieldConstraints,
  type FieldValidation,
  type FieldOption,
  type FieldType,
  type FieldDefinition,
  type GridState,
  type GridColumn,
  type GridDefinition,
  type DocumentType,
  type ConditionTarget,
  type ConditionEffect,
  type ConditionRule,
  type ProcessFormConfig,
  type ValidationError,
  type ValidationResult,
  type ComputedFieldState,
  type ComputedGridState
} from './schemas';

// Export dynamic validator functions
export {
  createFieldValueSchema,
  createGridRowSchema,
  createGridSchema,
  createDocumentFormSchema,
  validateFormData,
  validateFieldValue,
  validateGridData,
  sanitizeInput,
  createServerSchema,
  type FormValidationResult
} from './dynamic-validator';

// Export the Svelte 5 validation hook
export { useFormValidation } from './useFormValidation.svelte';
