import { describe, expect, it } from 'vitest';
import {
  exportToCSV,
  formatCurrency,
  formatDate,
  formatDuration,
  getVariableDisplay,
  toCSV
} from './utils';

describe('utils', () => {
  describe('getVariableDisplay', () => {
    it('formats numeric amount and truncates to three fields', () => {
      const output = getVariableDisplay({
        amount: '123.4',
        category: 'Travel',
        leaveType: 'Annual',
        title: 'Should not be included due to limit'
      });

      expect(output).toEqual([
        { label: 'Amount', value: '$123.40' },
        { label: 'Category', value: 'Travel' },
        { label: 'Type', value: 'Annual' }
      ]);
    });

    it('skips invalid amount values', () => {
      const output = getVariableDisplay({
        amount: 'invalid-number',
        title: 'Request'
      });

      expect(output).toEqual([{ label: 'Title', value: 'Request' }]);
    });

    it('supports priority labels and custom limits', () => {
      const output = getVariableDisplay(
        {
          amount: 50,
          category: 'A very long category name that should be truncated for compact display',
          priority: 82,
          title: 'ignored due to max items'
        },
        { maxItems: 3, maxTextLength: 22 }
      );

      expect(output).toEqual([
        { label: 'Amount', value: '$50.00' },
        { label: 'Category', value: 'A very long category …' },
        { label: 'Priority', value: 'High (82)' }
      ]);
    });
  });

  describe('formatCurrency', () => {
    it('formats known numeric values and falls back for invalid values', () => {
      expect(formatCurrency(123.456, { locale: 'en-US', currency: 'USD' })).toBe('$123.46');
      expect(formatCurrency('bad-input')).toBe('N/A');
    });
  });

  describe('formatDate', () => {
    it('returns Invalid date for invalid inputs', () => {
      expect(formatDate('not-a-date')).toBe('Invalid date');
    });

    it('supports date-only and relative date formats', () => {
      expect(formatDate('2024-01-01T00:00:00Z', { mode: 'date', locale: 'en-US' })).toMatch(
        /1\/1\/2024/
      );

      expect(
        formatDate(new Date(Date.now() - 45 * 60000).toISOString(), { mode: 'relative' })
      ).toBe('45m ago');
    });
  });

  describe('formatDuration', () => {
    it('handles zero and negative values safely', () => {
      expect(formatDuration(0)).toBe('0h 0m');
      expect(formatDuration(-1000)).toBe('N/A');
    });

    it('renders durations longer than one day', () => {
      expect(formatDuration(25 * 3600000)).toBe('1d 1h 0m');
    });

    it('supports compact mode and optional seconds', () => {
      expect(formatDuration(3723000, { compact: true, includeSeconds: true })).toBe('1h 2m 3s');
    });
  });

  describe('toCSV', () => {
    it('escapes CSV values and includes headers from all rows', () => {
      const csv = toCSV([
        { name: 'Alice', note: 'hello, "quoted" text' },
        { name: 'Bob', note: null, team: 'Ops' }
      ]);

      expect(csv).toBe('name,note,team\n"Alice","hello, ""quoted"" text",""\n"Bob","","Ops"');
    });

    it('supports custom delimiters and no-header mode', () => {
      const csv = toCSV(
        [
          { name: 'Alice', age: 20 },
          { name: 'Bob', age: 21 }
        ],
        {
          includeHeader: false,
          delimiter: ';'
        }
      );

      expect(csv).toBe('"Alice";"20"\n"Bob";"21"');
    });
  });

  describe('exportToCSV', () => {
    it('returns a failure result when called in non-browser context', () => {
      const result = exportToCSV([{ name: 'Alice' }], 'users.csv');
      expect(result).toEqual({
        success: false,
        reason: 'CSV export is not supported in this environment'
      });
    });
  });
});
