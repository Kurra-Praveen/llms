/**
 * Borrower Portal — Payment history
 */

import { useQuery } from '@tanstack/react-query';
import { Card, StatusBadge, LoadingScreen } from '@/components/ui';
import { portalService } from '@/services';
import { logger } from '@/utils/logger';

const paymentsLogger = logger.scope('PortalPaymentsPage');

const formatINR = (amount?: number) =>
  `₹${(amount ?? 0).toLocaleString('en-IN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`;

export function PortalPaymentsPage() {
  const { data, isLoading } = useQuery({
    queryKey: ['portal', 'payments'],
    queryFn: () => portalService.getMyPayments(0, 50),
  });

  paymentsLogger.debug('Portal payments render', { count: data?.totalElements });

  if (isLoading) {
    return <LoadingScreen message="Loading your payments..." />;
  }

  const payments = data?.content ?? [];

  return (
    <div className="space-y-4">
      <h1 className="text-2xl font-bold text-gray-900">Payment History</h1>

      {payments.length === 0 ? (
        <Card>
          <p className="text-gray-500 text-center py-8">No payments recorded yet.</p>
        </Card>
      ) : (
        <div className="space-y-3">
          {payments.map((payment) => (
            <Card key={payment.id}>
              <div className="flex items-center justify-between">
                <div>
                  <div className="flex items-center gap-2">
                    <p className="font-semibold text-gray-900">
                      {formatINR(payment.amountPaid)}
                    </p>
                    <StatusBadge status={payment.status} type="payment" />
                  </div>
                  <p className="text-sm text-gray-500 mt-1">
                    {payment.loanNumber ?? 'Loan'} · {payment.paymentDate} ·{' '}
                    {payment.paymentMethod}
                  </p>
                  {payment.receiptNumber && (
                    <p className="text-xs text-gray-400 mt-1">
                      Receipt: {payment.receiptNumber}
                    </p>
                  )}
                </div>
              </div>
            </Card>
          ))}
        </div>
      )}
    </div>
  );
}

export default PortalPaymentsPage;
