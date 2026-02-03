/* eslint-disable no-console */
import { fetchApi } from './core';

export const notificationsApi = {
  // ==================== Notifications ====================
  /**
   * Get all notifications for the current user.
   * @returns A promise that resolves to an array of notifications.
   */
  async getNotifications(): Promise<any[]> {
    console.log('[notificationsApi] getNotifications called');
    return fetchApi('/api/notifications');
  },

  /**
   * Mark a notification as read.
   * @param id - The ID of the notification.
   */
  async markNotificationAsRead(id: string): Promise<void> {
    console.log('[notificationsApi] markNotificationAsRead called with id:', id);
    await fetchApi(`/api/notifications/${id}/read`, { method: 'POST' });
  },

  /**
   * Mark all notifications as read.
   */
  async markAllNotificationsAsRead(): Promise<void> {
    console.log('[notificationsApi] markAllNotificationsAsRead called');
    await fetchApi('/api/notifications/read-all', { method: 'POST' });
  },

  /**
   * Get the count of unread notifications.
   * @returns A promise that resolves to the count of unread notifications.
   */
  async getUnreadNotificationCount(): Promise<number> {
    console.log('[notificationsApi] getUnreadNotificationCount called');
    return fetchApi('/api/notifications/unread-count');
  }
};
