/* eslint-disable no-console */
import type { User } from '$lib/types';
import { api } from '$lib/api/client';

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
    console.log('[AuthStore] Setting user:', user?.username);
    this.user = user;
    this.loading = false;
  }

  setLoading(loading: boolean) {
    this.loading = loading;
  }

  clear() {
    console.log('[AuthStore] Clearing session');
    this.user = null;
    this.loading = false;
  }

  // Add login method here to fix types
  async login(username: string, password: string) {
    console.log('[AuthStore] Logging in user:', username);
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
    console.log('[AuthStore] Logging out');
    this.setLoading(true);
    try {
      await api.logout();
    } finally {
      this.clear();
    }
  }
}

export const authStore = new AuthStore();
