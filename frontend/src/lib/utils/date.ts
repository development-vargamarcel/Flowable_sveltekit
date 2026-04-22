import type { FormatterLocale } from './currency';

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
