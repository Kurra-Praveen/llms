/**
 * Dashboard Page
 * Displays real-time statistics and activity from the loan management system
 */

import { useNavigate } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import {
  UsersIcon,
  BanknotesIcon,
  CreditCardIcon,
  ExclamationTriangleIcon,
  ClockIcon,
  DocumentCheckIcon,
} from '@heroicons/react/24/outline';
import { Card, CardHeader, Spinner } from '@/components/ui';
import { useAuth } from '@/contexts/AuthContext';
import { dashboardService, type RecentActivity } from '@/services/dashboardService';

interface StatCardProps {
  title: string;
  value: string | number;
  subtitle?: string;
  icon: React.ComponentType<{ className?: string }>;
  iconBgColor: string;
  onClick?: () => void;
}

function StatCard({ title, value, subtitle, icon: Icon, iconBgColor, onClick }: StatCardProps) {
  return (
    <Card className={onClick ? 'cursor-pointer hover:shadow-md transition-shadow' : ''} onClick={onClick}>
      <div className="flex items-center gap-4">
        <div className={`p-3 rounded-lg ${iconBgColor}`}>
          <Icon className="h-6 w-6 text-white" />
        </div>
        <div className="flex-1">
          <p className="text-sm font-medium text-gray-500">{title}</p>
          <p className="text-2xl font-bold text-gray-900">{value}</p>
          {subtitle && (
            <p className="text-xs text-gray-400 mt-1">{subtitle}</p>
          )}
        </div>
      </div>
    </Card>
  );
}

const activityTypeColors: Record<string, string> = {
  loan_created: 'bg-blue-100 text-blue-800',
  loan_approved: 'bg-indigo-100 text-indigo-800',
  payment_received: 'bg-green-100 text-green-800',
  loan_disbursed: 'bg-purple-100 text-purple-800',
  borrower_added: 'bg-yellow-100 text-yellow-800',
};

const activityTypeLabels: Record<string, string> = {
  loan_created: 'New Loan',
  loan_approved: 'Approved',
  payment_received: 'Payment',
  loan_disbursed: 'Disbursed',
  borrower_added: 'New Borrower',
};

export function DashboardPage() {
  const { user } = useAuth();
  const navigate = useNavigate();

  // Fetch dashboard stats
  const { data: stats, isLoading: isLoadingStats } = useQuery({
    queryKey: ['dashboard-stats'],
    queryFn: () => dashboardService.getStats(),
    refetchInterval: 60000, // Refresh every minute
  });

  // Fetch recent activity
  const { data: recentActivity, isLoading: isLoadingActivity } = useQuery({
    queryKey: ['dashboard-activity'],
    queryFn: () => dashboardService.getRecentActivity(),
    refetchInterval: 30000, // Refresh every 30 seconds
  });

  if (isLoadingStats) {
    return <Spinner className="mx-auto mt-20" />;
  }

  return (
    <div className="space-y-6">
      {/* Page header */}
      <div>
        <h1 className="text-2xl font-bold text-gray-900">Dashboard</h1>
        <p className="text-gray-500 mt-1">
          Welcome back, {user?.firstName}! Here's what's happening today.
        </p>
      </div>

      {/* Stats grid */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
        <StatCard
          title="Total Borrowers"
          value={stats?.totalBorrowers?.toLocaleString() ?? '0'}
          subtitle="Registered customers"
          icon={UsersIcon}
          iconBgColor="bg-blue-500"
          onClick={() => navigate('/borrowers')}
        />
        <StatCard
          title="Active Loans"
          value={stats?.activeLoans?.toLocaleString() ?? '0'}
          subtitle="Currently disbursed"
          icon={BanknotesIcon}
          iconBgColor="bg-green-500"
          onClick={() => navigate('/loans?status=ACTIVE')}
        />
        <StatCard
          title="Pending Approval"
          value={stats?.pendingApprovalLoans?.toLocaleString() ?? '0'}
          subtitle="Awaiting review"
          icon={ClockIcon}
          iconBgColor="bg-yellow-500"
          onClick={() => navigate('/loans?status=DRAFT')}
        />
        <StatCard
          title="Total Outstanding"
          value={`₹${(stats?.totalOutstanding ?? 0).toLocaleString()}`}
          subtitle="Principal remaining"
          icon={CreditCardIcon}
          iconBgColor="bg-purple-500"
        />
      </div>

      {/* Content grid */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Recent Activity */}
        <Card>
          <CardHeader title="Recent Activity" subtitle="Latest actions in the system" />
          {isLoadingActivity ? (
            <div className="flex justify-center py-8">
              <Spinner />
            </div>
          ) : recentActivity && recentActivity.length > 0 ? (
            <div className="space-y-4">
              {recentActivity.map((activity: RecentActivity) => (
                <div key={activity.id} className="flex items-start gap-3">
                  <div
                    className={`px-2 py-1 rounded text-xs font-medium whitespace-nowrap ${
                      activityTypeColors[activity.type] || 'bg-gray-100 text-gray-800'
                    }`}
                  >
                    {activityTypeLabels[activity.type] || activity.type.replace('_', ' ')}
                  </div>
                  <div className="flex-1 min-w-0">
                    <p className="text-sm text-gray-900 truncate">{activity.description}</p>
                    <p className="text-xs text-gray-500 mt-1">{activity.time}</p>
                  </div>
                </div>
              ))}
            </div>
          ) : (
            <div className="text-center py-8 text-gray-500">
              <DocumentCheckIcon className="h-12 w-12 mx-auto text-gray-300 mb-2" />
              <p>No recent activity</p>
              <p className="text-sm">Activity will appear here as you use the system</p>
            </div>
          )}
        </Card>

        {/* Quick Actions */}
        <Card>
          <CardHeader title="Quick Actions" subtitle="Frequently used actions" />
          <div className="grid grid-cols-2 gap-3">
            <button
              onClick={() => navigate('/borrowers/new')}
              className="p-4 border border-gray-200 rounded-lg hover:bg-gray-50 transition-colors text-left"
            >
              <UsersIcon className="h-6 w-6 text-blue-500 mb-2" />
              <p className="font-medium text-gray-900">Add Borrower</p>
              <p className="text-sm text-gray-500">Register new customer</p>
            </button>
            <button
              onClick={() => navigate('/loans/new')}
              className="p-4 border border-gray-200 rounded-lg hover:bg-gray-50 transition-colors text-left"
            >
              <BanknotesIcon className="h-6 w-6 text-green-500 mb-2" />
              <p className="font-medium text-gray-900">Create Loan</p>
              <p className="text-sm text-gray-500">New loan application</p>
            </button>
            <button
              onClick={() => navigate('/payments/new')}
              className="p-4 border border-gray-200 rounded-lg hover:bg-gray-50 transition-colors text-left"
            >
              <CreditCardIcon className="h-6 w-6 text-purple-500 mb-2" />
              <p className="font-medium text-gray-900">Record Payment</p>
              <p className="text-sm text-gray-500">Collect payment</p>
            </button>
            <button
              onClick={() => navigate('/loans?status=DRAFT')}
              className="p-4 border border-gray-200 rounded-lg hover:bg-gray-50 transition-colors text-left"
            >
              <ClockIcon className="h-6 w-6 text-yellow-500 mb-2" />
              <p className="font-medium text-gray-900">Pending Loans</p>
              <p className="text-sm text-gray-500">Review applications</p>
            </button>
          </div>
        </Card>
      </div>

      {/* Pending loans alert (if any) */}
      {(stats?.pendingApprovalLoans ?? 0) > 0 && (
        <Card className="bg-yellow-50 border-yellow-200">
          <div className="flex items-center gap-4">
            <ClockIcon className="h-8 w-8 text-yellow-600" />
            <div>
              <h3 className="font-semibold text-yellow-800">
                {stats?.pendingApprovalLoans} loan{(stats?.pendingApprovalLoans ?? 0) > 1 ? 's' : ''} pending approval
              </h3>
              <p className="text-sm text-yellow-700">
                Review and approve or reject pending loan applications.
              </p>
            </div>
            <button
              onClick={() => navigate('/loans?status=DRAFT')}
              className="ml-auto px-4 py-2 bg-yellow-600 text-white rounded-lg hover:bg-yellow-700 transition-colors"
            >
              Review Now
            </button>
          </div>
        </Card>
      )}

      {/* Overdue loans alert (if any) */}
      {(stats?.overdueLoans ?? 0) > 0 && (
        <Card className="bg-red-50 border-red-200">
          <div className="flex items-center gap-4">
            <ExclamationTriangleIcon className="h-8 w-8 text-red-500" />
            <div>
              <h3 className="font-semibold text-red-800">
                {stats?.overdueLoans} loan{(stats?.overdueLoans ?? 0) > 1 ? 's are' : ' is'} overdue
              </h3>
              <p className="text-sm text-red-700">
                Please review and take action on overdue accounts.
              </p>
            </div>
            <button
              onClick={() => navigate('/loans?status=OVERDUE')}
              className="ml-auto px-4 py-2 bg-red-600 text-white rounded-lg hover:bg-red-700 transition-colors"
            >
              View Overdue
            </button>
          </div>
        </Card>
      )}
    </div>
  );
}

export default DashboardPage;
