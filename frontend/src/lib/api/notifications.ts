import { fetchApi } from './core';

export const notificationsApi = {
  // ==================== Notifications ====================
  async getNotifications(): Promise<any[]> {
    return fetchApi('/api/notifications');
  },

  async markNotificationAsRead(id: string): Promise<void> {
    await fetchApi(`/api/notifications/${id}/read`, { method: 'POST' });
  },

  async markAllNotificationsAsRead(): Promise<void> {
    await fetchApi('/api/notifications/read-all', { method: 'POST' });
  },

  async getUnreadNotificationCount(): Promise<number> {
    return fetchApi('/api/notifications/unread-count');
  }
};
