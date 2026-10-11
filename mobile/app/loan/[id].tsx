/**
 * Loan details — summary + full repayment schedule.
 * Pushed on top of the tabs when a loan card is tapped.
 */

import { View, Text, StyleSheet, ScrollView, ActivityIndicator } from 'react-native';
import { useLocalSearchParams, Stack } from 'expo-router';
import { useQuery } from '@tanstack/react-query';
import { portalApi } from '@/lib/portalApi';

const inr = (n?: number) =>
  `₹${(n ?? 0).toLocaleString('en-IN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`;

export default function LoanDetailsScreen() {
  const { id } = useLocalSearchParams<{ id: string }>();

  const { data: loan, isLoading: loanLoading } = useQuery({
    queryKey: ['portal', 'loans', id],
    queryFn: () => portalApi.getLoan(id!),
    enabled: !!id,
  });

  const { data: schedule } = useQuery({
    queryKey: ['portal', 'loans', id, 'schedule'],
    queryFn: () => portalApi.getSchedule(id!),
    enabled: !!id,
  });

  if (loanLoading) {
    return (
      <View style={styles.center}>
        <ActivityIndicator size="large" color="#2563eb" />
      </View>
    );
  }

  if (!loan) {
    return (
      <View style={styles.center}>
        <Text style={styles.empty}>Loan not found</Text>
      </View>
    );
  }

  return (
    <ScrollView style={styles.container}>
      <Stack.Screen options={{ title: loan.loanNumber, headerShown: true }} />

      <View style={styles.card}>
        <Text style={styles.sectionTitle}>Loan Summary</Text>
        <Row label="Principal" value={inr(loan.principalAmount)} />
        <Row label="Interest" value={`${loan.interestRate}% (${loan.interestType})`} />
        <Row label="EMI" value={inr(loan.emiAmount)} />
        <Row label="Tenure" value={`${loan.tenureMonths} months`} />
        <Row label="Total Paid" value={inr(loan.totalPaid)} valueColor="#059669" />
        <Row label="Outstanding" value={inr(loan.totalOutstanding)} valueColor="#2563eb" />
        {loan.disbursementDate && <Row label="Disbursed" value={loan.disbursementDate} />}
        {loan.maturityDate && <Row label="Matures" value={loan.maturityDate} />}
      </View>

      <View style={styles.card}>
        <Text style={styles.sectionTitle}>Repayment Schedule</Text>
        {(schedule ?? []).map((entry) => (
          <View
            key={entry.id}
            style={[styles.scheduleRow, entry.status === 'OVERDUE' && styles.overdueRow]}
          >
            <Text style={styles.scheduleNum}>#{entry.installmentNumber}</Text>
            <View style={{ flex: 1 }}>
              <Text style={styles.scheduleDate}>{entry.dueDate}</Text>
              <Text style={styles.scheduleStatus}>{entry.status}</Text>
            </View>
            <View style={{ alignItems: 'flex-end' }}>
              <Text style={styles.scheduleAmount}>{inr(entry.installmentAmount)}</Text>
              <Text style={styles.schedulePaid}>Paid: {inr(entry.totalPaid)}</Text>
            </View>
          </View>
        ))}
      </View>
    </ScrollView>
  );
}

function Row({
  label,
  value,
  valueColor = '#111827',
}: {
  label: string;
  value: string;
  valueColor?: string;
}) {
  return (
    <View style={styles.row}>
      <Text style={styles.rowLabel}>{label}</Text>
      <Text style={[styles.rowValue, { color: valueColor }]}>{value}</Text>
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: '#f3f4f6', padding: 16 },
  center: { flex: 1, justifyContent: 'center', alignItems: 'center' },
  empty: { color: '#6b7280', fontSize: 16 },
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
  sectionTitle: { fontSize: 16, fontWeight: '700', color: '#111827', marginBottom: 12 },
  row: { flexDirection: 'row', justifyContent: 'space-between', marginBottom: 8 },
  rowLabel: { fontSize: 14, color: '#6b7280' },
  rowValue: { fontSize: 14, fontWeight: '600' },
  scheduleRow: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingVertical: 10,
    borderBottomWidth: 1,
    borderBottomColor: '#f3f4f6',
  },
  overdueRow: { backgroundColor: '#fef2f2', borderRadius: 8, paddingHorizontal: 6 },
  scheduleNum: { width: 36, fontSize: 14, fontWeight: '600', color: '#6b7280' },
  scheduleDate: { fontSize: 14, fontWeight: '500', color: '#111827' },
  scheduleStatus: { fontSize: 11, color: '#6b7280' },
  scheduleAmount: { fontSize: 14, fontWeight: '700', color: '#111827' },
  schedulePaid: { fontSize: 11, color: '#059669' },
});
