import { type ClassValue, clsx } from 'clsx';
import { twMerge } from 'tailwind-merge';

export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs));
}

export * from './utils/date';
export * from './utils/csv';
export * from './utils/currency';
export * from './utils/variables';
