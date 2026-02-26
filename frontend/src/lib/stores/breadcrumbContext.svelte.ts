/**
 * Breadcrumb Context Store
 *
 * Provides dynamic context labels for breadcrumbs.
 * Pages can set custom labels for route segments (like task names, process names)
 * instead of showing raw IDs.
 */

// Context labels keyed by route segment (e.g., task ID -> task name)
let contextLabels = $state<Record<string, string>>({});

/**
 * Set a context label for a route segment
 * @param segment - The route segment (usually an ID)
 * @param label - The human-readable label to display
 */
export function setBreadcrumbLabel(segment: string, label: string): void {
  contextLabels = { ...contextLabels, [segment]: label };
}

/**
 * Set multiple context labels at once
 */
export function setBreadcrumbLabels(labels: Record<string, string>): void {
  contextLabels = { ...contextLabels, ...labels };
}

/**
 * Get a context label for a segment, or return undefined
 */
export function getBreadcrumbLabel(segment: string): string | undefined {
  return contextLabels[segment];
}

/**
 * Clear a specific context label
 */
export function clearBreadcrumbLabel(segment: string): void {
  const { [segment]: _, ...rest } = contextLabels;
  contextLabels = rest;
}

/**
 * Clear all context labels
 */
export function clearAllBreadcrumbLabels(): void {
  contextLabels = {};
}

/**
 * Get all current context labels (for debugging)
 */
export function getAllBreadcrumbLabels(): Record<string, string> {
  return { ...contextLabels };
}
