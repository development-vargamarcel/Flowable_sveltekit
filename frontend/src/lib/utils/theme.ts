/**
 * Centralized theming utilities for consistent color mappings across the application
 */

export type ProcessType =
  | 'expense-approval'
  | 'leave-request'
  | 'task-assignment'
  | 'purchase-request'
  | 'project-proposal'
  | string;
export type ColorName =
  | 'green'
  | 'blue'
  | 'amber'
  | 'purple'
  | 'indigo'
  | 'emerald'
  | 'sky'
  | 'teal'
  | 'red'
  | 'gray';
export type Priority = 'high' | 'medium' | 'low';

/**
 * Process type to color mapping
 */
const processColorMap: Record<string, ColorName> = {
  'expense-approval': 'emerald',
  'leave-request': 'sky',
  'task-assignment': 'amber',
  'purchase-request': 'purple',
  'project-proposal': 'indigo'
};

/**
 * Get the color name for a process type
 */
export function getProcessColorName(processKey: string): ColorName {
  return processColorMap[processKey] || 'gray';
}

/**
 * Get background and border classes for a process card/task
 */
export function getProcessCardClasses(processKey: string): string {
  const colorMap: Record<ColorName, string> = {
    emerald: 'bg-emerald-50 border-emerald-200',
    sky: 'bg-sky-50 border-sky-200',
    amber: 'bg-amber-50 border-amber-200',
    purple: 'bg-purple-50 border-purple-200',
    indigo: 'bg-indigo-50 border-indigo-200',
    green: 'bg-green-50 border-green-200',
    blue: 'bg-blue-50 border-blue-200',
    teal: 'bg-teal-50 border-teal-200',
    red: 'bg-red-50 border-red-200',
    gray: 'bg-gray-50 border-gray-200'
  };
  return colorMap[getProcessColorName(processKey)];
}

/**
 * Get background, border, and hover classes for interactive process cards
 */
export function getProcessCardHoverClasses(color: ColorName): string {
  const colorMap: Record<ColorName, string> = {
    emerald: 'bg-emerald-50 border-emerald-200 hover:bg-emerald-100',
    sky: 'bg-sky-50 border-sky-200 hover:bg-sky-100',
    amber: 'bg-amber-50 border-amber-200 hover:bg-amber-100',
    purple: 'bg-purple-50 border-purple-200 hover:bg-purple-100',
    indigo: 'bg-indigo-50 border-indigo-200 hover:bg-indigo-100',
    green: 'bg-green-50 border-green-200 hover:bg-green-100',
    blue: 'bg-blue-50 border-blue-200 hover:bg-blue-100',
    teal: 'bg-teal-50 border-teal-200 hover:bg-teal-100',
    red: 'bg-red-50 border-red-200 hover:bg-red-100',
    gray: 'bg-gray-50 border-gray-200 hover:bg-gray-100'
  };
  return colorMap[color];
}

/**
 * Get icon background color class
 */
export function getIconBgClass(color: ColorName): string {
  const colorMap: Record<ColorName, string> = {
    emerald: 'bg-emerald-500',
    sky: 'bg-sky-500',
    amber: 'bg-amber-500',
    purple: 'bg-purple-500',
    indigo: 'bg-indigo-500',
    green: 'bg-green-500',
    blue: 'bg-blue-500',
    teal: 'bg-teal-500',
    red: 'bg-red-500',
    gray: 'bg-gray-500'
  };
  return colorMap[color];
}

/**
 * Get priority based on numeric value
 */
export function getPriorityFromValue(priority: number): Priority {
  if (priority >= 75) return 'high';
  if (priority >= 50) return 'medium';
  return 'low';
}

/**
 * Get priority display label
 */
export function getPriorityLabel(priority: number): string {
  const p = getPriorityFromValue(priority);
  return p.charAt(0).toUpperCase() + p.slice(1);
}

/**
 * Get priority badge classes
 */
export function getPriorityClasses(priority: number): string {
  const priorityMap: Record<Priority, string> = {
    high: 'bg-red-100 text-red-800 border-red-200',
    medium: 'bg-yellow-100 text-yellow-800 border-yellow-200',
    low: 'bg-green-100 text-green-800 border-green-200'
  };
  return priorityMap[getPriorityFromValue(priority)];
}

/**
 * Get button variant classes for different action types
 */
export function getActionButtonClasses(
  variant: 'approve' | 'reject' | 'escalate' | 'deescalate' | 'handoff' | 'primary' | 'secondary'
): string {
  const variantMap: Record<string, string> = {
    approve: 'bg-green-600 text-white hover:bg-green-700',
    reject: 'bg-red-600 text-white hover:bg-red-700',
    escalate: 'bg-amber-500 text-white hover:bg-amber-600',
    deescalate: 'bg-teal-500 text-white hover:bg-teal-600',
    handoff: 'bg-indigo-500 text-white hover:bg-indigo-600',
    primary: 'bg-blue-600 text-white hover:bg-blue-700',
    secondary: 'border border-gray-300 bg-white text-gray-700 hover:bg-gray-50'
  };
  return variantMap[variant] || variantMap.primary;
}
