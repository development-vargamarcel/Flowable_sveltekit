import { type ClassValue, clsx } from 'clsx';
import { twMerge } from 'tailwind-merge';

export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs));
}

export function getVariableDisplay(variables: Record<string, unknown> | undefined | null): { label: string; value: string }[] {
  const displays: { label: string; value: string }[] = [];

  if (!variables) return displays;

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

export function formatDate(dateStr: string): string {
    if (!dateStr) return 'N/A';
    return new Date(dateStr).toLocaleString();
}

export function formatDuration(millis: number | null): string {
    if (!millis) return 'N/A';
    const hours = Math.floor(millis / 3600000);
    const minutes = Math.floor((millis % 3600000) / 60000);
    if (hours > 24) {
        const days = Math.floor(hours / 24);
        return `${days}d ${hours % 24}h`;
    }
    return `${hours}h ${minutes}m`;
}
