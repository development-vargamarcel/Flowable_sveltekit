import { describe, expect, it } from 'vitest';
import { formatDate, formatDuration, getVariableDisplay, toCSV } from './utils';

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
  });

  describe('formatDate', () => {
    it('returns Invalid date for invalid inputs', () => {
      expect(formatDate('not-a-date')).toBe('Invalid date');
    });
  });

  describe('formatDuration', () => {
    it('handles zero and negative values safely', () => {
      expect(formatDuration(0)).toBe('0h 0m');
      expect(formatDuration(-1000)).toBe('N/A');
    });

    it('renders durations longer than one day', () => {
      expect(formatDuration(25 * 3600000)).toBe('1d 1h');
    });
  });

  describe('toCSV', () => {
    it('escapes CSV values and keeps column order from first row', () => {
      const csv = toCSV([
        { name: 'Alice', note: 'hello, "quoted" text' },
        { name: 'Bob', note: null }
      ]);

      expect(csv).toBe('name,note\n"Alice","hello, ""quoted"" text"\n"Bob",""');
    });
  });
});
