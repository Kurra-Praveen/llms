/**
 * Borrower Portal — Dashboard
 * Shows active loans, total outstanding, and the next EMI due.
 */

import { useQuery } from '@tanstack/react-query';
import { useNavigate } from 'react-router-dom';
import {
  BanknotesIcon,
  CreditCardIcon,
  CalendarDaysIcon,
} from '@heroicons/react/24/outline';
import { Card, CardHeader, Button, LoadingScreen } from '@/components/ui';
import { portalService } from '@/services';
import { useAuth } from '@/contexts/AuthContext';
import { logger } from '@/utils/logger';

const dashboardLogger = logger.scope('PortalDashboardPage');

const formatINR = (amount?: number) =>
  `₹${(amount ?? 0).toLocaleString('en-IN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`;

export function PortalDashboardPage() {
  const { user } = useAuth();
  const navigate = useNavigate();

  const { data: summary, isLoading } = useQuery({
    queryKey: ['portal', 'summary'],
    queryFn: () => portalService.getMySummary(),
  });

  dashboardLogger.debug('Portal dashboard render', { userId: user?.id });

  if (isLoading) {
    return <LoadingScreen message="Loading your dashboard..." />;
  }

  const nextEmi = summary?.nextEmi;

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold text-gray-900">
          Welcome back, {user?.firstName}!
        </h1>
        <p className="text-gray-500 mt-1">Here's an overview of your loans</p>
      </div>

      {/* Summary cards */}
      <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
        <Card>
          <div className="flex items-center gap-4">
            <div className="p-3 bg-primary-50 rounded-lg">
              <BanknotesIcon className="h-6 w-6 text-primary-600" />
            </div>
            <div>
              <p className="text-sm text-gray-500">Active Loans</p>
              <p className="text-2xl font-bold text-gray-900">
                {summary?.activeLoansCount ?? 0}
              </p>
            </div>
          </div>
        </Card>

        <Card>
          <div className="flex items-center gap-4">
            <div className="p-3 bg-warning-50 rounded-lg">
              <CreditCardIcon className="h-6 w-6 text-warning-600" />
            </div>
            <div>
              <p className="text-sm text-gray-500">Total Outstanding</p>
              <p className="text-2xl font-bold text-gray-900">
                {formatINR(summary?.totalOutstanding)}
              </p>
            </div>
          </div>
        </Card>
      </div>

      {/* Next EMI */}
      <Card>
        <CardHeader
          title="Next EMI"
          subtitle={nextEmi ? `Loan ${nextEmi.loanNumber ?? ''}` : 'No upcoming payments'}
          action={<CalendarDaysIcon className="h-6 w-6 text-gray-400" />}
        />
        {nextEmi ? (
          <div className="space-y-3">
            <div className="flex justify-between items-center">
              <span className="text-gray-600">Amount due</span>
              <span className="text-xl font-bold text-gray-900">
                {formatINR(nextEmi.outstandingAmount)}
              </span>
            </div>
            <div className="flex justify-between items-center">
              <span className="text-gray-600">Due date</span>
              <span className="font-medium text-gray-900">{nextEmi.dueDate}</span>
            </div>
            {nextEmi.status === 'OVERDUE' && (
              <div className="bg-red-50 border border-red-200 text-red-700 px-4 py-2 rounded-lg text-sm">
                Overdue by {nextEmi.daysPastDue ?? 0} day(s). Please pay immediately to
                avoid additional penalties.
              </div>
            )}
            <Button
              variant="primary"
              fullWidth
              onClick={() => navigate(`/portal/loans/${nextEmi.loanId}`)}
            >
              View Loan Details
            </Button>
          </div>
        ) : (
          <p className="text-gray-500 text-sm">
            You're all caught up! No pending installments.
          </p>
        )}
      </Card>

      {/* Quick actions */}
      <div className="grid grid-cols-2 gap-4">
        <Button variant="outline" onClick={() => navigate('/portal/loans')}>
          My Loans
        </Button>
        <Button variant="outline" onClick={() => navigate('/portal/payments')}>
          Payment History
        </Button>
      </div>
    </div>
  );
}

export default PortalDashboardPage;
