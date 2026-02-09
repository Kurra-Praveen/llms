/**
 * Aging Report Component
 * Displays loan aging analysis by DPD buckets
 */

import { useQuery } from '@tanstack/react-query';
import { Card, CardHeader, Spinner } from '@/components/ui';
import { reportService, type AgingBucket } from '@/services/reportService';

const bucketColors: Record<string, { bg: string; text: string; bar: string }> = {
  CURRENT: { bg: 'bg-green-50', text: 'text-green-700', bar: 'bg-green-500' },
  '1-30': { bg: 'bg-yellow-50', text: 'text-yellow-700', bar: 'bg-yellow-500' },
  '31-60': { bg: 'bg-orange-50', text: 'text-orange-700', bar: 'bg-orange-500' },
  '61-90': { bg: 'bg-red-50', text: 'text-red-700', bar: 'bg-red-500' },
  '90+': { bg: 'bg-red-100', text: 'text-red-800', bar: 'bg-red-700' },
};

export function AgingReport() {
  const { data: report, isLoading } = useQuery({
    queryKey: ['report-aging'],
    queryFn: reportService.getAgingReport,
    refetchInterval: 60000,
  });

  if (isLoading) {
    return (
      <div className="p-12 text-center">
        <Spinner />
      </div>
    );
  }

  if (!report || report.totalLoans === 0) {
    return (
      <div className="text-center py-12 text-gray-500 bg-white rounded-lg border border-gray-200">
        <p className="text-lg font-medium text-gray-900">No Active Loans</p>
        <p className="mt-1">Aging analysis will appear when there are active loans in the portfolio.</p>
      </div>
    );
  }

  // Calculate max for chart scaling
  const maxOutstanding = Math.max(...report.buckets.map(b => b.totalOutstanding), 1);

  return (
    <div className="space-y-6">
      {/* Summary Cards */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
        <Card className="bg-blue-50 border-blue-100">
          <div className="text-center">
            <div className="text-sm font-medium text-blue-600 mb-1">Total Active Loans</div>
            <div className="text-3xl font-bold text-blue-900">{report.totalLoans}</div>
          </div>
        </Card>
        <Card className="bg-purple-50 border-purple-100">
          <div className="text-center">
            <div className="text-sm font-medium text-purple-600 mb-1">Total Outstanding</div>
            <div className="text-3xl font-bold text-purple-900">
              ₹{report.totalOutstanding.toLocaleString()}
            </div>
          </div>
        </Card>
        <Card className="bg-gray-50 border-gray-200">
          <div className="text-center">
            <div className="text-sm font-medium text-gray-600 mb-1">Report Generated</div>
            <div className="text-lg font-semibold text-gray-900">
              {new Date(report.generatedAt).toLocaleString()}
            </div>
          </div>
        </Card>
      </div>

      {/* Aging Distribution Chart */}
      <Card>
        <CardHeader title="Aging Distribution" subtitle="Outstanding amount by DPD bucket" />
        <div className="space-y-4">
          {report.buckets.map((bucket: AgingBucket) => {
            const colors = bucketColors[bucket.bucket] || bucketColors['CURRENT'];
            const barWidth = (bucket.totalOutstanding / maxOutstanding) * 100;

            return (
              <div key={bucket.bucket} className="flex items-center gap-4">
                <div className="w-32 text-sm font-medium text-gray-700">{bucket.label}</div>
                <div className="flex-1">
                  <div className="w-full bg-gray-100 rounded-full h-8 overflow-hidden">
                    <div
                      className={`h-full ${colors.bar} rounded-full flex items-center justify-end pr-3 transition-all duration-500`}
                      style={{ width: `${Math.max(barWidth, bucket.loansCount > 0 ? 5 : 0)}%` }}
                    >
                      {bucket.loansCount > 0 && (
                        <span className="text-xs font-medium text-white">
                          {bucket.loansCount} loans
                        </span>
                      )}
                    </div>
                  </div>
                </div>
                <div className="w-32 text-right">
                  <span className="font-semibold text-gray-900">
                    ₹{bucket.totalOutstanding.toLocaleString()}
                  </span>
                  <span className="text-xs text-gray-500 ml-1">
                    ({bucket.percentageOfPortfolio}%)
                  </span>
                </div>
              </div>
            );
          })}
        </div>
      </Card>

      {/* Detailed Table */}
      <Card>
        <CardHeader title="Detailed Aging Analysis" subtitle="Breakdown by component" />
        <div className="overflow-x-auto">
          <table className="min-w-full divide-y divide-gray-200">
            <thead className="bg-gray-50">
              <tr>
                <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  DPD Bucket
                </th>
                <th className="px-4 py-3 text-right text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Loans
                </th>
                <th className="px-4 py-3 text-right text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Principal
                </th>
                <th className="px-4 py-3 text-right text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Interest
                </th>
                <th className="px-4 py-3 text-right text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Penalty
                </th>
                <th className="px-4 py-3 text-right text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Total Outstanding
                </th>
                <th className="px-4 py-3 text-right text-xs font-medium text-gray-500 uppercase tracking-wider">
                  % of Portfolio
                </th>
              </tr>
            </thead>
            <tbody className="bg-white divide-y divide-gray-200">
              {report.buckets.map((bucket: AgingBucket) => {
                const colors = bucketColors[bucket.bucket] || bucketColors['CURRENT'];
                return (
                  <tr key={bucket.bucket} className={bucket.loansCount > 0 ? colors.bg : ''}>
                    <td className={`px-4 py-3 text-sm font-medium ${colors.text}`}>
                      {bucket.label}
                    </td>
                    <td className="px-4 py-3 text-sm text-right text-gray-900">
                      {bucket.loansCount}
                    </td>
                    <td className="px-4 py-3 text-sm text-right text-gray-900">
                      ₹{bucket.outstandingPrincipal.toLocaleString()}
                    </td>
                    <td className="px-4 py-3 text-sm text-right text-gray-900">
                      ₹{bucket.outstandingInterest.toLocaleString()}
                    </td>
                    <td className="px-4 py-3 text-sm text-right text-gray-900">
                      ₹{bucket.outstandingPenalty.toLocaleString()}
                    </td>
                    <td className="px-4 py-3 text-sm text-right font-semibold text-gray-900">
                      ₹{bucket.totalOutstanding.toLocaleString()}
                    </td>
                    <td className="px-4 py-3 text-sm text-right text-gray-900">
                      {bucket.percentageOfPortfolio}%
                    </td>
                  </tr>
                );
              })}
            </tbody>
            <tfoot className="bg-gray-100">
              <tr>
                <td className="px-4 py-3 text-sm font-bold text-gray-900">Total</td>
                <td className="px-4 py-3 text-sm text-right font-bold text-gray-900">
                  {report.totalLoans}
                </td>
                <td className="px-4 py-3 text-sm text-right font-bold text-gray-900">
                  ₹{report.buckets.reduce((sum, b) => sum + b.outstandingPrincipal, 0).toLocaleString()}
                </td>
                <td className="px-4 py-3 text-sm text-right font-bold text-gray-900">
                  ₹{report.buckets.reduce((sum, b) => sum + b.outstandingInterest, 0).toLocaleString()}
                </td>
                <td className="px-4 py-3 text-sm text-right font-bold text-gray-900">
                  ₹{report.buckets.reduce((sum, b) => sum + b.outstandingPenalty, 0).toLocaleString()}
                </td>
                <td className="px-4 py-3 text-sm text-right font-bold text-gray-900">
                  ₹{report.totalOutstanding.toLocaleString()}
                </td>
                <td className="px-4 py-3 text-sm text-right font-bold text-gray-900">
                  100%
                </td>
              </tr>
            </tfoot>
          </table>
        </div>
      </Card>

      {/* Risk Indicators */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
        <Card>
          <CardHeader title="Portfolio at Risk" subtitle="Loans overdue by 30+ days" />
          <div className="space-y-3">
            {[
              { label: 'PAR > 30', buckets: ['1-30', '31-60', '61-90', '90+'] },
              { label: 'PAR > 60', buckets: ['31-60', '61-90', '90+'] },
              { label: 'PAR > 90 (NPA)', buckets: ['61-90', '90+'] },
            ].map(({ label, buckets }) => {
              const parLoans = report.buckets
                .filter(b => buckets.includes(b.bucket))
                .reduce((sum, b) => sum + b.loansCount, 0);
              const parAmount = report.buckets
                .filter(b => buckets.includes(b.bucket))
                .reduce((sum, b) => sum + b.totalOutstanding, 0);
              const parPercentage = report.totalLoans > 0
                ? Math.round((parLoans / report.totalLoans) * 100 * 10) / 10
                : 0;

              return (
                <div key={label} className="flex items-center justify-between p-3 bg-gray-50 rounded-lg">
                  <span className="text-sm font-medium text-gray-700">{label}</span>
                  <div className="text-right">
                    <span className="text-lg font-bold text-gray-900">{parPercentage}%</span>
                    <span className="text-xs text-gray-500 ml-2">
                      ({parLoans} loans, ₹{parAmount.toLocaleString()})
                    </span>
                  </div>
                </div>
              );
            })}
          </div>
        </Card>

        <Card>
          <CardHeader title="Collection Priority" subtitle="Focus areas based on aging" />
          <div className="space-y-3">
            {report.buckets
              .filter(b => b.loansCount > 0 && b.bucket !== 'CURRENT')
              .sort((a, b) => b.totalOutstanding - a.totalOutstanding)
              .slice(0, 3)
              .map((bucket, index) => {
                const colors = bucketColors[bucket.bucket];
                return (
                  <div
                    key={bucket.bucket}
                    className={`flex items-center gap-3 p-3 rounded-lg ${colors.bg}`}
                  >
                    <div className={`w-8 h-8 rounded-full ${colors.bar} flex items-center justify-center text-white font-bold text-sm`}>
                      {index + 1}
                    </div>
                    <div className="flex-1">
                      <div className={`font-medium ${colors.text}`}>{bucket.label}</div>
                      <div className="text-sm text-gray-600">
                        {bucket.loansCount} loans outstanding
                      </div>
                    </div>
                    <div className="text-right">
                      <div className={`font-bold ${colors.text}`}>
                        ₹{bucket.totalOutstanding.toLocaleString()}
                      </div>
                    </div>
                  </div>
                );
              })}
            {report.buckets.filter(b => b.loansCount > 0 && b.bucket !== 'CURRENT').length === 0 && (
              <div className="text-center py-4 text-green-600 bg-green-50 rounded-lg">
                <p className="font-medium">Excellent! No overdue loans to collect.</p>
              </div>
            )}
          </div>
        </Card>
      </div>
    </div>
  );
}

export default AgingReport;
