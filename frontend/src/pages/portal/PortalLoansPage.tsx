/**
 * Borrower Portal — My Loans list
 */

import { useQuery } from '@tanstack/react-query';
import { useNavigate } from 'react-router-dom';
import { ChevronRightIcon } from '@heroicons/react/24/outline';
import { Card, StatusBadge, LoadingScreen } from '@/components/ui';
import { portalService } from '@/services';
import { logger } from '@/utils/logger';

const loansLogger = logger.scope('PortalLoansPage');

const formatINR = (amount?: number) =>
  `₹${(amount ?? 0).toLocaleString('en-IN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`;

export function PortalLoansPage() {
  const navigate = useNavigate();

  const { data, isLoading } = useQuery({
    queryKey: ['portal', 'loans'],
    queryFn: () => portalService.getMyLoans(0, 50),
  });

  loansLogger.debug('Portal loans render', { count: data?.totalElements });

  if (isLoading) {
    return <LoadingScreen message="Loading your loans..." />;
  }

  const loans = data?.content ?? [];

  return (
    <div className="space-y-4">
      <h1 className="text-2xl font-bold text-gray-900">My Loans</h1>

      {loans.length === 0 ? (
        <Card>
          <p className="text-gray-500 text-center py-8">
            You don't have any loans yet.
          </p>
        </Card>
      ) : (
        <div className="space-y-3">
          {loans.map((loan) => (
            <Card
              key={loan.id}
              onClick={() => navigate(`/portal/loans/${loan.id}`)}
              className="hover:shadow-md transition-shadow cursor-pointer"
            >
              <div className="flex items-center justify-between">
                <div className="flex-1 min-w-0">
                  <div className="flex items-center gap-2">
                    <p className="font-semibold text-gray-900 truncate">
                      {loan.loanNumber}
                    </p>
                    <StatusBadge status={loan.status} type="loan" />
                  </div>
                  <div className="mt-2 grid grid-cols-2 gap-x-4 gap-y-1 text-sm">
                    <div>
                      <span className="text-gray-500">Principal: </span>
                      <span className="font-medium">{formatINR(loan.principalAmount)}</span>
                    </div>
                    <div>
                      <span className="text-gray-500">EMI: </span>
                      <span className="font-medium">{formatINR(loan.emiAmount)}</span>
                    </div>
                    <div>
                      <span className="text-gray-500">Outstanding: </span>
                      <span className="font-medium text-primary-700">
                        {formatINR(loan.totalOutstanding)}
                      </span>
                    </div>
                    <div>
                      <span className="text-gray-500">Paid: </span>
                      <span className="font-medium text-success-700">
                        {formatINR(loan.totalPaid)}
                      </span>
                    </div>
                  </div>
                </div>
                <ChevronRightIcon className="h-5 w-5 text-gray-400 flex-shrink-0" />
              </div>
            </Card>
          ))}
        </div>
      )}
    </div>
  );
}

export default PortalLoansPage;
