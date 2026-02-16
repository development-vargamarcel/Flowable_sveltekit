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
}

export interface DurationFormatOptions {
  fallback?: string;
  compact?: boolean;
  includeSeconds?: boolean;
}

export interface VariableDisplayOptions {
  maxItems?: number;
  maxTextLength?: number;
  locale?: FormatterLocale;
  currency?: string;
}

function toFiniteNumber(value: unknown): number | null {
  if (typeof value === 'number' && Number.isFinite(value)) {
    return value;
  }

  if (typeof value === 'string') {
    const trimmed = value.trim();
    if (!trimmed) return null;

    const parsed = Number(trimmed);
    return Number.isFinite(parsed) ? parsed : null;
  }

  return null;
}

function truncateText(value: unknown, maxLength: number): string {
  const normalized = String(value).trim().replace(/\s+/g, ' ');
  if (normalized.length <= maxLength) return normalized;
  return `${normalized.slice(0, Math.max(0, maxLength - 1))}…`;
}

export function formatCurrency(
  value: unknown,
  options: {
    locale?: FormatterLocale;
    currency?: string;
    fallback?: string;
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
      minimumFractionDigits: 2,
      maximumFractionDigits: 2
    }).format(amount);
  } catch {
    return `$${amount.toFixed(2)}`;
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

  return displays.slice(0, maxItems);
}

export function formatDate(dateStr: string, options: DateFormatOptions = {}): string {
  const fallback = options.fallback ?? 'N/A';
  if (!dateStr) return fallback;

  const date = new Date(dateStr);
  if (Number.isNaN(date.getTime())) return 'Invalid date';

  const mode = options.mode ?? 'datetime';

  if (mode === 'relative') {
    const diffMs = date.getTime() - Date.now();
    const diffMinutes = Math.round(diffMs / 60000);
    const absMinutes = Math.abs(diffMinutes);

    if (absMinutes < 1) return 'just now';
    if (absMinutes < 60) return `${Math.abs(diffMinutes)}m ${diffMinutes < 0 ? 'ago' : 'from now'}`;

    const diffHours = Math.round(diffMinutes / 60);
    const absHours = Math.abs(diffHours);
    if (absHours < 24) return `${absHours}h ${diffHours < 0 ? 'ago' : 'from now'}`;

    const diffDays = Math.round(diffHours / 24);
    return `${Math.abs(diffDays)}d ${diffDays < 0 ? 'ago' : 'from now'}`;
  }

  if (mode === 'date') {
    return date.toLocaleDateString(options.locale);
  }

  if (mode === 'time') {
    return date.toLocaleTimeString(options.locale);
  }

  return date.toLocaleString(options.locale);
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
      days > 0 ? `${days}d` : null,
      hours > 0 || days > 0 ? `${hours}h` : null,
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
};

export function toCSV(data: Record<string, unknown>[], options: CSVExportOptions = {}): string {
  if (!data.length) return '';

  const headers =
    options.headers && options.headers.length > 0
      ? options.headers
      : Array.from(new Set(data.flatMap((row) => Object.keys(row))));
  const delimiter = options.delimiter ?? ',';
  const includeHeader = options.includeHeader ?? true;
  const lineEnding = options.lineEnding ?? '\n';
  const csvRows: string[] = [];

  if (includeHeader) {
    csvRows.push(headers.join(delimiter));
  }

  for (const row of data) {
    const values = headers.map((header) => {
      const val = row[header];
      const escaped = String(val ?? '').replace(/"/g, '""');
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
): { success: boolean; reason?: string } {
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

  const blob = new Blob([payload], { type: 'text/csv' });
  const url = window.URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.setAttribute('hidden', '');
  a.setAttribute('href', url);
  a.setAttribute('download', filename);
  document.body.appendChild(a);
  a.click();
  document.body.removeChild(a);
  window.URL.revokeObjectURL(url);

  return { success: true };
}
