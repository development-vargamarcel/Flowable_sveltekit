/**
 * Public validation API.
 *
 * Keep this barrel intentionally small: only export symbols needed by external
 * consumers. Internal modules inside `lib/validation` should import directly
 * from sibling files (e.g. `./schemas`, `./dynamic-validator`).
 */

export {
  // Top-level schema entrypoints intended for app-level callers
  documentTypeSchema,
  processFormConfigSchema,
  validationResultSchema,
  validationErrorSchema,
  // Primary public types
  type DocumentType,
  type ProcessFormConfig,
  type ValidationResult,
  type ValidationError
} from './schemas';

export {
  // Dynamic validation API intended for app-level callers
  createDocumentFormSchema,
  validateFormData,
  validateFieldValue,
  validateGridData,
  sanitizeInput,
  createServerSchema,
  type FormValidationResult
} from './dynamic-validator';
