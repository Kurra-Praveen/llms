/**
 * Portfolio Report Component
 * Displays portfolio summary with real data from APIs
 */

import { useQuery } from '@tanstack/react-query';
import { Card, CardHeader, Spinner } from '@/components/ui';
import { reportService, type LoansByStatus } from '@/services/reportService';

export function PortfolioReport() {
  const { data: summary, isLoading } = useQuery({
    queryKey: ['report-portfolio'],
    queryFn: reportService.getPortfolioSummary,
    refetchInterval: 60000, // Refresh every minute
  });

  const { data: loansByStatus } = useQuery({
    queryKey: ['report-loans-by-status'],
    queryFn: reportService.getLoansByStatus,
  });

  if (isLoading) {
    return <div className="p-12 text-center"><Spinner /></div>;
  }

  if (!summary) {
    return (
      <div className="text-center py-12 text-gray-500">
        <p>No portfolio data available yet.</p>
        <p className="text-sm mt-1">Create and disburse loans to see portfolio statistics.</p>
      </div>
    );
  }

  // Calculate max amount for status chart scaling
  const maxStatusAmount = loansByStatus
    ? Math.max(...loansByStatus.map((s: LoansByStatus) => s.count), 1)
    : 1;

  return (
    <div className="space-y-6">
      {/* Summary Cards */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
        <Card className="bg-blue-50 border-blue-100">
          <div className="text-sm font-medium text-blue-600 mb-1">Total Disbursed</div>
          <div className="text-2xl font-bold text-blue-900">
            ₹{summary.totalDisbursed.toLocaleString()}
          </div>
          <div className="text-xs text-blue-600 mt-1">
            {summary.totalLoansCount} total loans
          </div>
        </Card>
        <Card className="bg-green-50 border-green-100">
          <div className="text-sm font-medium text-green-600 mb-1">Total Repaid</div>
          <div className="text-2xl font-bold text-green-900">
            ₹{summary.totalRepaid.toLocaleString()}
          </div>
          <div className="text-xs text-green-600 mt-1">
            {summary.closedLoansCount} loans closed
          </div>
        </Card>
        <Card className="bg-purple-50 border-purple-100">
          <div className="text-sm font-medium text-purple-600 mb-1">Outstanding Principal</div>
          <div className="text-2xl font-bold text-purple-900">
            ₹{summary.totalOutstanding.toLocaleString()}
          </div>
          <div className="text-xs text-purple-600 mt-1">
            Avg: ₹{summary.averageLoanSize.toLocaleString()}
          </div>
        </Card>
        <Card className="bg-gray-50 border-gray-200">
          <div className="text-sm font-medium text-gray-600 mb-1">Active Loans</div>
          <div className="text-2xl font-bold text-gray-900">{summary.activeLoansCount}</div>
          <div className="text-xs text-gray-500 mt-1">
            Currently disbursed
          </div>
        </Card>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Portfolio at Risk */}
        <Card>
          <CardHeader title="Portfolio at Risk (PAR)" subtitle="Percentage of loans with overdue payments" />
          <div className="space-y-4">
            <div>
              <div className="flex justify-between text-sm mb-1">
                <span className="text-gray-600">PAR 30 (30+ days overdue)</span>
                <span className="font-medium text-yellow-600">{summary.par30}%</span>
              </div>
              <div className="w-full bg-gray-200 rounded-full h-2">
                <div
                  className="bg-yellow-500 h-2 rounded-full transition-all duration-500"
                  style={{ width: `${Math.min(summary.par30 * 10, 100)}%` }}
                />
              </div>
            </div>
            <div>
              <div className="flex justify-between text-sm mb-1">
                <span className="text-gray-600">PAR 60 (60+ days overdue)</span>
                <span className="font-medium text-orange-600">{summary.par60}%</span>
              </div>
              <div className="w-full bg-gray-200 rounded-full h-2">
                <div
                  className="bg-orange-500 h-2 rounded-full transition-all duration-500"
                  style={{ width: `${Math.min(summary.par60 * 10, 100)}%` }}
                />
              </div>
            </div>
            <div>
              <div className="flex justify-between text-sm mb-1">
                <span className="text-gray-600">PAR 90 (90+ days overdue)</span>
                <span className="font-medium text-red-600">{summary.par90}%</span>
              </div>
              <div className="w-full bg-gray-200 rounded-full h-2">
                <div
                  className="bg-red-500 h-2 rounded-full transition-all duration-500"
                  style={{ width: `${Math.min(summary.par90 * 10, 100)}%` }}
                />
              </div>
            </div>

            {summary.par30 === 0 && summary.par60 === 0 && summary.par90 === 0 && (
              <div className="text-center py-4 text-green-600 bg-green-50 rounded-lg">
                <p className="font-medium">Excellent! No overdue loans</p>
              </div>
            )}
          </div>
        </Card>

        {/* Loans by Status */}
        <Card>
          <CardHeader title="Loans by Status" subtitle="Distribution of loans across statuses" />
          {loansByStatus && loansByStatus.length > 0 ? (
            <div className="space-y-3">
              {loansByStatus.map((item: LoansByStatus) => (
                <div key={item.status} className="flex items-center gap-4">
                  <div className="w-32 text-sm text-gray-600 capitalize">
                    {item.status.toLowerCase().replace('_', ' ')}
                  </div>
                  <div className="flex-1 bg-gray-100 rounded-full h-6 overflow-hidden">
                    <div
                      className={`h-full rounded-full flex items-center justify-end pr-2 text-xs font-medium text-white ${
                        item.status === 'ACTIVE' ? 'bg-green-500' :
                        item.status === 'CLOSED' ? 'bg-blue-500' :
                        item.status === 'DRAFT' ? 'bg-gray-400' :
                        item.status === 'APPROVED' ? 'bg-purple-500' :
                        'bg-yellow-500'
                      }`}
                      style={{ width: `${Math.max((item.count / maxStatusAmount) * 100, 10)}%` }}
                    >
                      {item.count > 0 ? item.count : ''}
                    </div>
                  </div>
                  <div className="w-12 text-sm font-medium text-right">{item.count}</div>
                </div>
              ))}
            </div>
          ) : (
            <div className="text-center py-8 text-gray-500">
              <p>No loan data available</p>
            </div>
          )}
        </Card>
      </div>

      {/* Summary Stats */}
      <Card>
        <CardHeader title="Portfolio Health" />
        <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
          <div className="text-center p-4 bg-gray-50 rounded-lg">
            <div className="text-3xl font-bold text-gray-900">{summary.totalLoansCount}</div>
            <div className="text-sm text-gray-500">Total Loans</div>
          </div>
          <div className="text-center p-4 bg-green-50 rounded-lg">
            <div className="text-3xl font-bold text-green-600">{summary.activeLoansCount}</div>
            <div className="text-sm text-gray-500">Active</div>
          </div>
          <div className="text-center p-4 bg-blue-50 rounded-lg">
            <div className="text-3xl font-bold text-blue-600">{summary.closedLoansCount}</div>
            <div className="text-sm text-gray-500">Closed</div>
          </div>
          <div className="text-center p-4 bg-purple-50 rounded-lg">
            <div className="text-3xl font-bold text-purple-600">
              {summary.totalDisbursed > 0
                ? Math.round((summary.totalRepaid / summary.totalDisbursed) * 100)
                : 0}%
            </div>
            <div className="text-sm text-gray-500">Recovery Rate</div>
          </div>
        </div>
      </Card>
    </div>
  );
}
