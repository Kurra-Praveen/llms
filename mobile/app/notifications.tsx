/**
 * Notifications screen — in-app notification center.
 * Pushed as a modal from the bell icon on the Home tab.
 */

import { View, Text, StyleSheet, FlatList, TouchableOpacity, ActivityIndicator } from 'react-native';
import { Stack } from 'expo-router';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { notificationApi } from '@/lib/portalApi';
import type { AppNotification } from '@/lib/types';

function timeAgo(iso: string): string {
  const seconds = Math.floor((Date.now() - new Date(iso).getTime()) / 1000);
  if (seconds < 60) return 'just now';
  const minutes = Math.floor(seconds / 60);
  if (minutes < 60) return `${minutes}m ago`;
  const hours = Math.floor(minutes / 60);
  if (hours < 24) return `${hours}h ago`;
  const days = Math.floor(hours / 24);
  if (days < 30) return `${days}d ago`;
  return new Date(iso).toLocaleDateString();
}

export default function NotificationsScreen() {
  const queryClient = useQueryClient();

  const { data, isLoading } = useQuery({
    queryKey: ['notifications'],
    queryFn: () => notificationApi.getAll(),
  });

  const markRead = useMutation({
    mutationFn: (id: string) => notificationApi.markAsRead(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['notifications'] });
      queryClient.invalidateQueries({ queryKey: ['notifications', 'unread-count'] });
    },
  });

  const notifications = data?.content ?? [];

  return (
    <View style={styles.container}>
      <Stack.Screen options={{ title: 'Notifications', headerShown: true }} />

      {isLoading ? (
        <View style={styles.center}>
          <ActivityIndicator size="large" color="#2563eb" />
        </View>
      ) : (
        <FlatList
          data={notifications}
          keyExtractor={(item) => item.id}
          contentContainerStyle={{ padding: 16 }}
          ListEmptyComponent={
            <View style={styles.center}>
              <Text style={styles.emptyIcon}>🔔</Text>
              <Text style={styles.empty}>No notifications yet.</Text>
            </View>
          }
          renderItem={({ item }: { item: AppNotification }) => {
            const isUnread = !item.readAt;
            return (
              <TouchableOpacity
                style={[styles.card, isUnread && styles.cardUnread]}
                onPress={() => isUnread && markRead.mutate(item.id)}
                activeOpacity={isUnread ? 0.7 : 1}
              >
                <View style={styles.cardRow}>
                  {isUnread && <View style={styles.dot} />}
                  <View style={{ flex: 1 }}>
                    {item.subject && (
                      <Text style={[styles.subject, isUnread && styles.subjectUnread]}>
                        {item.subject}
                      </Text>
                    )}
                    <Text style={[styles.content, isUnread && styles.contentUnread]}>
                      {item.content}
                    </Text>
                    <Text style={styles.time}>{timeAgo(item.createdAt)}</Text>
                  </View>
                </View>
              </TouchableOpacity>
            );
          }}
        />
      )}
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: '#f3f4f6' },
  center: { flex: 1, justifyContent: 'center', alignItems: 'center', paddingTop: 60 },
  emptyIcon: { fontSize: 48, opacity: 0.3 },
  empty: { color: '#6b7280', fontSize: 15, marginTop: 12 },
  card: {
    backgroundColor: '#fff',
    borderRadius: 12,
    padding: 14,
    marginBottom: 8,
    shadowColor: '#000',
    shadowOpacity: 0.04,
    shadowRadius: 6,
    shadowOffset: { width: 0, height: 1 },
    elevation: 1,
  },
  cardUnread: { backgroundColor: '#eff6ff', borderColor: '#bfdbfe', borderWidth: 1 },
  cardRow: { flexDirection: 'row', alignItems: 'flex-start' },
  dot: {
    width: 8,
    height: 8,
    borderRadius: 4,
    backgroundColor: '#2563eb',
    marginTop: 6,
    marginRight: 10,
  },
  subject: { fontSize: 14, fontWeight: '500', color: '#374151' },
  subjectUnread: { fontWeight: '700', color: '#111827' },
  content: { fontSize: 14, color: '#6b7280', marginTop: 2 },
  contentUnread: { color: '#1f2937' },
  time: { fontSize: 11, color: '#9ca3af', marginTop: 6 },
});
