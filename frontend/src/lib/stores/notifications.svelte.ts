import { browser } from '$app/environment';
import { api } from '$lib/api/client';
import type { Notification } from '$lib/types';

function createNotificationStore() {
  let notifications = $state<Notification[]>([]);
  let unreadCount = $state(0);
  let loading = $state(false);
  let error = $state<string | null>(null);
  let hasLoaded = $state(false);
  let pollingInterval: ReturnType<typeof setInterval> | null = null;

  function applyNotifications(nextNotifications: Notification[]) {
    notifications = nextNotifications;
    unreadCount = nextNotifications.filter((notification) => !notification.read).length;
  }

  async function loadNotifications() {
    if (!browser) {
      return notifications;
    }

    loading = true;
    error = null;

    try {
      const nextNotifications = await api.getNotifications();
      applyNotifications(nextNotifications);
      return notifications;
    } catch (err) {
      console.error('Failed to load notifications', err);
      error = err instanceof Error ? err.message : 'Failed to load notifications';
      return notifications;
    } finally {
      loading = false;
      hasLoaded = true;
    }
  }

  async function markAsRead(id: string) {
    const targetNotification = notifications.find((notification) => notification.id === id);

    if (!targetNotification || targetNotification.read) {
      return;
    }

    const previousNotifications = notifications;
    applyNotifications(
      notifications.map((notification) =>
        notification.id === id ? { ...notification, read: true } : notification
      )
    );

    try {
      await api.markNotificationAsRead(id);
    } catch (err) {
      console.error('Failed to mark notification as read', err);
      error = err instanceof Error ? err.message : 'Failed to mark notification as read';
      applyNotifications(previousNotifications);
    }
  }

  async function markAllAsRead() {
    const hasUnreadNotifications = notifications.some((notification) => !notification.read);

    if (!hasUnreadNotifications) {
      return;
    }

    const previousNotifications = notifications;
    applyNotifications(notifications.map((notification) => ({ ...notification, read: true })));

    try {
      await api.markAllNotificationsAsRead();
    } catch (err) {
      console.error('Failed to mark all notifications as read', err);
      error = err instanceof Error ? err.message : 'Failed to mark all notifications as read';
      applyNotifications(previousNotifications);
    }
  }

  function startPolling(intervalMs = 30000) {
    if (!browser) {
      return;
    }

    stopPolling();
    void loadNotifications();
    pollingInterval = setInterval(() => {
      void loadNotifications();
    }, intervalMs);
  }

  function stopPolling() {
    if (!pollingInterval) {
      return;
    }

    clearInterval(pollingInterval);
    pollingInterval = null;
  }

  return {
    get notifications() {
      return notifications;
    },
    get unreadCount() {
      return unreadCount;
    },
    get loading() {
      return loading;
    },
    get error() {
      return error;
    },
    get hasLoaded() {
      return hasLoaded;
    },
    loadNotifications,
    markAsRead,
    markAllAsRead,
    startPolling,
    stopPolling
  };
}

export const notificationStore = createNotificationStore();
