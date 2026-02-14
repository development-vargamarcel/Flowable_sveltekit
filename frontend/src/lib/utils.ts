import { type ClassValue, clsx } from 'clsx';
import { twMerge } from 'tailwind-merge';

export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs));
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

export function getVariableDisplay(
  variables: Record<string, unknown> | undefined | null
): { label: string; value: string }[] {
  const displays: { label: string; value: string }[] = [];

  if (!variables) return displays;

  const amount = toFiniteNumber(variables.amount);
  if (amount !== null) {
    displays.push({ label: 'Amount', value: `$${amount.toFixed(2)}` });
  }
  if (variables.category) {
    displays.push({ label: 'Category', value: String(variables.category) });
  }
  if (variables.leaveType) {
    displays.push({ label: 'Type', value: String(variables.leaveType) });
  }
  if (variables.days !== undefined) {
    displays.push({ label: 'Days', value: String(variables.days) });
  }
  if (variables.title) {
    displays.push({ label: 'Title', value: String(variables.title) });
  }
  if (variables.employeeName) {
    displays.push({ label: 'From', value: String(variables.employeeName) });
  }

  return displays.slice(0, 3);
}

export function formatDate(dateStr: string): string {
  if (!dateStr) return 'N/A';

  const date = new Date(dateStr);
  if (Number.isNaN(date.getTime())) return 'Invalid date';

  return date.toLocaleString();
}

export function formatDuration(millis: number | null): string {
  const duration = typeof millis === 'number' ? millis : null;
  if (duration === null || duration < 0) return 'N/A';
  if (duration === 0) return '0h 0m';

  const hours = Math.floor(duration / 3600000);
  const minutes = Math.floor((duration % 3600000) / 60000);
  if (hours >= 24) {
    const days = Math.floor(hours / 24);
    return `${days}d ${hours % 24}h`;
  }
  return `${hours}h ${minutes}m`;
}

export type CSVExportOptions = {
  includeBom?: boolean;
};

export function toCSV(data: Record<string, unknown>[]): string {
  if (!data.length) return '';

  const headers = Object.keys(data[0]);
  const csvRows = [headers.join(',')];

  for (const row of data) {
    const values = headers.map((header) => {
      const val = row[header];
      const escaped = String(val ?? '').replace(/"/g, '""');
      return `"${escaped}"`;
    });
    csvRows.push(values.join(','));
  }

  return csvRows.join('\n');
}

export function exportToCSV(
  data: Record<string, unknown>[],
  filename: string,
  options: CSVExportOptions = {}
) {
  if (!data || !data.length) {
    console.warn('No data to export');
    return;
  }

  const csvString = toCSV(data);
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
}
