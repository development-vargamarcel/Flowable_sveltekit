export type FormatterLocale = string | string[] | undefined;

export function toFiniteNumber(value: unknown): number | null {
  if (typeof value === 'number' && Number.isFinite(value)) {
    return value;
  }

  if (typeof value === 'string') {
    const trimmed = value.trim();
    if (!trimmed) return null;

    // Handle common numeric formats from user input and CSV imports.
    const normalized = trimmed.replace(/,/g, '');
    const parsed = Number(normalized);
    return Number.isFinite(parsed) ? parsed : null;
  }

  return null;
}

export function formatCurrency(
  value: unknown,
  options: {
    locale?: FormatterLocale;
    currency?: string;
    fallback?: string;
    minimumFractionDigits?: number;
    maximumFractionDigits?: number;
    currencySign?: 'standard' | 'accounting';
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
      minimumFractionDigits: options.minimumFractionDigits ?? 2,
      maximumFractionDigits: options.maximumFractionDigits ?? 2,
      currencySign: options.currencySign ?? 'standard'
    }).format(amount);
  } catch {
    const sign = amount < 0 ? '-' : '';
    return `${sign}$${Math.abs(amount).toFixed(options.maximumFractionDigits ?? 2)}`;
  }
}
