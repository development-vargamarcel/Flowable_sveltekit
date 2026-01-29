import type { ProcessDefinition, ProcessInstance, Dashboard, Page } from '$lib/types';

/**
 * Centralized process store for managing process definitions and instances.
 * Ensures synchronization across all components that consume process data.
 *
 * This store provides:
 * - Cached process definitions with automatic invalidation
 * - Dashboard data caching with SWR (stale-while-revalidate) pattern
 * - Event-driven updates when processes are deployed/deleted
 * - Stale data detection with background refresh
 * - Optimistic updates for better perceived performance
 */
class ProcessStore {
  // Process definitions (deployed processes)
  definitions = $state<ProcessDefinition[]>([]);
  definitionsLoading = $state(false);
  definitionsRefreshing = $state(false);
  definitionsError = $state<string | null>(null);
  definitionsLastFetched = $state<number | null>(null);

  // My process instances
  myInstances = $state<Page<ProcessInstance> | null>(null);
  myInstancesLoading = $state(false);
  myInstancesRefreshing = $state(false);
  myInstancesError = $state<string | null>(null);
  myInstancesLastFetched = $state<number | null>(null);

  // Dashboard data
  dashboard = $state<Dashboard | null>(null);
  dashboardLoading = $state(false);
  dashboardRefreshing = $state(false);
  dashboardError = $state<string | null>(null);
  dashboardLastFetched = $state<number | null>(null);

  // Cache configuration (in milliseconds)
  private readonly CACHE_TTL = 30000; // 30 seconds - data is fresh
  private readonly STALE_THRESHOLD = 5000; // 5 seconds - trigger background refresh
  private readonly MAX_AGE = 300000; // 5 minutes - force refresh even with errors

  // Listeners for process changes
  private changeListeners = new Set<() => void>();

  /**
   * Check if cached data is still valid
   */
  private isCacheValid(lastFetched: number | null): boolean {
    if (!lastFetched) return false;
    return Date.now() - lastFetched < this.CACHE_TTL;
  }

  /**
   * Check if data should be refreshed in the background
   */
  private isStale(lastFetched: number | null): boolean {
    if (!lastFetched) return true;
    return Date.now() - lastFetched > this.STALE_THRESHOLD;
  }

  /**
   * Subscribe to process changes
   */
  onProcessChange(listener: () => void): () => void {
    this.changeListeners.add(listener);
    return () => {
      this.changeListeners.delete(listener);
    };
  }

  /**
   * Notify all listeners that processes have changed
   */
  private notifyChange() {
    this.changeListeners.forEach((listener) => listener());
  }

  /**
   * Load process definitions with caching
   */
  async loadDefinitions(
    fetchFn: () => Promise<ProcessDefinition[]>,
    forceRefresh = false
  ): Promise<ProcessDefinition[]> {
    // Return cached data if valid and not forcing refresh
    if (!forceRefresh && this.isCacheValid(this.definitionsLastFetched)) {
      return this.definitions;
    }

    this.definitionsLoading = true;
    this.definitionsError = null;

    try {
      const data = await fetchFn();
      this.definitions = data;
      this.definitionsLastFetched = Date.now();
      return data;
    } catch (err) {
      const message = err instanceof Error ? err.message : 'Failed to load processes';
      this.definitionsError = message;
      throw err;
    } finally {
      this.definitionsLoading = false;
    }
  }

  /**
   * Load my process instances with caching
   */
  async loadMyInstances(
    fetchFn: () => Promise<Page<ProcessInstance>>,
    forceRefresh = false
  ): Promise<Page<ProcessInstance>> {
    if (!forceRefresh && this.myInstances && this.isCacheValid(this.myInstancesLastFetched)) {
      return this.myInstances;
    }

    this.myInstancesLoading = true;
    this.myInstancesError = null;

    try {
      const data = await fetchFn();
      this.myInstances = data;
      this.myInstancesLastFetched = Date.now();
      return data;
    } catch (err) {
      const message = err instanceof Error ? err.message : 'Failed to load process instances';
      this.myInstancesError = message;
      throw err;
    } finally {
      this.myInstancesLoading = false;
    }
  }

  /**
   * Load dashboard data with caching
   */
  async loadDashboard(
    fetchFn: () => Promise<Dashboard>,
    forceRefresh = false
  ): Promise<Dashboard> {
    if (!forceRefresh && this.dashboard && this.isCacheValid(this.dashboardLastFetched)) {
      return this.dashboard;
    }

    this.dashboardLoading = true;
    this.dashboardError = null;

    try {
      const data = await fetchFn();
      this.dashboard = data;
      this.dashboardLastFetched = Date.now();
      return data;
    } catch (err) {
      const message = err instanceof Error ? err.message : 'Failed to load dashboard';
      this.dashboardError = message;
      throw err;
    } finally {
      this.dashboardLoading = false;
    }
  }

  /**
   * Invalidate all caches - call after deploying or deleting a process
   */
  invalidateAll() {
    this.definitionsLastFetched = null;
    this.myInstancesLastFetched = null;
    this.dashboardLastFetched = null;
    this.notifyChange();
  }

  /**
   * Invalidate only definitions cache
   */
  invalidateDefinitions() {
    this.definitionsLastFetched = null;
    this.notifyChange();
  }

  /**
   * Invalidate only instances cache
   */
  invalidateInstances() {
    this.myInstancesLastFetched = null;
    this.dashboardLastFetched = null;
    this.notifyChange();
  }

  /**
   * Add a newly deployed process definition to the store immediately
   * This provides optimistic update while cache is invalidated
   */
  addDeployedProcess(process: ProcessDefinition) {
    // We append the new process to the list.
    // Since we now support multiple versions, we just add it.
    // The grouping logic will handle version sorting.
    this.definitions = [...this.definitions, process];
    this.invalidateDefinitions();
  }

  /**
   * Remove a process definition from the store
   */
  removeProcess(processId: string) {
    // If we are showing all versions, removing by ID just removes that version.
    // However, the backend logic for deletion might cascade to all versions if we are not careful
    // or if the user requested cascade.
    // The current UI logic in manage page calls deleteProcess with cascade=false usually,
    // or it might intend to delete the deployment which removes all contained processes.
    // For safety, let's just filter out by ID.
    this.definitions = this.definitions.filter((p) => p.id !== processId);
    this.invalidateDefinitions();
  }

  /**
   * Get grouped processes by key (for manage page)
   */
  get groupedDefinitions() {
    const groups = new Map<string, ProcessDefinition[]>();

    this.definitions.forEach((process) => {
      if (!groups.has(process.key)) {
        groups.set(process.key, []);
      }
      groups.get(process.key)?.push(process);
    });

    // Sort versions within each group (highest version first)
    groups.forEach((versions) => {
      versions.sort((a, b) => (b.version || 0) - (a.version || 0));
    });

    return Array.from(groups.entries()).map(([key, versions]) => ({
      key,
      versions,
      latest: versions[0]
    }));
  }

  /**
   * Check if definitions should be refreshed
   */
  get shouldRefreshDefinitions(): boolean {
    return this.isStale(this.definitionsLastFetched);
  }

  /**
   * Check if dashboard should be refreshed
   */
  get shouldRefreshDashboard(): boolean {
    return this.isStale(this.dashboardLastFetched);
  }

  /**
   * Clear all data (e.g., on logout)
   */
  clear() {
    this.definitions = [];
    this.myInstances = null;
    this.dashboard = null;
    this.definitionsLastFetched = null;
    this.myInstancesLastFetched = null;
    this.dashboardLastFetched = null;
    this.definitionsError = null;
    this.myInstancesError = null;
    this.dashboardError = null;
  }
}

export const processStore = new ProcessStore();
