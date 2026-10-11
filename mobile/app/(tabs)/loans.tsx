/**
 * My Loans tab — list of the borrower's loans.
 */

import { View, Text, StyleSheet, FlatList, TouchableOpacity, ActivityIndicator } from 'react-native';
import { useQuery } from '@tanstack/react-query';
import { useRouter } from 'expo-router';
import { portalApi } from '@/lib/portalApi';
import type { Loan } from '@/lib/types';

const inr = (n?: number) =>
  `₹${(n ?? 0).toLocaleString('en-IN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`;

const statusColor: Record<string, string> = {
  ACTIVE: '#059669',
  DISBURSED: '#059669',
  CLOSED: '#6b7280',
  PENDING_APPROVAL: '#d97706',
  DRAFT: '#6b7280',
};

export default function LoansScreen() {
  const router = useRouter();

  const { data, isLoading } = useQuery({
    queryKey: ['portal', 'loans'],
    queryFn: () => portalApi.getLoans(),
  });

  if (isLoading) {
    return (
      <View style={styles.center}>
        <ActivityIndicator size="large" color="#2563eb" />
      </View>
    );
  }

  const loans = data?.content ?? [];

  return (
    <View style={styles.container}>
      <FlatList
        data={loans}
        keyExtractor={(item) => item.id}
        contentContainerStyle={{ padding: 16 }}
        ListEmptyComponent={
          <Text style={styles.empty}>You don't have any loans yet.</Text>
        }
        renderItem={({ item }: { item: Loan }) => (
          <TouchableOpacity
            style={styles.card}
            onPress={() => router.push(`/loan/${item.id}`)}
          >
            <View style={styles.cardHeader}>
              <Text style={styles.loanNumber}>{item.loanNumber}</Text>
              <View
                style={[
                  styles.badge,
                  { backgroundColor: (statusColor[item.status] ?? '#6b7280') + '20' },
                ]}
              >
                <Text
                  style={[styles.badgeText, { color: statusColor[item.status] ?? '#6b7280' }]}
                >
                  {item.status}
                </Text>
              </View>
            </View>
            <View style={styles.row}>
              <Text style={styles.label}>Principal</Text>
              <Text style={styles.value}>{inr(item.principalAmount)}</Text>
            </View>
            <View style={styles.row}>
              <Text style={styles.label}>EMI</Text>
              <Text style={styles.value}>{inr(item.emiAmount)}</Text>
            </View>
            <View style={styles.row}>
              <Text style={styles.label}>Outstanding</Text>
              <Text style={[styles.value, { color: '#2563eb' }]}>
                {inr(item.totalOutstanding)}
              </Text>
            </View>
          </TouchableOpacity>
        )}
      />
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: '#f3f4f6' },
  center: { flex: 1, justifyContent: 'center', alignItems: 'center' },
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
  cardHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: 10,
  },
  loanNumber: { fontSize: 16, fontWeight: '700', color: '#111827' },
  badge: { borderRadius: 12, paddingHorizontal: 10, paddingVertical: 3 },
  badgeText: { fontSize: 11, fontWeight: '600' },
  row: { flexDirection: 'row', justifyContent: 'space-between', marginTop: 4 },
  label: { fontSize: 13, color: '#6b7280' },
  value: { fontSize: 13, fontWeight: '600', color: '#111827' },
});
