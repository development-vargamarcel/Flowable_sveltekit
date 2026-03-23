<script lang="ts">
  import { onMount } from 'svelte';
  import { Bell, Check } from '@lucide/svelte';
  import EmptyState from '$lib/components/EmptyState.svelte';
  import ErrorDisplay from '$lib/components/ErrorDisplay.svelte';
  import Loading from '$lib/components/Loading.svelte';
  import { notificationStore } from '$lib/stores/notifications.svelte';
  import { getNotificationDisplay } from '$lib/utils/notification-display';

  onMount(() => {
    void notificationStore.loadNotifications();
  });
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
    {#if notificationStore.notifications.some((notification) => !notification.read)}
      <button
        onclick={() => notificationStore.markAllAsRead()}
        class="flex items-center gap-2 px-3 py-1.5 text-sm font-medium text-blue-600 bg-blue-50 hover:bg-blue-100 rounded-md transition-colors"
      >
        <Check class="w-4 h-4" />
        Mark all as read
      </button>
    {/if}
  </div>

  {#if notificationStore.loading || !notificationStore.hasLoaded}
    <Loading text="Loading notifications..." />
  {:else if notificationStore.error}
    <ErrorDisplay
      error={notificationStore.error}
      onRetry={() => notificationStore.loadNotifications()}
      title="Error Loading Notifications"
    />
  {:else if notificationStore.notifications.length === 0}
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
        {#each notificationStore.notifications as notification (notification.id)}
          {@const notificationDisplay = getNotificationDisplay(notification.type)}
          {@const IconComponent = notificationDisplay.icon}
          <li class="hover:bg-gray-50 transition-colors duration-150 {notification.read ? 'opacity-75' : 'bg-blue-50/30'}">
            <div class="p-4 sm:px-6">
              <div class="flex items-start">
                <div class="flex-shrink-0 mr-4">
                  <div class={`w-10 h-10 rounded-full flex items-center justify-center ${notificationDisplay.colorClass}`}>
                    <IconComponent class="w-5 h-5" />
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
                        onclick={() => !notification.read && notificationStore.markAsRead(notification.id)}
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
                      onclick={() => notificationStore.markAsRead(notification.id)}
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
