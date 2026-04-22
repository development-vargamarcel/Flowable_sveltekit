import { formatCurrency, toFiniteNumber, type FormatterLocale } from './currency';

export interface VariableDisplayOptions {
  maxItems?: number;
  maxTextLength?: number;
  locale?: FormatterLocale;
  currency?: string;
  /**
   * Optional variable order for deterministic display composition.
   */
  preferredOrder?: string[];
  /**
   * Maps raw variable keys to user-facing labels.
   */
  labelMap?: Record<string, string>;
  /**
   * Include a trailing summary when values are truncated by maxItems.
   */
  showRemainingCount?: boolean;
}

export function truncateText(value: unknown, maxLength: number): string {
  const normalized = String(value).trim().replace(/\s+/g, ' ');
  if (normalized.length <= maxLength) return normalized;
  return `${normalized.slice(0, Math.max(0, maxLength - 1))}…`;
}

export function normalizeVariableLabel(rawKey: string, labelMap?: Record<string, string>): string {
  if (labelMap?.[rawKey]) {
    return labelMap[rawKey];
  }

  return rawKey
    .replace(/[_-]+/g, ' ')
    .replace(/\s+/g, ' ')
    .trim()
    .replace(/\b\w/g, (char) => char.toUpperCase());
}

export function normalizeVariableValue(value: unknown, maxTextLength: number): string {
  if (typeof value === 'boolean') {
    return value ? 'Yes' : 'No';
  }

  if (value === null || value === undefined) {
    return 'N/A';
  }

  if (typeof value === 'object') {
    return truncateText(JSON.stringify(value), maxTextLength);
  }

  return truncateText(value, maxTextLength);
}

export function getVariableDisplay(
  variables: Record<string, unknown> | undefined | null,
  options: VariableDisplayOptions = {}
): { label: string; value: string }[] {
  const maxItems = options.maxItems ?? 3;
  const maxTextLength = options.maxTextLength ?? 48;

  const displays: { label: string; value: string }[] = [];

  if (!variables) return displays;

  if (variables.amount !== undefined && variables.amount !== null) {
    const formatted = formatCurrency(variables.amount, {
      locale: options.locale,
      currency: options.currency
    });
    if (formatted !== 'N/A') {
      displays.push({ label: 'Amount', value: formatted });
    }
  }

  if (variables.category) {
    displays.push({ label: 'Category', value: truncateText(variables.category, maxTextLength) });
  }
  if (variables.leaveType) {
    displays.push({ label: 'Type', value: truncateText(variables.leaveType, maxTextLength) });
  }
  if (variables.days !== undefined) {
    displays.push({ label: 'Days', value: truncateText(variables.days, maxTextLength) });
  }
  if (variables.priority !== undefined) {
    const priority = toFiniteNumber(variables.priority);
    if (priority !== null) {
      const label = priority >= 75 ? 'High' : priority >= 35 ? 'Medium' : 'Low';
      displays.push({ label: 'Priority', value: `${label} (${Math.round(priority)})` });
    }
  }
  if (variables.title) {
    displays.push({ label: 'Title', value: truncateText(variables.title, maxTextLength) });
  }
  if (variables.employeeName) {
    displays.push({ label: 'From', value: truncateText(variables.employeeName, maxTextLength) });
  }

  // Add remaining fields in a deterministic order, excluding keys handled by semantic shortcuts.
  const handledKeys = new Set([
    'amount',
    'category',
    'leaveType',
    'days',
    'priority',
    'title',
    'employeeName'
  ]);
  const dynamicEntries = Object.entries(variables).filter(([key]) => !handledKeys.has(key));
  const preferredOrder = options.preferredOrder ?? [];

  dynamicEntries.sort(([a], [b]) => {
    const aIdx = preferredOrder.indexOf(a);
    const bIdx = preferredOrder.indexOf(b);
    if (aIdx !== -1 || bIdx !== -1) {
      if (aIdx === -1) return 1;
      if (bIdx === -1) return -1;
      return aIdx - bIdx;
    }
    return a.localeCompare(b);
  });

  for (const [key, value] of dynamicEntries) {
    displays.push({
      label: normalizeVariableLabel(key, options.labelMap),
      value: normalizeVariableValue(value, maxTextLength)
    });
  }

  if (!options.showRemainingCount) {
    return displays.slice(0, maxItems);
  }

  if (displays.length <= maxItems) {
    return displays;
  }

  const result = displays.slice(0, Math.max(0, maxItems - 1));
  const remainingCount = displays.length - result.length;
  result.push({ label: 'More', value: `+${remainingCount} more` });
  return result;
}
