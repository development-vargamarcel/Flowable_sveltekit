import { type ClassValue, clsx } from 'clsx';
import { twMerge } from 'tailwind-merge';

export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs));
}

type FormatterLocale = string | string[] | undefined;

export interface DateFormatOptions {
  locale?: FormatterLocale;
  mode?: 'datetime' | 'date' | 'time' | 'relative';
  fallback?: string;
  /**
   * Enforce UTC output to avoid timezone-dependent snapshots and inconsistent dashboards.
   */
  useUTC?: boolean;
  /**
   * Optional timezone for locale formatting, ignored when mode is relative.
   */
  timeZone?: string;
  hour12?: boolean;
}

export interface DurationFormatOptions {
  fallback?: string;
  compact?: boolean;
  includeSeconds?: boolean;
  /**
   * When true, include zero-value units in compact mode for predictable alignment.
   */
  padUnits?: boolean;
}

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

function toFiniteNumber(value: unknown): number | null {
  if (typeof value === 'number' && Number.isFinite(value)) {
    return value;
  }

  if (typeof value === 'string') {
    const trimmed = value.trim();
    if (!trimmed) return null;

    // Handle common numeric formats from user input and CSV imports.
    const normalized = trimmed.replace(/,/g, '');
    const parsed = Number(normalized);
    return Number.isFinite(parsed) ? parsed : null;
  }

  return null;
}

function truncateText(value: unknown, maxLength: number): string {
  const normalized = String(value).trim().replace(/\s+/g, ' ');
  if (normalized.length <= maxLength) return normalized;
  return `${normalized.slice(0, Math.max(0, maxLength - 1))}…`;
}

function normalizeVariableLabel(rawKey: string, labelMap?: Record<string, string>): string {
  if (labelMap?.[rawKey]) {
    return labelMap[rawKey];
  }

  return rawKey
    .replace(/[_-]+/g, ' ')
    .replace(/\s+/g, ' ')
    .trim()
    .replace(/\b\w/g, (char) => char.toUpperCase());
}

function normalizeVariableValue(value: unknown, maxTextLength: number): string {
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

export function formatCurrency(
  value: unknown,
  options: {
    locale?: FormatterLocale;
    currency?: string;
    fallback?: string;
    minimumFractionDigits?: number;
    maximumFractionDigits?: number;
    currencySign?: 'standard' | 'accounting';
  } = {}
): string {
  const amount = toFiniteNumber(value);
  const fallback = options.fallback ?? 'N/A';
  if (amount === null) return fallback;

  const currency = options.currency ?? 'USD';

  try {
    return new Intl.NumberFormat(options.locale, {
      style: 'currency',
      currency,
      minimumFractionDigits: options.minimumFractionDigits ?? 2,
      maximumFractionDigits: options.maximumFractionDigits ?? 2,
      currencySign: options.currencySign ?? 'standard'
    }).format(amount);
  } catch {
    const sign = amount < 0 ? '-' : '';
    return `${sign}$${Math.abs(amount).toFixed(options.maximumFractionDigits ?? 2)}`;
  }
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

export function formatDate(dateStr: string, options: DateFormatOptions = {}): string {
  const fallback = options.fallback ?? 'N/A';
  if (!dateStr) return fallback;

  const date = new Date(dateStr);
  if (Number.isNaN(date.getTime())) return 'Invalid date';

  const mode = options.mode ?? 'datetime';

  if (mode === 'relative') {
    const diffMs = date.getTime() - Date.now();
    const absSeconds = Math.abs(Math.round(diffMs / 1000));

    if (absSeconds < 30) return 'just now';

    const rtf = new Intl.RelativeTimeFormat(options.locale, { numeric: 'auto' });
    const absMinutes = Math.round(absSeconds / 60);
    if (absMinutes < 60) return rtf.format(Math.round(diffMs / 60000), 'minute');

    const absHours = Math.round(absMinutes / 60);
    if (absHours < 24) return rtf.format(Math.round(diffMs / 3600000), 'hour');

    const absDays = Math.round(absHours / 24);
    if (absDays < 30) return rtf.format(Math.round(diffMs / 86400000), 'day');

    const absMonths = Math.round(absDays / 30);
    if (absMonths < 12) return rtf.format(Math.round(diffMs / (30 * 86400000)), 'month');

    return rtf.format(Math.round(diffMs / (365 * 86400000)), 'year');
  }

  const formatOptions: Intl.DateTimeFormatOptions = {
    timeZone: options.useUTC ? 'UTC' : options.timeZone,
    hour12: options.hour12
  };

  if (mode === 'date') {
    return date.toLocaleDateString(options.locale, formatOptions);
  }

  if (mode === 'time') {
    return date.toLocaleTimeString(options.locale, formatOptions);
  }

  return date.toLocaleString(options.locale, formatOptions);
}

export function formatDuration(millis: number | null, options: DurationFormatOptions = {}): string {
  const fallback = options.fallback ?? 'N/A';
  const duration = typeof millis === 'number' ? millis : null;
  if (duration === null || duration < 0) return fallback;
  if (duration === 0) return options.includeSeconds ? '0h 0m 0s' : '0h 0m';

  const totalSeconds = Math.floor(duration / 1000);
  const days = Math.floor(totalSeconds / 86400);
  const hours = Math.floor((totalSeconds % 86400) / 3600);
  const minutes = Math.floor((totalSeconds % 3600) / 60);
  const seconds = totalSeconds % 60;

  if (options.compact) {
    const compactParts = [
      days > 0 || options.padUnits ? `${days}d` : null,
      hours > 0 || days > 0 || options.padUnits ? `${hours}h` : null,
      `${minutes}m`,
      options.includeSeconds ? `${seconds}s` : null
    ].filter(Boolean);
    return compactParts.join(' ');
  }

  const parts = [`${days}d`, `${hours}h`, `${minutes}m`];
  if (options.includeSeconds) {
    parts.push(`${seconds}s`);
  }

  if (days === 0) {
    const trimmed = [`${hours}h`, `${minutes}m`];
    if (options.includeSeconds) {
      trimmed.push(`${seconds}s`);
    }
    return trimmed.join(' ');
  }

  return parts.join(' ');
}

export type CSVExportOptions = {
  includeBom?: boolean;
  headers?: string[];
  includeHeader?: boolean;
  delimiter?: ',' | ';' | '\t';
  lineEnding?: '\n' | '\r\n';
  nullValue?: string;
  quoteAllFields?: boolean;
  sortHeaders?: boolean;
  filenamePrefix?: string;
};

function serializeCsvValue(value: unknown, nullValue: string): string {
  if (value === null || value === undefined) {
    return nullValue;
  }

  if (typeof value === 'boolean') {
    return value ? 'true' : 'false';
  }

  if (value instanceof Date) {
    return value.toISOString();
  }

  if (typeof value === 'object') {
    return JSON.stringify(value);
  }

  return String(value);
}

function sanitizeFilename(filename: string, prefix?: string): string {
  const base = `${prefix ?? ''}${filename}`;
  const sanitized = base
    .replace(/[\\/:*?"<>|]+/g, '-')
    .replace(/\s+/g, '_')
    .trim();
  return sanitized.length > 0 ? sanitized : 'export.csv';
}

export function toCSV(data: Record<string, unknown>[], options: CSVExportOptions = {}): string {
  if (!data.length) return '';

  const headers =
    options.headers && options.headers.length > 0
      ? options.headers
      : Array.from(new Set(data.flatMap((row) => Object.keys(row))));
  const orderedHeaders = options.sortHeaders
    ? [...headers].sort((a, b) => a.localeCompare(b))
    : headers;

  const delimiter = options.delimiter ?? ',';
  const includeHeader = options.includeHeader ?? true;
  const lineEnding = options.lineEnding ?? '\n';
  const nullValue = options.nullValue ?? '';
  const csvRows: string[] = [];

  if (includeHeader) {
    csvRows.push(orderedHeaders.join(delimiter));
  }

  for (const row of data) {
    const values = orderedHeaders.map((header) => {
      const serialized = serializeCsvValue(row[header], nullValue);
      const escaped = serialized.replace(/"/g, '""');
      if (
        options.quoteAllFields === false &&
        !escaped.includes(delimiter) &&
        !escaped.includes('"') &&
        !escaped.includes('\n')
      ) {
        return escaped;
      }
      return `"${escaped}"`;
    });
    csvRows.push(values.join(delimiter));
  }

  return csvRows.join(lineEnding);
}

export function exportToCSV(
  data: Record<string, unknown>[],
  filename: string,
  options: CSVExportOptions = {}
): { success: boolean; reason?: string; filename?: string } {
  if (!data || !data.length) {
    console.warn('No data to export');
    return { success: false, reason: 'No data to export' };
  }

  if (typeof window === 'undefined' || typeof document === 'undefined') {
    return { success: false, reason: 'CSV export is only available in the browser' };
  }

  if (typeof window.URL?.createObjectURL !== 'function') {
    return { success: false, reason: 'CSV export is not supported in this environment' };
  }

  const csvString = toCSV(data, options);
  const payload = options.includeBom ? `\uFEFF${csvString}` : csvString;
  const safeFilename = sanitizeFilename(filename, options.filenamePrefix);

  const blob = new Blob([payload], { type: 'text/csv' });
  const url = window.URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.setAttribute('hidden', '');
  a.setAttribute('href', url);
  a.setAttribute('download', safeFilename);
  document.body.appendChild(a);
  a.click();
  document.body.removeChild(a);
  window.URL.revokeObjectURL(url);

  return { success: true, filename: safeFilename };
}
