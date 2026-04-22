import { describe, it, expect } from 'vitest';
import { getVariableDisplay, truncateText, normalizeVariableLabel, normalizeVariableValue } from './variables';

describe('truncateText', () => {
  it('truncates long text', () => {
    expect(truncateText('Hello World', 5)).toBe('Hell…');
  });

  it('keeps short text as is', () => {
    expect(truncateText('Hello', 10)).toBe('Hello');
  });

  it('normalizes whitespace', () => {
    expect(truncateText('Hello    World', 20)).toBe('Hello World');
  });
});

describe('normalizeVariableLabel', () => {
  it('converts snake_case to Title Case', () => {
    expect(normalizeVariableLabel('first_name')).toBe('First Name');
  });

  it('uses labelMap if provided', () => {
    expect(normalizeVariableLabel('first_name', { first_name: 'Given Name' })).toBe('Given Name');
  });
});

describe('normalizeVariableValue', () => {
  it('handles booleans', () => {
    expect(normalizeVariableValue(true, 10)).toBe('Yes');
    expect(normalizeVariableValue(false, 10)).toBe('No');
  });

  it('handles null/undefined', () => {
    expect(normalizeVariableValue(null, 10)).toBe('N/A');
    expect(normalizeVariableValue(undefined, 10)).toBe('N/A');
  });

  it('handles objects', () => {
    expect(normalizeVariableValue({ a: 1 }, 20)).toBe('{"a":1}');
  });
});

describe('getVariableDisplay', () => {
  it('returns empty array for null variables', () => {
    expect(getVariableDisplay(null)).toEqual([]);
  });

  it('maps common variables correctly', () => {
    const vars = { amount: 100, category: 'Travel' };
    const display = getVariableDisplay(vars);
    expect(display).toContainEqual({ label: 'Amount', value: '$100.00' });
    expect(display).toContainEqual({ label: 'Category', value: 'Travel' });
  });

  it('limits items based on maxItems', () => {
    const vars = { a: 1, b: 2, c: 3, d: 4 };
    const display = getVariableDisplay(vars, { maxItems: 2 });
    expect(display.length).toBe(2);
  });

  it('shows "More" if showRemainingCount is true', () => {
    const vars = { a: 1, b: 2, c: 3 };
    const display = getVariableDisplay(vars, { maxItems: 2, showRemainingCount: true });
    expect(display).toContainEqual({ label: 'More', value: '+2 more' });
  });
});
