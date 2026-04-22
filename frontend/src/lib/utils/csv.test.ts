import { describe, it, expect } from 'vitest';
import { toCSV, serializeCsvValue } from './csv';

describe('serializeCsvValue', () => {
  it('handles null and undefined', () => {
    expect(serializeCsvValue(null, 'N/A')).toBe('N/A');
    expect(serializeCsvValue(undefined, '')).toBe('');
  });

  it('handles booleans', () => {
    expect(serializeCsvValue(true, '')).toBe('true');
    expect(serializeCsvValue(false, '')).toBe('false');
  });

  it('handles dates', () => {
    const date = new Date('2023-10-27T10:00:00Z');
    expect(serializeCsvValue(date, '')).toBe(date.toISOString());
  });

  it('handles objects', () => {
    const obj = { key: 'value' };
    expect(serializeCsvValue(obj, '')).toBe(JSON.stringify(obj));
  });
});

describe('toCSV', () => {
  const data = [
    { name: 'John Doe', age: 30, city: 'New York' },
    { name: 'Jane Smith', age: 25, city: 'London' }
  ];

  it('generates CSV with headers', () => {
    const csv = toCSV(data, { quoteAllFields: false });
    const lines = csv.split('\n');
    expect(lines[0]).toBe('name,age,city');
    expect(lines[1]).toBe('John Doe,30,New York');
  });

  it('handles custom delimiters', () => {
    const csv = toCSV(data, { delimiter: ';', quoteAllFields: false });
    const lines = csv.split('\n');
    expect(lines[0]).toBe('name;age;city');
    expect(lines[1]).toBe('John Doe;30;New York');
  });

  it('escapes quotes in values', () => {
    const complexData = [{ name: 'John "The Boss" Doe' }];
    const csv = toCSV(complexData);
    expect(csv).toContain('"John ""The Boss"" Doe"');
  });

  it('omits headers if requested', () => {
    const csv = toCSV(data, { includeHeader: false, quoteAllFields: false });
    const lines = csv.split('\n');
    expect(lines[0]).toBe('John Doe,30,New York');
  });
});
