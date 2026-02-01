import { fetchApi } from './core';
import type { LoginRequest, RegisterRequest, User } from '$lib/types';

export const authApi = {
  async login(credentials: LoginRequest): Promise<{ message: string; user: User }> {
    return fetchApi('/api/auth/login', {
      method: 'POST',
      body: JSON.stringify(credentials)
    });
  },

  async register(request: RegisterRequest): Promise<{ message: string; user: User }> {
    return fetchApi('/api/auth/register', {
      method: 'POST',
      body: JSON.stringify(request)
    });
  },

  async logout(): Promise<void> {
    await fetchApi('/api/auth/logout', { method: 'POST' });
  },

  /**
   * Clear all session cookies including HttpOnly cookies.
   * Used to recover from "Request Header Or Cookie Too Large" errors.
   */
  async clearSession(): Promise<{ message: string; details: string }> {
    return fetchApi('/api/auth/clear-session', { method: 'POST' });
  },

  async getCurrentUser(): Promise<User> {
    return fetchApi('/api/auth/me');
  },

  async updateProfile(request: {
    firstName: string;
    lastName: string;
    email: string;
  }): Promise<User> {
    return fetchApi('/api/users/profile', {
      method: 'PUT',
      body: JSON.stringify(request)
    });
  }
};
