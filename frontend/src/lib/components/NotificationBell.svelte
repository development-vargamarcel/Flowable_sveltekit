<script lang="ts">
  import { onMount, onDestroy } from 'svelte';
  import { notificationStore } from '$lib/stores/notifications.svelte';
  import { fade, fly } from 'svelte/transition';
  import { Bell, CheckSquare, AlertCircle, CheckCircle, Info } from '@lucide/svelte';

  let showDropdown = $state(false);
  let dropdownRef: HTMLDivElement;

  function toggleDropdown() {
    showDropdown = !showDropdown;
    if (showDropdown) {
      // Refresh when opening
      notificationStore.loadNotifications();
    }
  }

  function handleClickOutside(event: MouseEvent) {
    if (showDropdown && dropdownRef && !dropdownRef.contains(event.target as Node)) {
      showDropdown = false;
    }
  }

  onMount(() => {
    document.addEventListener('click', handleClickOutside);
    notificationStore.startPolling();
  });

  onDestroy(() => {
    if (typeof document !== 'undefined') {
      document.removeEventListener('click', handleClickOutside);
    }
    notificationStore.stopPolling();
  });

  function handleNotificationClick(notification: any) {
    if (!notification.read) {
      notificationStore.markAsRead(notification.id);
    }
    showDropdown = false;
  }

  function getIconColor(type: string) {
    switch (type) {
      case 'TASK_ASSIGNED': return 'text-blue-500 bg-blue-50';
      case 'TASK_DUE_SOON': return 'text-yellow-500 bg-yellow-50';
      case 'TASK_OVERDUE': return 'text-red-500 bg-red-50';
      case 'PROCESS_COMPLETED': return 'text-green-500 bg-green-50';
      case 'PROCESS_REJECTED': return 'text-red-500 bg-red-50';
      default: return 'text-gray-500 bg-gray-50';
    }
  }
</script>

<div class="relative" bind:this={dropdownRef}>
  <button
    onclick={toggleDropdown}
    class="relative p-2 text-gray-500 hover:text-gray-700 focus:outline-none rounded-full hover:bg-gray-100 transition-colors"
    aria-label="Notifications"
  >
    <Bell class="w-6 h-6" />
    {#if notificationStore.unreadCount > 0}
      <span
        class="absolute top-0 right-0 inline-flex items-center justify-center px-1.5 py-0.5 text-xs font-bold leading-none text-white transform translate-x-1/4 -translate-y-1/4 bg-red-600 rounded-full"
      >
        {notificationStore.unreadCount > 9 ? '9+' : notificationStore.unreadCount}
      </span>
    {/if}
  </button>

  {#if showDropdown}
    <div
      transition:fly={{ y: 10, duration: 200 }}
      class="absolute right-0 z-50 mt-2 w-80 bg-white rounded-lg shadow-xl ring-1 ring-black ring-opacity-5 origin-top-right overflow-hidden"
    >
      <div class="p-3 border-b border-gray-100 flex justify-between items-center bg-gray-50">
        <h3 class="text-sm font-semibold text-gray-900">Notifications</h3>
        {#if notificationStore.unreadCount > 0}
          <button
            onclick={() => notificationStore.markAllAsRead()}
            class="text-xs text-blue-600 hover:text-blue-800 font-medium"
          >
            Mark all read
          </button>
        {/if}
      </div>

      <div class="max-h-96 overflow-y-auto">
        {#if notificationStore.notifications.length === 0}
          <div class="p-8 text-center text-gray-500">
            <p class="text-sm">No notifications yet</p>
          </div>
        {:else}
          <ul class="divide-y divide-gray-100">
            {#each notificationStore.notifications as notification (notification.id)}
              <li>
                <a
                  href={notification.link || '#'}
                  onclick={(e) => {
                    if (!notification.link) e.preventDefault();
                    handleNotificationClick(notification);
                  }}
                  class="block p-4 hover:bg-gray-50 transition-colors duration-150 {notification.read ? 'opacity-75' : 'bg-blue-50/30'}"
                >
                  <div class="flex items-start">
                    <div class="flex-shrink-0 mr-3">
                      <div class={`w-8 h-8 rounded-full flex items-center justify-center ${getIconColor(notification.type)}`}>
                        {#if notification.type === 'TASK_ASSIGNED' || notification.type === 'TASK_DUE_SOON'}
                          <CheckSquare class="w-4 h-4" />
                        {:else if notification.type === 'TASK_OVERDUE' || notification.type === 'PROCESS_REJECTED'}
                           <AlertCircle class="w-4 h-4" />
                        {:else if notification.type === 'PROCESS_COMPLETED'}
                          <CheckCircle class="w-4 h-4" />
                        {:else}
                          <Info class="w-4 h-4" />
                        {/if}
                      </div>
                    </div>
                    <div class="flex-1 min-w-0">
                      <p class="text-sm font-medium text-gray-900 truncate">
                        {notification.title}
                      </p>
                      <p class="text-xs text-gray-500 mt-0.5 line-clamp-2">
                        {notification.message}
                      </p>
                      <p class="text-xs text-gray-400 mt-1">
                        {new Date(notification.createdAt).toLocaleString()}
                      </p>
                    </div>
                    {#if !notification.read}
                      <span class="inline-block w-2 h-2 bg-blue-600 rounded-full mt-1.5 ml-2"></span>
                    {/if}
                  </div>
                </a>
              </li>
            {/each}
          </ul>
        {/if}
      </div>
      
      <div class="bg-gray-50 p-2 text-center border-t border-gray-100">
        <a href="/notifications" class="text-xs font-medium text-blue-600 hover:text-blue-800">
          View all notifications
        </a>
      </div>
    </div>
  {/if}
</div>
