/**
 * Collection Report Component
 * Displays collection performance with real data from APIs
 */

import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import {
  CurrencyRupeeIcon,
  ChartBarIcon,
  ExclamationTriangleIcon,
  CheckCircleIcon,
} from '@heroicons/react/24/outline';
import { Card, CardHeader, Spinner } from '@/components/ui';
import { reportService, type DailyCollection } from '@/services/reportService';

export function CollectionReport() {
  const [dateRange, setDateRange] = useState('this_month');

  const { data: summary, isLoading } = useQuery({
    queryKey: ['report-collection', dateRange],
    queryFn: () => reportService.getCollectionSummary(dateRange),
    refetchInterval: 60000, // Refresh every minute
  });

  const { data: dailyData, isLoading: isLoadingDaily } = useQuery({
    queryKey: ['report-daily-collection'],
    queryFn: () => reportService.getDailyCollections(7),
    refetchInterval: 60000,
  });

  if (isLoading) {
    return <div className="p-12 text-center"><Spinner /></div>;
  }

  // Calculate max amount for chart scaling
  const maxDailyAmount = dailyData
    ? Math.max(...dailyData.map((d: DailyCollection) => d.amount), 1)
    : 1;

  // Determine efficiency status
  const efficiencyStatus = summary
    ? summary.collectionEfficiency >= 90 ? 'excellent'
      : summary.collectionEfficiency >= 70 ? 'good'
      : summary.collectionEfficiency >= 50 ? 'fair'
      : 'poor'
    : 'unknown';

  const efficiencyColors = {
    excellent: 'text-green-600 bg-green-50',
    good: 'text-blue-600 bg-blue-50',
    fair: 'text-yellow-600 bg-yellow-50',
    poor: 'text-red-600 bg-red-50',
    unknown: 'text-gray-600 bg-gray-50',
  };

  return (
    <div className="space-y-6">
      {/* Date Range Selector */}
      <div className="flex gap-2 mb-4">
        <select
          className="rounded-md border-gray-300 shadow-sm focus:border-primary-500 focus:ring-primary-500 sm:text-sm"
          value={dateRange}
          onChange={(e) => setDateRange(e.target.value)}
        >
          <option value="this_week">This Week</option>
          <option value="this_month">This Month</option>
          <option value="last_month">Last Month</option>
          <option value="this_quarter">This Quarter</option>
        </select>
      </div>

      {/* Summary Cards */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
        <Card className="bg-white">
          <div className="flex items-center gap-3">
            <div className="p-2 bg-blue-100 rounded-lg">
              <ChartBarIcon className="h-5 w-5 text-blue-600" />
            </div>
            <div>
              <div className="text-sm font-medium text-gray-500">Expected Collection</div>
              <div className="text-xl font-bold text-gray-900">
                ₹{(summary?.expectedCollection ?? 0).toLocaleString()}
              </div>
            </div>
          </div>
        </Card>

        <Card className="bg-white">
          <div className="flex items-center gap-3">
            <div className="p-2 bg-green-100 rounded-lg">
              <CurrencyRupeeIcon className="h-5 w-5 text-green-600" />
            </div>
            <div>
              <div className="text-sm font-medium text-gray-500">Actual Collection</div>
              <div className="text-xl font-bold text-green-600">
                ₹{(summary?.actualCollection ?? 0).toLocaleString()}
              </div>
              <div className="text-xs text-gray-500">
                {summary?.paymentsCount ?? 0} payment{(summary?.paymentsCount ?? 0) !== 1 ? 's' : ''}
              </div>
            </div>
          </div>
        </Card>

        <Card className={`${efficiencyColors[efficiencyStatus]}`}>
          <div className="flex items-center gap-3">
            <div className={`p-2 rounded-lg ${
              efficiencyStatus === 'excellent' ? 'bg-green-200' :
              efficiencyStatus === 'good' ? 'bg-blue-200' :
              efficiencyStatus === 'fair' ? 'bg-yellow-200' :
              'bg-red-200'
            }`}>
              <CheckCircleIcon className={`h-5 w-5 ${
                efficiencyStatus === 'excellent' ? 'text-green-700' :
                efficiencyStatus === 'good' ? 'text-blue-700' :
                efficiencyStatus === 'fair' ? 'text-yellow-700' :
                'text-red-700'
              }`} />
            </div>
            <div>
              <div className="text-sm font-medium opacity-80">Collection Efficiency</div>
              <div className="text-xl font-bold">
                {summary?.collectionEfficiency ?? 0}%
              </div>
              <div className="text-xs capitalize opacity-70">{efficiencyStatus}</div>
            </div>
          </div>
        </Card>

        <Card className="bg-white">
          <div className="flex items-center gap-3">
            <div className="p-2 bg-red-100 rounded-lg">
              <ExclamationTriangleIcon className="h-5 w-5 text-red-600" />
            </div>
            <div>
              <div className="text-sm font-medium text-gray-500">Overdue Amount</div>
              <div className="text-xl font-bold text-red-600">
                ₹{(summary?.overdueAmount ?? 0).toLocaleString()}
              </div>
            </div>
          </div>
        </Card>
      </div>

      {/* Daily Collection Chart */}
      <Card>
        <CardHeader
          title="Collection Trends (Last 7 Days)"
          subtitle="Daily payment collection summary"
        />
        {isLoadingDaily ? (
          <div className="flex justify-center py-8">
            <Spinner />
          </div>
        ) : dailyData && dailyData.length > 0 ? (
          <div className="mt-4 space-y-3">
            {dailyData.map((item: DailyCollection) => (
              <div key={item.date} className="flex items-center gap-4">
                <div className="w-24 text-sm text-gray-500">
                  {new Date(item.date).toLocaleDateString('en-US', {
                    weekday: 'short',
                    month: 'short',
                    day: 'numeric',
                  })}
                </div>
                <div className="flex-1 bg-gray-100 rounded-full h-6 overflow-hidden">
                  {item.amount > 0 ? (
                    <div
                      className="bg-primary-500 h-full rounded-full flex items-center justify-end pr-2 text-xs font-medium text-white transition-all duration-300"
                      style={{ width: `${Math.max((item.amount / maxDailyAmount) * 100, 5)}%` }}
                    >
                      {item.count > 0 && <span>{item.count}</span>}
                    </div>
                  ) : (
                    <div className="h-full flex items-center pl-2 text-xs text-gray-400">
                      No collections
                    </div>
                  )}
                </div>
                <div className="w-28 text-sm font-medium text-right">
                  {item.amount > 0 ? `₹${item.amount.toLocaleString()}` : '-'}
                </div>
              </div>
            ))}

            {/* Total for the period */}
            <div className="flex items-center gap-4 pt-3 border-t border-gray-200 mt-3">
              <div className="w-24 text-sm font-medium text-gray-700">Total</div>
              <div className="flex-1" />
              <div className="w-28 text-sm font-bold text-right text-primary-600">
                ₹{dailyData.reduce((sum: number, d: DailyCollection) => sum + d.amount, 0).toLocaleString()}
              </div>
            </div>
          </div>
        ) : (
          <div className="text-center py-8 text-gray-500">
            <CurrencyRupeeIcon className="h-12 w-12 mx-auto text-gray-300 mb-2" />
            <p>No collection data for this period</p>
            <p className="text-sm mt-1">Payments will appear here once recorded</p>
          </div>
        )}
      </Card>

      {/* Collection Tips */}
      {summary && summary.collectionEfficiency < 90 && (
        <Card className="bg-yellow-50 border-yellow-200">
          <div className="flex items-start gap-3">
            <ExclamationTriangleIcon className="h-6 w-6 text-yellow-600 flex-shrink-0 mt-0.5" />
            <div>
              <h3 className="font-medium text-yellow-800">Collection Improvement Tips</h3>
              <ul className="mt-2 text-sm text-yellow-700 space-y-1">
                <li>• Follow up with borrowers who have overdue payments</li>
                <li>• Send payment reminders before due dates</li>
                <li>• Review loans with multiple missed payments</li>
                <li>• Consider restructuring options for struggling borrowers</li>
              </ul>
            </div>
          </div>
        </Card>
      )}
    </div>
  );
}
