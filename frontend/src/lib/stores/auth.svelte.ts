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
    this.user = user;
    this.loading = false;
  }

  setLoading(loading: boolean) {
    this.loading = loading;
  }

  clear() {
    this.user = null;
    this.loading = false;
  }

  // Add login method here to fix types
  async login(username, password) {
      this.setLoading(true);
      try {
          const user = await api.login(username, password);
          this.setUser(user);
          return user;
      } finally {
          this.setLoading(false);
      }
  }

  // Add logout method here to fix types
  async logout() {
      this.setLoading(true);
      try {
          await api.logout();
      } finally {
          this.clear();
      }
  }
}

export const authStore = new AuthStore();
