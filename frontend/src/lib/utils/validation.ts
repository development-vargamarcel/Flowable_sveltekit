/**
 * Form validation utilities for production-ready validation
 */

export interface ValidationResult {
  isValid: boolean;
  errors: Record<string, string>;
}

export interface ValidationRule {
  validate: (value: unknown) => boolean;
  message: string;
}

/**
 * Common validation rules
 */
export const rules = {
  required: (message = 'This field is required'): ValidationRule => ({
    validate: (value) => {
      if (value === null || value === undefined) return false;
      if (typeof value === 'string') return value.trim().length > 0;
      if (typeof value === 'number') return !isNaN(value);
      return true;
    },
    message
  }),

  minLength: (min: number, message?: string): ValidationRule => ({
    validate: (value) => typeof value === 'string' && value.length >= min,
    message: message ?? `Must be at least ${min} characters`
  }),

  maxLength: (max: number, message?: string): ValidationRule => ({
    validate: (value) => typeof value === 'string' && value.length <= max,
    message: message ?? `Must be at most ${max} characters`
  }),

  min: (minValue: number, message?: string): ValidationRule => ({
    validate: (value) => typeof value === 'number' && value >= minValue,
    message: message ?? `Must be at least ${minValue}`
  }),

  max: (maxValue: number, message?: string): ValidationRule => ({
    validate: (value) => typeof value === 'number' && value <= maxValue,
    message: message ?? `Must be at most ${maxValue}`
  }),

  positive: (message = 'Must be a positive number'): ValidationRule => ({
    validate: (value) => typeof value === 'number' && value > 0,
    message
  }),

  email: (message = 'Please enter a valid email address'): ValidationRule => ({
    validate: (value) => {
      if (typeof value !== 'string') return false;
      const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
      return emailRegex.test(value);
    },
    message
  }),

  dateAfter: (
    getMinDate: () => string | Date,
    message = 'Date must be after the minimum date'
  ): ValidationRule => ({
    validate: (value) => {
      if (!value) return true;
      const date = new Date(value as string);
      const minDate = new Date(getMinDate());
      return date >= minDate;
    },
    message
  }),

  dateBefore: (
    getMaxDate: () => string | Date,
    message = 'Date must be before the maximum date'
  ): ValidationRule => ({
    validate: (value) => {
      if (!value) return true;
      const date = new Date(value as string);
      const maxDate = new Date(getMaxDate());
      return date <= maxDate;
    },
    message
  })
};

/**
 * Validate a single field with multiple rules
 */
export function validateField(value: unknown, fieldRules: ValidationRule[]): string | null {
  for (const rule of fieldRules) {
    if (!rule.validate(value)) {
      return rule.message;
    }
  }
  return null;
}

/**
 * Validate an entire form
 */
export function validateForm(
  data: Record<string, unknown>,
  schema: Record<string, ValidationRule[]>
): ValidationResult {
  const errors: Record<string, string> = {};

  for (const [field, fieldRules] of Object.entries(schema)) {
    const error = validateField(data[field], fieldRules);
    if (error) {
      errors[field] = error;
    }
  }

  return {
    isValid: Object.keys(errors).length === 0,
    errors
  };
}

/**
 * Sanitize string input to prevent XSS
 */
export function sanitizeInput(value: string): string {
  return value
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#039;');
}

/**
 * Format validation errors for display
 */
export function formatErrors(errors: Record<string, string>): string[] {
  return Object.entries(errors).map(([field, message]) => {
    const formattedField = field
      .replace(/([A-Z])/g, ' $1')
      .replace(/^./, (str) => str.toUpperCase())
      .trim();
    return `${formattedField}: ${message}`;
  });
}
