import { AlertCircle, CheckCircle, CheckSquare, Info } from '@lucide/svelte';
import type { NotificationType } from '$lib/types';

const notificationDisplayMap: Record<NotificationType, { icon: typeof Info; colorClass: string }> =
  {
    TASK_ASSIGNED: {
      icon: CheckSquare,
      colorClass: 'text-blue-500 bg-blue-50'
    },
    TASK_DUE_SOON: {
      icon: CheckSquare,
      colorClass: 'text-yellow-500 bg-yellow-50'
    },
    TASK_OVERDUE: {
      icon: AlertCircle,
      colorClass: 'text-red-500 bg-red-50'
    },
    PROCESS_COMPLETED: {
      icon: CheckCircle,
      colorClass: 'text-green-500 bg-green-50'
    },
    PROCESS_REJECTED: {
      icon: AlertCircle,
      colorClass: 'text-red-500 bg-red-50'
    },
    SLA_WARNING: {
      icon: AlertCircle,
      colorClass: 'text-orange-500 bg-orange-50'
    },
    SLA_BREACH: {
      icon: AlertCircle,
      colorClass: 'text-red-600 bg-red-100'
    },
    INFO: {
      icon: Info,
      colorClass: 'text-gray-500 bg-gray-50'
    }
  };

export function getNotificationDisplay(type: NotificationType) {
  return notificationDisplayMap[type] ?? notificationDisplayMap.INFO;
}
