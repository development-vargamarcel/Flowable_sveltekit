import { fetchApi } from './core';
import { createLogger } from '$lib/utils/logger';
import type { Notification } from '$lib/types';

const log = createLogger('api.notifications');

export const notificationsApi = {
  // ==================== Notifications ====================
  /**
   * Get all notifications for the current user.
   * @returns A promise that resolves to an array of notifications.
   */
  async getNotifications(): Promise<Notification[]> {
    log.debug('getNotifications called');
    return fetchApi('/api/notifications');
  },

  /**
   * Mark a notification as read.
   * @param id - The ID of the notification.
   */
  async markNotificationAsRead(id: string): Promise<void> {
    log.debug('markNotificationAsRead called', { id });
    await fetchApi(`/api/notifications/${id}/read`, { method: 'POST' });
  },

  /**
   * Mark all notifications as read.
   */
  async markAllNotificationsAsRead(): Promise<void> {
    log.debug('markAllNotificationsAsRead called');
    await fetchApi('/api/notifications/read-all', { method: 'POST' });
  },

  /**
   * Get the count of unread notifications.
   * @returns A promise that resolves to the count of unread notifications.
   */
  async getUnreadNotificationCount(): Promise<number> {
    log.debug('getUnreadNotificationCount called');
    return fetchApi('/api/notifications/unread-count');
  }
};
