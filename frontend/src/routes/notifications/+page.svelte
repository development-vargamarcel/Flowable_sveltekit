<script lang="ts">
  import { onMount } from 'svelte';
  import { api } from '$lib/api/client';
  import type { Notification } from '$lib/types';
  import { Bell, CheckSquare, AlertCircle, CheckCircle, Info, Check } from '@lucide/svelte';
  import Loading from '$lib/components/Loading.svelte';
  import EmptyState from '$lib/components/EmptyState.svelte';
  import ErrorDisplay from '$lib/components/ErrorDisplay.svelte';
  import { notificationStore } from '$lib/stores/notifications.svelte';

  let loading = $state(true);
  let error = $state<string | null>(null);
  let notifications = $state<Notification[]>([]);

  onMount(async () => {
    await loadNotifications();
  });

  async function loadNotifications() {
    loading = true;
    error = null;
    try {
      const data = await api.getNotifications();
      notifications = data;
      // Also update store to sync badge
      notificationStore.loadNotifications();
    } catch (err) {
      error = err instanceof Error ? err.message : 'Failed to load notifications';
    } finally {
      loading = false;
    }
  }

  async function markAllRead() {
    try {
      await api.markAllNotificationsAsRead();
      await loadNotifications();
    } catch (err) {
      console.error('Failed to mark all as read', err);
    }
  }

  async function markAsRead(id: string) {
    try {
        // Optimistic update
        notifications = notifications.map(n => n.id === id ? { ...n, read: true } : n);
        await api.markNotificationAsRead(id);
        notificationStore.loadNotifications();
    } catch (err) {
        console.error('Failed to mark notification as read', err);
    }
  }

  function getIconColor(type: string) {
    switch (type) {
      case 'TASK_ASSIGNED': return 'text-blue-500 bg-blue-50';
      case 'TASK_DUE_SOON': return 'text-yellow-500 bg-yellow-50';
      case 'TASK_OVERDUE': return 'text-red-500 bg-red-50';
      case 'PROCESS_COMPLETED': return 'text-green-500 bg-green-50';
      case 'PROCESS_REJECTED': return 'text-red-500 bg-red-50';
      case 'SLA_WARNING': return 'text-orange-500 bg-orange-50';
      case 'SLA_BREACH': return 'text-red-600 bg-red-100';
      default: return 'text-gray-500 bg-gray-50';
    }
  }
</script>

<svelte:head>
  <title>Notifications - BPM Demo</title>
</svelte:head>

<div class="max-w-4xl mx-auto px-4 py-8">
  <div class="flex items-center justify-between mb-6">
    <h1 class="text-2xl font-bold text-gray-900 flex items-center gap-2">
      <Bell class="w-6 h-6 text-gray-500" />
      Notifications
    </h1>
    {#if notifications.some(n => !n.read)}
      <button
        onclick={markAllRead}
        class="flex items-center gap-2 px-3 py-1.5 text-sm font-medium text-blue-600 bg-blue-50 hover:bg-blue-100 rounded-md transition-colors"
      >
        <Check class="w-4 h-4" />
        Mark all as read
      </button>
    {/if}
  </div>

  {#if loading}
    <Loading text="Loading notifications..." />
  {:else if error}
    <ErrorDisplay {error} onRetry={loadNotifications} title="Error Loading Notifications" />
  {:else if notifications.length === 0}
    {#snippet bellIcon()}
      <Bell class="w-12 h-12 text-gray-400 mb-3" />
    {/snippet}
    <EmptyState
      message="No notifications. You're all caught up! Check back later for updates."
      icon={bellIcon}
    />
  {:else}
    <div class="bg-white shadow rounded-lg overflow-hidden border border-gray-200">
      <ul class="divide-y divide-gray-200">
        {#each notifications as notification (notification.id)}
          <li class="hover:bg-gray-50 transition-colors duration-150 {notification.read ? 'opacity-75' : 'bg-blue-50/30'}">
            <div class="p-4 sm:px-6">
              <div class="flex items-start">
                <div class="flex-shrink-0 mr-4">
                  <div class={`w-10 h-10 rounded-full flex items-center justify-center ${getIconColor(notification.type)}`}>
                    {#if notification.type === 'TASK_ASSIGNED' || notification.type === 'TASK_DUE_SOON'}
                      <CheckSquare class="w-5 h-5" />
                    {:else if notification.type === 'TASK_OVERDUE' || notification.type === 'PROCESS_REJECTED' || notification.type === 'SLA_BREACH'}
                       <AlertCircle class="w-5 h-5" />
                    {:else if notification.type === 'PROCESS_COMPLETED'}
                      <CheckCircle class="w-5 h-5" />
                    {:else}
                      <Info class="w-5 h-5" />
                    {/if}
                  </div>
                </div>
                <div class="flex-1 min-w-0">
                  <div class="flex justify-between items-start">
                      <p class="text-sm font-semibold text-gray-900 truncate pr-4">
                        {notification.title}
                      </p>
                      <span class="text-xs text-gray-500 whitespace-nowrap">
                        {new Date(notification.createdAt).toLocaleString()}
                      </span>
                  </div>
                  <p class="text-sm text-gray-600 mt-1">
                    {notification.message}
                  </p>
                  {#if notification.link}
                    <div class="mt-2">
                        <a
                            href={notification.link}
                            class="text-sm font-medium text-blue-600 hover:text-blue-500 inline-flex items-center gap-1"
                            onclick={() => !notification.read && markAsRead(notification.id)}
                        >
                            View details
                            <svg class="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7" />
                            </svg>
                        </a>
                    </div>
                  {/if}
                </div>
                {#if !notification.read}
                  <div class="ml-4 flex-shrink-0 self-center">
                      <button
                        onclick={() => markAsRead(notification.id)}
                        class="p-1 rounded-full text-blue-600 hover:bg-blue-100"
                        title="Mark as read"
                        aria-label="Mark as read"
                      >
                          <div class="w-3 h-3 bg-blue-600 rounded-full"></div>
                      </button>
                  </div>
                {/if}
              </div>
            </div>
          </li>
        {/each}
      </ul>
    </div>
  {/if}
</div>
