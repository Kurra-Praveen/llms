/**
 * Notification API Service (/v1/notifications)
 * Powers the in-app notification center and the header unread badge.
 */

import api from './api';
import { logger } from '@/utils/logger';
import type { ApiResponse, PagedResponse, AppNotification } from '@/types';

const notificationLogger = logger.scope('NotificationService');

export const notificationService = {
  /** List my notifications (paginated, newest first) */
  async getAll(page = 0, size = 20): Promise<PagedResponse<AppNotification>['data']> {
    notificationLogger.info('Fetching notifications', { page, size });
    const response = await api.get<PagedResponse<AppNotification>>('/v1/notifications', {
      params: { page, size },
    });
    return response.data.data;
  },

  /** Get unread notification count (for the header badge) */
  async getUnreadCount(): Promise<number> {
    const response = await api.get<ApiResponse<{ unreadCount: number }>>(
      '/v1/notifications/unread-count'
    );
    return response.data.data.unreadCount;
  },

  /** Mark a notification as read */
  async markAsRead(notificationId: string): Promise<AppNotification> {
    notificationLogger.info('Marking notification read', { notificationId });
    const response = await api.post<ApiResponse<AppNotification>>(
      `/v1/notifications/${notificationId}/read`
    );
    return response.data.data;
  },
};

export default notificationService;
