import { type ClassValue, clsx } from 'clsx';
import { twMerge } from 'tailwind-merge';

export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs));
}

export function getVariableDisplay(variables: Record<string, unknown>): { label: string; value: string }[] {
  const displays: { label: string; value: string }[] = [];

  if (variables.amount !== undefined) {
    displays.push({ label: 'Amount', value: `$${Number(variables.amount).toFixed(2)}` });
  }
  if (variables.category) {
    displays.push({ label: 'Category', value: String(variables.category) });
  }
  if (variables.leaveType) {
    displays.push({ label: 'Type', value: String(variables.leaveType) });
  }
  if (variables.days !== undefined) {
    displays.push({ label: 'Days', value: String(variables.days) });
  }
  if (variables.title) {
    displays.push({ label: 'Title', value: String(variables.title) });
  }
  if (variables.employeeName) {
    displays.push({ label: 'From', value: String(variables.employeeName) });
  }

  return displays.slice(0, 3);
}
