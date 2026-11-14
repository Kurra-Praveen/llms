/**
 * Payments tab — the borrower's payment history.
 */

import { View, Text, StyleSheet, FlatList, RefreshControl } from 'react-native';
import { useQuery } from '@tanstack/react-query';
import { portalApi } from '@/lib/portalApi';
import { ErrorState } from '@/lib/components/ErrorState';
import { useQueryState } from '@/lib/hooks/useQueryState';
import type { Payment } from '@/lib/types';

const inr = (n?: number) =>
  `₹${(n ?? 0).toLocaleString('en-IN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`;

export default function PaymentsScreen() {
  const query = useQuery({
    queryKey: ['portal', 'payments'],
    queryFn: () => portalApi.getPayments(),
  });
  const state = useQueryState(query);

  if (!state.isReady) {
    return (
      <ErrorState
        variant={state.status}
        message={state.status === 'error' ? 'Could not load your payments.' : undefined}
        onRetry={state.refetch}
      />
    );
  }

  const payments = state.data.content;

  return (
    <View style={styles.container}>
      <FlatList
        data={payments}
        keyExtractor={(item) => item.id}
        contentContainerStyle={{ padding: 16 }}
        refreshControl={
          <RefreshControl refreshing={state.isRefetching} onRefresh={state.refetch} />
        }
        ListEmptyComponent={<Text style={styles.empty}>No payments recorded yet.</Text>}
        renderItem={({ item }: { item: Payment }) => (
          <View style={styles.card}>
            <View style={styles.cardHeader}>
              <Text style={styles.amount}>{inr(item.amountPaid)}</Text>
              <Text style={styles.status}>{item.status}</Text>
            </View>
            <Text style={styles.meta}>
              {item.loanNumber ?? 'Loan'} · {item.paymentDate} · {item.paymentMethod}
            </Text>
            {item.receiptNumber && (
              <Text style={styles.receipt}>Receipt: {item.receiptNumber}</Text>
            )}
          </View>
        )}
      />
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: '#f3f4f6' },
  empty: { textAlign: 'center', color: '#6b7280', marginTop: 40 },
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
  cardHeader: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center' },
  amount: { fontSize: 18, fontWeight: '700', color: '#111827' },
  status: { fontSize: 12, fontWeight: '600', color: '#059669' },
  meta: { fontSize: 13, color: '#6b7280', marginTop: 6 },
  receipt: { fontSize: 12, color: '#9ca3af', marginTop: 4 },
});