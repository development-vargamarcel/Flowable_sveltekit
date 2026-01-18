import { browser } from '$app/environment';
import { api } from '$lib/api/client';

export interface Notification {
  id: string;
  userId: string;
  title: string;
  message: string;
  type: 'TASK_ASSIGNED' | 'TASK_DUE_SOON' | 'TASK_OVERDUE' | 'PROCESS_COMPLETED' | 'PROCESS_REJECTED' | 'MENTION' | 'SYSTEM';
  link?: string;
  read: boolean;
  createdAt: string;
}

function createNotificationStore() {
  let notifications = $state<Notification[]>([]);
  let unreadCount = $state(0);
  let loading = $state(false);
  let error = $state<string | null>(null);
  let pollingInterval: any = null;

  async function loadNotifications() {
    if (!browser) return;
    loading = true;
    try {
      notifications = await api.getNotifications();
      updateUnreadCount();
    } catch (err) {
      console.error('Failed to load notifications', err);
      error = 'Failed to load notifications';
    } finally {
      loading = false;
    }
  }

  function updateUnreadCount() {
    unreadCount = notifications.filter(n => !n.read).length;
  }

  async function markAsRead(id: string) {
    // Optimistic update
    const notification = notifications.find(n => n.id === id);
    if (notification && !notification.read) {
      notification.read = true;
      updateUnreadCount();

      try {
        await api.markNotificationAsRead(id);
      } catch (err) {
        console.error('Failed to mark notification as read', err);
        // Revert on error
        notification.read = false;
        updateUnreadCount();
      }
    }
  }

  async function markAllAsRead() {
    // Optimistic update
    const unread = notifications.filter(n => !n.read);
    unread.forEach(n => n.read = true);
    updateUnreadCount();

    try {
      await api.markAllNotificationsAsRead();
    } catch (err) {
      console.error('Failed to mark all as read', err);
      // Revert
      unread.forEach(n => n.read = false);
      updateUnreadCount();
    }
  }

  function startPolling(intervalMs = 30000) {
    if (!browser) return;
    loadNotifications();
    stopPolling();
    pollingInterval = setInterval(loadNotifications, intervalMs);
  }

  function stopPolling() {
    if (pollingInterval) {
      clearInterval(pollingInterval);
      pollingInterval = null;
    }
  }

  return {
    get notifications() { return notifications; },
    get unreadCount() { return unreadCount; },
    get loading() { return loading; },
    loadNotifications,
    markAsRead,
    markAllAsRead,
    startPolling,
    stopPolling
  };
}

export const notificationStore = createNotificationStore();
