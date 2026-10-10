/**
 * Notifications Page — in-app notification center
 * Shared by all roles (borrowers see their loan alerts; staff see theirs).
 */

import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { formatDistanceToNow } from 'date-fns';
import { BellIcon } from '@heroicons/react/24/outline';
import { Card, LoadingScreen } from '@/components/ui';
import { notificationService } from '@/services';
import { logger } from '@/utils/logger';

const notificationsLogger = logger.scope('NotificationsPage');

export function NotificationsPage() {
  const queryClient = useQueryClient();

  const { data, isLoading } = useQuery({
    queryKey: ['notifications'],
    queryFn: () => notificationService.getAll(0, 50),
  });

  const markReadMutation = useMutation({
    mutationFn: (id: string) => notificationService.markAsRead(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['notifications'] });
      queryClient.invalidateQueries({ queryKey: ['notifications', 'unread-count'] });
    },
  });

  notificationsLogger.debug('Notifications render', { count: data?.totalElements });

  if (isLoading) {
    return <LoadingScreen message="Loading notifications..." />;
  }

  const notifications = data?.content ?? [];

  return (
    <div className="space-y-4">
      <h1 className="text-2xl font-bold text-gray-900">Notifications</h1>

      {notifications.length === 0 ? (
        <Card>
          <div className="text-center py-8">
            <BellIcon className="h-12 w-12 text-gray-300 mx-auto" />
            <p className="text-gray-500 mt-4">No notifications yet.</p>
          </div>
        </Card>
      ) : (
        <div className="space-y-2">
          {notifications.map((notification) => {
            const isUnread = !notification.readAt;
            return (
              <Card
                key={notification.id}
                padding="sm"
                onClick={() => {
                  if (isUnread) {
                    markReadMutation.mutate(notification.id);
                  }
                }}
                className={`cursor-pointer transition-colors ${
                  isUnread ? 'bg-primary-50 border-primary-200' : ''
                }`}
              >
                <div className="flex items-start gap-3">
                  {isUnread && (
                    <span className="mt-1.5 h-2 w-2 flex-shrink-0 bg-primary-600 rounded-full" />
                  )}
                  <div className="flex-1 min-w-0">
                    {notification.subject && (
                      <p
                        className={`text-sm ${
                          isUnread ? 'font-semibold text-gray-900' : 'font-medium text-gray-700'
                        }`}
                      >
                        {notification.subject}
                      </p>
                    )}
                    <p
                      className={`text-sm ${
                        isUnread ? 'text-gray-800' : 'text-gray-500'
                      }`}
                    >
                      {notification.content}
                    </p>
                    <p className="text-xs text-gray-400 mt-1">
                      {formatDistanceToNow(new Date(notification.createdAt), {
                        addSuffix: true,
                      })}
                    </p>
                  </div>
                </div>
              </Card>
            );
          })}
        </div>
      )}
    </div>
  );
}

export default NotificationsPage;
