/**
 * Borrower Portal — Loan Details with repayment schedule
 */

import { useQuery } from '@tanstack/react-query';
import { useNavigate, useParams } from 'react-router-dom';
import { ArrowLeftIcon } from '@heroicons/react/24/outline';
import { Button, Card, CardHeader, StatusBadge, LoadingScreen } from '@/components/ui';
import { portalService } from '@/services';
import { logger } from '@/utils/logger';

const detailsLogger = logger.scope('PortalLoanDetailsPage');

const formatINR = (amount?: number) =>
  `₹${(amount ?? 0).toLocaleString('en-IN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`;

export function PortalLoanDetailsPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();

  const { data: loan, isLoading: loanLoading } = useQuery({
    queryKey: ['portal', 'loans', id],
    queryFn: () => portalService.getMyLoan(id!),
    enabled: !!id,
  });

  const { data: schedule, isLoading: scheduleLoading } = useQuery({
    queryKey: ['portal', 'loans', id, 'schedule'],
    queryFn: () => portalService.getMyLoanSchedule(id!),
    enabled: !!id,
  });

  detailsLogger.debug('Portal loan details render', { loanId: id });

  if (loanLoading) {
    return <LoadingScreen message="Loading loan details..." />;
  }

  if (!loan) {
    return (
      <div className="text-center py-12">
        <h2 className="text-xl font-bold text-gray-900">Loan not found</h2>
        <Button variant="secondary" className="mt-4" onClick={() => navigate('/portal/loans')}>
          Back to My Loans
        </Button>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-center gap-4">
        <Button variant="ghost" size="sm" onClick={() => navigate('/portal/loans')} className="p-2">
          <ArrowLeftIcon className="h-5 w-5" />
        </Button>
        <div>
          <h1 className="text-2xl font-bold text-gray-900">{loan.loanNumber}</h1>
          <StatusBadge status={loan.status} type="loan" />
        </div>
      </div>

      {/* Loan summary */}
      <Card>
        <CardHeader title="Loan Summary" />
        <dl className="grid grid-cols-2 gap-x-4 gap-y-3 text-sm">
          <div>
            <dt className="text-gray-500">Principal</dt>
            <dd className="font-medium text-gray-900">{formatINR(loan.principalAmount)}</dd>
          </div>
          <div>
            <dt className="text-gray-500">Interest Rate</dt>
            <dd className="font-medium text-gray-900">{loan.interestRate}% ({loan.interestType})</dd>
          </div>
          <div>
            <dt className="text-gray-500">EMI</dt>
            <dd className="font-medium text-gray-900">{formatINR(loan.emiAmount)}</dd>
          </div>
          <div>
            <dt className="text-gray-500">Tenure</dt>
            <dd className="font-medium text-gray-900">{loan.tenureMonths} months</dd>
          </div>
          <div>
            <dt className="text-gray-500">Total Paid</dt>
            <dd className="font-medium text-success-700">{formatINR(loan.totalPaid)}</dd>
          </div>
          <div>
            <dt className="text-gray-500">Outstanding</dt>
            <dd className="font-medium text-primary-700">{formatINR(loan.totalOutstanding)}</dd>
          </div>
          {loan.disbursementDate && (
            <div>
              <dt className="text-gray-500">Disbursed On</dt>
              <dd className="font-medium text-gray-900">{loan.disbursementDate}</dd>
            </div>
          )}
          {loan.maturityDate && (
            <div>
              <dt className="text-gray-500">Matures On</dt>
              <dd className="font-medium text-gray-900">{loan.maturityDate}</dd>
            </div>
          )}
        </dl>
      </Card>

      {/* Repayment schedule */}
      <Card padding="none">
        <div className="p-6 pb-0">
          <CardHeader title="Repayment Schedule" subtitle="Your EMI installments" />
        </div>
        {scheduleLoading ? (
          <p className="text-gray-500 text-sm p-6">Loading schedule...</p>
        ) : (
          <div className="overflow-x-auto">
            <table className="min-w-full divide-y divide-gray-200">
              <thead className="bg-gray-50">
                <tr>
                  <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">#</th>
                  <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">Due Date</th>
                  <th className="px-4 py-3 text-right text-xs font-medium text-gray-500 uppercase">Amount</th>
                  <th className="px-4 py-3 text-right text-xs font-medium text-gray-500 uppercase">Paid</th>
                  <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">Status</th>
                </tr>
              </thead>
              <tbody className="bg-white divide-y divide-gray-200">
                {(schedule ?? []).map((entry) => (
                  <tr key={entry.id} className={entry.status === 'OVERDUE' ? 'bg-red-50' : ''}>
                    <td className="px-4 py-3 text-sm text-gray-900">{entry.installmentNumber}</td>
                    <td className="px-4 py-3 text-sm text-gray-900">{entry.dueDate}</td>
                    <td className="px-4 py-3 text-sm text-right text-gray-900">
                      {formatINR(entry.installmentAmount)}
                    </td>
                    <td className="px-4 py-3 text-sm text-right text-gray-900">
                      {formatINR(entry.totalPaid)}
                    </td>
                    <td className="px-4 py-3 text-sm">
                      <StatusBadge status={entry.status} type="loan" />
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </Card>
    </div>
  );
}

export default PortalLoanDetailsPage;
