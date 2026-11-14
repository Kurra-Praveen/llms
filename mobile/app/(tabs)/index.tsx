/**
 * Home tab — summary: active loans, total outstanding, next EMI.
 */

import { View, Text, StyleSheet, ScrollView, RefreshControl, TouchableOpacity } from 'react-native';
import { useQuery } from '@tanstack/react-query';
import { useRouter } from 'expo-router';
import { portalApi, notificationApi } from '@/lib/portalApi';
import { useAuth } from '@/lib/auth';
import { ErrorState } from '@/lib/components/ErrorState';
import { useQueryState } from '@/lib/hooks/useQueryState';

const inr = (n?: number) =>
  `₹${(n ?? 0).toLocaleString('en-IN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`;

export default function HomeScreen() {
  const { user } = useAuth();
  const router = useRouter();

  const summaryQuery = useQuery({
    queryKey: ['portal', 'summary'],
    queryFn: portalApi.getSummary,
  });

  // Poll unread notification count for the header bell badge
  const unreadQuery = useQuery({
    queryKey: ['notifications', 'unread-count'],
    queryFn: notificationApi.getUnreadCount,
    refetchInterval: 30_000,
    retry: false,
  });

  const state = useQueryState(summaryQuery);
  const unreadCount = unreadQuery.data ?? 0;

  if (state.status === 'loading') {
    return <ErrorState variant="loading" />;
  }
  if (state.status === 'error') {
    return <ErrorState variant="error" message="Could not load your summary." onRetry={state.refetch} />;
  }

  const summary = state.data!;
  const nextEmi = summary.nextEmi;

  return (
    <ScrollView
      style={styles.container}
      refreshControl={<RefreshControl refreshing={state.isRefetching} onRefresh={state.refetch} />}
    >
      <View style={styles.headerRow}>
        <Text style={styles.greeting}>Welcome back, {user?.firstName}!</Text>
        <TouchableOpacity
          style={styles.bell}
          onPress={() => router.push('/notifications')}
          accessibilityLabel="Notifications"
        >
          <Text style={styles.bellIcon}>🔔</Text>
          {unreadCount > 0 && (
            <View style={styles.badge}>
              <Text style={styles.badgeText}>{unreadCount > 99 ? '99+' : unreadCount}</Text>
            </View>
          )}
        </TouchableOpacity>
      </View>

      <View style={styles.row}>
        <View style={[styles.card, styles.half]}>
          <Text style={styles.cardLabel}>Active Loans</Text>
          <Text style={styles.cardValue}>{summary.activeLoansCount ?? 0}</Text>
        </View>
        <View style={[styles.card, styles.half]}>
          <Text style={styles.cardLabel}>Total Outstanding</Text>
          <Text style={styles.cardValue}>{inr(summary.totalOutstanding)}</Text>
        </View>
      </View>

      <View style={styles.card}>
        <Text style={styles.sectionTitle}>Next EMI</Text>
        {nextEmi ? (
          <>
            <View style={styles.emiRow}>
              <Text style={styles.emiLabel}>Amount due</Text>
              <Text style={styles.emiAmount}>{inr(nextEmi.outstandingAmount)}</Text>
            </View>
            <View style={styles.emiRow}>
              <Text style={styles.emiLabel}>Due date</Text>
              <Text style={styles.emiValue}>{nextEmi.dueDate}</Text>
            </View>
            <View style={styles.emiRow}>
              <Text style={styles.emiLabel}>Loan</Text>
              <Text style={styles.emiValue}>{nextEmi.loanNumber ?? '-'}</Text>
            </View>
            {nextEmi.status === 'OVERDUE' && (
              <View style={styles.overdueBox}>
                <Text style={styles.overdueText}>
                  Overdue by {nextEmi.daysPastDue ?? 0} day(s). Please pay immediately.
                </Text>
              </View>
            )}
          </>
        ) : (
          <Text style={styles.allClear}>You are all caught up! 🎉</Text>
        )}
      </View>
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: '#f3f4f6', padding: 16 },
  headerRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: 16,
  },
  greeting: { fontSize: 22, fontWeight: '700', color: '#111827' },
  bell: { position: 'relative', padding: 4 },
  bellIcon: { fontSize: 24 },
  badge: {
    position: 'absolute',
    top: -2,
    right: -4,
    minWidth: 18,
    height: 18,
    borderRadius: 9,
    backgroundColor: '#ef4444',
    justifyContent: 'center',
    alignItems: 'center',
    paddingHorizontal: 4,
  },
  badgeText: { color: '#fff', fontSize: 10, fontWeight: '700' },
  row: { flexDirection: 'row', gap: 12, marginBottom: 12 },
  card: {
    backgroundColor: '#fff',
    borderRadius: 14,
    padding: 16,
    marginBottom: 12,
    shadowColor: '#000',
    shadowOpacity: 0.05,
    shadowRadius: 8,
    shadowOffset: { width: 0, height: 2 },
    elevation: 2,
  },
  half: { flex: 1 },
  cardLabel: { fontSize: 13, color: '#6b7280' },
  cardValue: { fontSize: 22, fontWeight: '700', color: '#111827', marginTop: 4 },
  sectionTitle: { fontSize: 16, fontWeight: '700', color: '#111827', marginBottom: 12 },
  emiRow: { flexDirection: 'row', justifyContent: 'space-between', marginBottom: 8 },
  emiLabel: { fontSize: 14, color: '#6b7280' },
  emiAmount: { fontSize: 18, fontWeight: '700', color: '#111827' },
  emiValue: { fontSize: 14, fontWeight: '500', color: '#111827' },
  overdueBox: {
    backgroundColor: '#fef2f2',
    borderColor: '#fecaca',
    borderWidth: 1,
    borderRadius: 8,
    padding: 10,
    marginTop: 8,
  },
  overdueText: { color: '#b91c1c', fontSize: 13 },
  allClear: { fontSize: 14, color: '#059669' },
});