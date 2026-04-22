import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { formatDate, formatDuration } from './date';

describe('formatDate', () => {
  it('returns fallback for empty date string', () => {
    expect(formatDate('')).toBe('N/A');
    expect(formatDate('', { fallback: 'Never' })).toBe('Never');
  });

  it('returns "Invalid date" for malformed strings', () => {
    expect(formatDate('not-a-date')).toBe('Invalid date');
  });

  it('formats absolute dates', () => {
    const dateStr = '2023-10-27T10:00:00Z';
    // Use UTC to avoid locale-specific differences in CI
    const formatted = formatDate(dateStr, { mode: 'date', useUTC: true, locale: 'en-US' });
    expect(formatted).toBe('10/27/2023');
  });

  it('formats relative dates', () => {
    const now = new Date('2023-10-27T10:00:00Z').getTime();
    vi.useFakeTimers();
    vi.setSystemTime(now);

    const fiveMinutesAgo = new Date(now - 5 * 60 * 1000).toISOString();
    expect(formatDate(fiveMinutesAgo, { mode: 'relative', locale: 'en-US' })).toBe('5 minutes ago');

    vi.useRealTimers();
  });
});

describe('formatDuration', () => {
  it('returns fallback for null/negative values', () => {
    expect(formatDuration(null)).toBe('N/A');
    expect(formatDuration(-100)).toBe('N/A');
  });

  it('formats zero duration', () => {
    expect(formatDuration(0)).toBe('0h 0m');
    expect(formatDuration(0, { includeSeconds: true })).toBe('0h 0m 0s');
  });

  it('formats basic durations', () => {
    const millis = (1 * 3600 + 30 * 60) * 1000; // 1h 30m
    expect(formatDuration(millis)).toBe('1h 30m');
  });

  it('formats compact durations', () => {
    const millis = (2 * 86400 + 5 * 3600 + 10 * 60) * 1000; // 2d 5h 10m
    expect(formatDuration(millis, { compact: true })).toBe('2d 5h 10m');
  });
});
