import { describe, expect, it, vi } from 'vitest';
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

    it('supports dynamic keys, label mapping and remaining count summary', () => {
      const output = getVariableDisplay(
        {
          amount: 5,
          approved: true,
          custom_field: 'value',
          team: 'Operations',
          metadata: { code: 'A1' }
        },
        {
          maxItems: 4,
          showRemainingCount: true,
          preferredOrder: ['approved', 'team'],
          labelMap: { custom_field: 'Custom Field' }
        }
      );

      expect(output).toEqual([
        { label: 'Amount', value: '$5.00' },
        { label: 'Approved', value: 'Yes' },
        { label: 'Team', value: 'Operations' },
        { label: 'More', value: '+2 more' }
      ]);
    });
  });

  describe('formatCurrency', () => {
    it('formats known numeric values and falls back for invalid values', () => {
      expect(formatCurrency(123.456, { locale: 'en-US', currency: 'USD' })).toBe('$123.46');
      expect(formatCurrency('bad-input')).toBe('N/A');
    });

    it('supports accounting style and comma-delimited numeric strings', () => {
      expect(
        formatCurrency('-1,200.5', {
          locale: 'en-US',
          currency: 'USD',
          currencySign: 'accounting',
          minimumFractionDigits: 1,
          maximumFractionDigits: 1
        })
      ).toBe('($1,200.5)');
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
        formatDate(new Date(Date.now() - 45 * 60000).toISOString(), {
          mode: 'relative',
          locale: 'en-US'
        })
      ).toBe('45 minutes ago');
    });

    it('supports UTC output for deterministic formatting', () => {
      expect(
        formatDate('2024-01-01T03:45:00Z', {
          mode: 'time',
          locale: 'en-US',
          useUTC: true,
          hour12: false
        })
      ).toBe('03:45:00');
    });

    it('supports month and year relative formatting for long distances', () => {
      expect(
        formatDate(new Date(Date.now() - 45 * 24 * 60 * 60 * 1000).toISOString(), {
          mode: 'relative',
          locale: 'en-US'
        })
      ).toContain('month');

      expect(
        formatDate(new Date(Date.now() - 800 * 24 * 60 * 60 * 1000).toISOString(), {
          mode: 'relative',
          locale: 'en-US'
        })
      ).toContain('year');
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
      expect(formatDuration(3723000, { compact: true, includeSeconds: true, padUnits: true })).toBe(
        '0d 1h 2m 3s'
      );
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
          delimiter: ';',
          quoteAllFields: false
        }
      );

      expect(csv).toBe('Alice;20\nBob;21');
    });

    it('serializes objects, null values and sorted headers', () => {
      const csv = toCSV(
        [
          { z: null, a: { nested: true }, b: true },
          { z: 'x', a: { nested: false }, b: false }
        ],
        { nullValue: 'NULL', sortHeaders: true }
      );

      expect(csv).toBe(
        'a,b,z\n"{""nested"":true}","true","NULL"\n"{""nested"":false}","false","x"'
      );
    });

    it('serializes date instances and applies custom null values', () => {
      const csv = toCSV(
        [
          { createdAt: new Date('2024-01-01T00:00:00.000Z'), status: null },
          { createdAt: new Date('2024-01-02T00:00:00.000Z'), status: 'done' }
        ],
        { nullValue: 'none' }
      );

      expect(csv).toContain('2024-01-01T00:00:00.000Z');
      expect(csv).toContain('"none"');
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

    it('returns browser-safe sanitized filename when export succeeds', () => {
      const createObjectURL = vi.fn(() => 'blob:download');
      const revokeObjectURL = vi.fn();
      const click = vi.fn();

      Object.defineProperty(window, 'URL', {
        value: { createObjectURL, revokeObjectURL },
        configurable: true
      });

      const nativeCreateElement = document.createElement.bind(document);
      const createElementSpy = vi.spyOn(document, 'createElement').mockImplementation((tag) => {
        if (tag === 'a') {
          return {
            setAttribute: vi.fn(),
            click,
            remove: vi.fn()
          } as unknown as HTMLAnchorElement;
        }
        return nativeCreateElement(tag);
      });

      const appendChildSpy = vi
        .spyOn(document.body, 'appendChild')
        .mockImplementation((node) => node);
      const removeChildSpy = vi
        .spyOn(document.body, 'removeChild')
        .mockImplementation((node) => node);

      const result = exportToCSV([{ name: 'Alice' }], 'report:/2024?.csv', {
        filenamePrefix: 'team report '
      });

      expect(result).toEqual({ success: true, filename: 'team_report_report-2024-.csv' });
      expect(createObjectURL).toHaveBeenCalled();
      expect(revokeObjectURL).toHaveBeenCalledWith('blob:download');
      expect(click).toHaveBeenCalled();

      createElementSpy.mockRestore();
      appendChildSpy.mockRestore();
      removeChildSpy.mockRestore();
    });
  });
});
