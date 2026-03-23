/* eslint-disable no-console */
import type { ProcessDefinition, ProcessInstance, Dashboard, Page } from '$lib/types';

class ProcessStore {
  // Process definitions (deployed processes)
  definitions = $state<ProcessDefinition[]>([]);

  // My process instances
  myInstances = $state<Page<ProcessInstance> | null>(null);

  // Dashboard data
  dashboard = $state<Dashboard | null>(null);

  private definitionsLoading = false;
  private definitionsError: string | null = null;
  private definitionsLastFetched: number | null = null;
  private definitionsRequest: Promise<ProcessDefinition[]> | null = null;

  private myInstancesLoading = false;
  private myInstancesError: string | null = null;
  private myInstancesLastFetched: number | null = null;
  private myInstancesRequest: Promise<Page<ProcessInstance>> | null = null;

  private dashboardLoading = false;
  private dashboardError: string | null = null;
  private dashboardLastFetched: number | null = null;
  private dashboardRequest: Promise<Dashboard> | null = null;

  private readonly CACHE_TTL = 30000;

  // Listeners for process changes
  private changeListeners = new Set<() => void>();

  private isCacheValid(lastFetched: number | null): boolean {
    if (!lastFetched) return false;
    return Date.now() - lastFetched < this.CACHE_TTL;
  }

  private normalizeError(err: unknown, fallbackMessage: string): string {
    return err instanceof Error ? err.message : fallbackMessage;
  }

  private async loadCached<T>({
    label,
    currentValue,
    hasValue,
    getRequest,
    setRequest,
    setLoading,
    setError,
    getLastFetched,
    setLastFetched,
    updateValue,
    fetchFn,
    forceRefresh,
    fallbackError
  }: {
    label: string;
    currentValue: T | null;
    hasValue: (value: T | null) => value is T;
    getRequest: () => Promise<T> | null;
    setRequest: (value: Promise<T> | null) => void;
    setLoading: (value: boolean) => void;
    setError: (value: string | null) => void;
    getLastFetched: () => number | null;
    setLastFetched: (value: number | null) => void;
    updateValue: (value: T) => void;
    fetchFn: () => Promise<T>;
    forceRefresh: boolean;
    fallbackError: string;
  }): Promise<T> {
    if (!forceRefresh && hasValue(currentValue) && this.isCacheValid(getLastFetched())) {
      console.log(`[ProcessStore] Using cached ${label}`);
      return currentValue;
    }

    const existingRequest = getRequest();
    if (existingRequest) {
      console.log(`[ProcessStore] ${label} load already in progress`);
      return existingRequest;
    }

    console.log(`[ProcessStore] Loading ${label}`);
    setLoading(true);
    setError(null);

    const request = (async () => {
      try {
        const data = await fetchFn();
        updateValue(data);
        setLastFetched(Date.now());
        return data;
      } catch (err) {
        console.error(`[ProcessStore] Failed to load ${label}:`, err);
        setError(this.normalizeError(err, fallbackError));
        throw err;
      } finally {
        setLoading(false);
        setRequest(null);
      }
    })();

    setRequest(request);
    return request;
  }

  onProcessChange(listener: () => void): () => void {
    this.changeListeners.add(listener);
    return () => {
      this.changeListeners.delete(listener);
    };
  }

  private notifyChange() {
    this.changeListeners.forEach((listener) => listener());
  }

  async loadDefinitions(
    fetchFn: () => Promise<ProcessDefinition[]>,
    forceRefresh = false
  ): Promise<ProcessDefinition[]> {
    return this.loadCached({
      label: 'definitions',
      currentValue: this.definitions,
      hasValue: (value): value is ProcessDefinition[] => value !== null && value.length > 0,
      getRequest: () => this.definitionsRequest,
      setRequest: (value) => {
        this.definitionsRequest = value;
      },
      setLoading: (value) => {
        this.definitionsLoading = value;
      },
      setError: (value) => {
        this.definitionsError = value;
      },
      getLastFetched: () => this.definitionsLastFetched,
      setLastFetched: (value) => {
        this.definitionsLastFetched = value;
      },
      updateValue: (value) => {
        this.definitions = value;
      },
      fetchFn,
      forceRefresh,
      fallbackError: 'Failed to load processes'
    });
  }

  async loadMyInstances(
    fetchFn: () => Promise<Page<ProcessInstance>>,
    forceRefresh = false
  ): Promise<Page<ProcessInstance>> {
    return this.loadCached({
      label: 'my instances',
      currentValue: this.myInstances,
      hasValue: (value): value is Page<ProcessInstance> => value !== null,
      getRequest: () => this.myInstancesRequest,
      setRequest: (value) => {
        this.myInstancesRequest = value;
      },
      setLoading: (value) => {
        this.myInstancesLoading = value;
      },
      setError: (value) => {
        this.myInstancesError = value;
      },
      getLastFetched: () => this.myInstancesLastFetched,
      setLastFetched: (value) => {
        this.myInstancesLastFetched = value;
      },
      updateValue: (value) => {
        this.myInstances = value;
      },
      fetchFn,
      forceRefresh,
      fallbackError: 'Failed to load process instances'
    });
  }

  async loadDashboard(fetchFn: () => Promise<Dashboard>, forceRefresh = false): Promise<Dashboard> {
    return this.loadCached({
      label: 'dashboard',
      currentValue: this.dashboard,
      hasValue: (value): value is Dashboard => value !== null,
      getRequest: () => this.dashboardRequest,
      setRequest: (value) => {
        this.dashboardRequest = value;
      },
      setLoading: (value) => {
        this.dashboardLoading = value;
      },
      setError: (value) => {
        this.dashboardError = value;
      },
      getLastFetched: () => this.dashboardLastFetched,
      setLastFetched: (value) => {
        this.dashboardLastFetched = value;
      },
      updateValue: (value) => {
        this.dashboard = value;
      },
      fetchFn,
      forceRefresh,
      fallbackError: 'Failed to load dashboard'
    });
  }

  invalidateAll() {
    console.log('[ProcessStore] Invalidating all caches');
    this.definitionsLastFetched = null;
    this.myInstancesLastFetched = null;
    this.dashboardLastFetched = null;
    this.notifyChange();
  }

  invalidateDefinitions() {
    this.definitionsLastFetched = null;
    this.notifyChange();
  }

  invalidateInstances() {
    this.myInstancesLastFetched = null;
    this.dashboardLastFetched = null;
    this.notifyChange();
  }

  addDeployedProcess(process: ProcessDefinition) {
    this.definitions = [...this.definitions, process];
    this.invalidateDefinitions();
  }

  removeProcess(processId: string) {
    this.definitions = this.definitions.filter((p) => p.id !== processId);
    this.invalidateDefinitions();
  }

  get groupedDefinitions() {
    const groups = new Map<string, ProcessDefinition[]>();

    this.definitions.forEach((process) => {
      if (!groups.has(process.key)) {
        groups.set(process.key, []);
      }
      groups.get(process.key)?.push(process);
    });

    groups.forEach((versions) => {
      versions.sort((a, b) => (b.version || 0) - (a.version || 0));
    });

    return Array.from(groups.entries()).map(([key, versions]) => ({
      key,
      versions,
      latest: versions[0]
    }));
  }

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
    this.definitionsRequest = null;
    this.myInstancesRequest = null;
    this.dashboardRequest = null;
    this.definitionsLoading = false;
    this.myInstancesLoading = false;
    this.dashboardLoading = false;
  }
}

export const processStore = new ProcessStore();
