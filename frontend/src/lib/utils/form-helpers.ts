/**
 * Form state management helpers for Svelte 5
 * Provides reusable patterns for form validation and submission
 */

import { goto } from '$app/navigation';
import { rules, validateField, type ValidationRule } from './validation';

export { rules, validateField, type ValidationRule };

/**
 * Toast state type
 */
export interface ToastState {
  message: string;
  type: 'success' | 'error';
}

/**
 * Form state interface
 */
export interface FormState<T extends Record<string, unknown>> {
  values: T;
  touched: Record<keyof T, boolean>;
  errors: Record<keyof T, string | null>;
  submitting: boolean;
  toast: ToastState | null;
}

/**
 * Create initial form state
 */
export function createFormState<T extends Record<string, unknown>>(initialValues: T): FormState<T> {
  const touched = {} as Record<keyof T, boolean>;
  const errors = {} as Record<keyof T, string | null>;

  for (const key of Object.keys(initialValues)) {
    touched[key as keyof T] = false;
    errors[key as keyof T] = null;
  }

  return {
    values: initialValues,
    touched,
    errors,
    submitting: false,
    toast: null
  };
}

/**
 * Validate a single field and update errors
 */
export function validateFormField<T extends Record<string, unknown>>(
  field: keyof T,
  value: unknown,
  validationRules: Record<keyof T, ValidationRule[]>,
  touched: Record<keyof T, boolean>,
  errors: Record<keyof T, string | null>
): void {
  touched[field] = true;
  const fieldRules = validationRules[field];
  if (fieldRules) {
    errors[field] = validateField(value, fieldRules);
  }
}

/**
 * Validate all fields in a form
 */
export function validateAllFormFields<T extends Record<string, unknown>>(
  values: T,
  validationRules: Partial<Record<keyof T, ValidationRule[]>>,
  touched: Record<keyof T, boolean>,
  errors: Record<keyof T, string | null>
): boolean {
  let isValid = true;

  for (const [field, value] of Object.entries(values)) {
    touched[field as keyof T] = true;
    const fieldRules = validationRules[field as keyof T];
    if (fieldRules) {
      const error = validateField(value, fieldRules);
      errors[field as keyof T] = error;
      if (error) isValid = false;
    }
  }

  return isValid;
}

/**
 * Create a form submission handler
 */
export function createSubmitHandler(options: {
  validate: () => boolean;
  onSubmit: () => Promise<void>;
  onSuccess: (message: string) => void;
  onError: (message: string) => void;
  setSubmitting: (value: boolean) => void;
  successMessage: string;
  errorMessage: string;
  redirectTo?: string;
  redirectDelay?: number;
}) {
  return async (event: Event) => {
    event.preventDefault();

    if (!options.validate()) {
      options.onError('Please fix the errors below');
      return;
    }

    options.setSubmitting(true);
    try {
      await options.onSubmit();
      options.onSuccess(options.successMessage);

      const redirectUrl = options.redirectTo;
      if (redirectUrl) {
        setTimeout(() => goto(redirectUrl), options.redirectDelay ?? 1500);
      }
    } catch (err) {
      options.onError(err instanceof Error ? err.message : options.errorMessage);
    } finally {
      options.setSubmitting(false);
    }
  };
}

/**
 * Get CSS classes for form input based on validation state
 */
export function getInputClasses(touched: boolean, error: string | null): string {
  if (touched && error) {
    return 'border-red-500 focus:ring-red-500';
  }
  return '';
}

/**
 * Format date for display
 */
export function formatDate(dateString: string): string {
  return new Date(dateString).toLocaleDateString('en-US', {
    month: 'short',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit'
  });
}

/**
 * Format date as ISO date string (YYYY-MM-DD)
 */
export function formatISODate(date: Date): string {
  return date.toISOString().split('T')[0];
}

/**
 * Calculate days between two dates (inclusive)
 */
export function calculateDays(startDate: string, endDate: string): number {
  if (!startDate || !endDate) return 0;
  const start = new Date(startDate);
  const end = new Date(endDate);
  const diffTime = Math.abs(end.getTime() - start.getTime());
  return Math.ceil(diffTime / (1000 * 60 * 60 * 24)) + 1;
}

/**
 * Format currency value
 */
export function formatCurrency(value: number, currency = 'USD'): string {
  return new Intl.NumberFormat('en-US', {
    style: 'currency',
    currency
  }).format(value);
}
