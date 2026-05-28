import { api } from '$lib/api/client';
import { logger } from '$lib/utils/logger';
import type { User } from '$lib/types';

// Svelte 5 runes-based auth store
class AuthStore {
  user = $state<User | null>(null);
  loading = $state(true);

  get isAuthenticated() {
    return this.user !== null;
  }

  get isUser() {
    return this.user?.roles.includes('USER') ?? false;
  }

  get isSupervisor() {
    return this.user?.roles.includes('SUPERVISOR') ?? false;
  }

  get isExecutive() {
    return this.user?.roles.includes('EXECUTIVE') ?? false;
  }

  // Expose the username directly for easier access or map it from user object
  get username() {
    return this.user?.username || '';
  }

  setUser(user: User | null) {
    logger.debug('Auth store user updated', { username: user?.username });
    this.user = user;
    this.loading = false;
  }

  setLoading(loading: boolean) {
    this.loading = loading;
  }

  clear() {
    logger.debug('Auth store session cleared');
    this.user = null;
    this.loading = false;
  }

  // Add login method here to fix types
  async login(username: string, password: string) {
    logger.debug('Auth store login started', { username });
    this.setLoading(true);
    try {
      const response = await api.login({ username, password });
      this.setUser(response.user);
      return response.user;
    } finally {
      this.setLoading(false);
    }
  }

  // Add logout method here to fix types
  async logout() {
    logger.debug('Auth store logout started');
    this.setLoading(true);
    try {
      await api.logout();
    } finally {
      this.clear();
    }
  }
}

export const authStore = new AuthStore();
