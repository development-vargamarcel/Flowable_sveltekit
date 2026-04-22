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

export function serializeCsvValue(value: unknown, nullValue: string): string {
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

export function sanitizeFilename(filename: string, prefix?: string): string {
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
