/**
 * Report Service
 * Aggregates data from various APIs for reporting
 */

import api from './api';
import { logger } from '@/utils/logger';
import type { PagedResponse, Loan, Payment } from '@/types';

const reportLogger = logger.scope('ReportService');

export interface PortfolioSummary {
  totalDisbursed: number;
  totalOutstanding: number;
  totalRepaid: number;
  activeLoansCount: number;
  averageLoanSize: number;
  par30: number; // Portfolio At Risk > 30 days
  par60: number;
  par90: number;
  totalLoansCount: number;
  closedLoansCount: number;
}

export interface CollectionSummary {
  expectedCollection: number;
  actualCollection: number;
  collectionEfficiency: number;
  overdueAmount: number;
  paymentsCount: number;
}

export interface DailyCollection {
  date: string;
  amount: number;
  count: number;
}

export interface LoansByStatus {
  status: string;
  count: number;
  amount: number;
}

// Helper to calculate date ranges
function getDateRange(period: string): { startDate: string; endDate: string } {
  const now = new Date();
  const endDate = now.toISOString().split('T')[0];
  let startDate: string;

  switch (period) {
    case 'this_week':
      const weekStart = new Date(now);
      weekStart.setDate(now.getDate() - now.getDay());
      startDate = weekStart.toISOString().split('T')[0];
      break;
    case 'last_month':
      const lastMonthStart = new Date(now.getFullYear(), now.getMonth() - 1, 1);
      const lastMonthEnd = new Date(now.getFullYear(), now.getMonth(), 0);
      startDate = lastMonthStart.toISOString().split('T')[0];
      return { startDate, endDate: lastMonthEnd.toISOString().split('T')[0] };
    case 'this_quarter':
      const quarterMonth = Math.floor(now.getMonth() / 3) * 3;
      const quarterStart = new Date(now.getFullYear(), quarterMonth, 1);
      startDate = quarterStart.toISOString().split('T')[0];
      break;
    case 'this_month':
    default:
      const monthStart = new Date(now.getFullYear(), now.getMonth(), 1);
      startDate = monthStart.toISOString().split('T')[0];
      break;
  }

  return { startDate, endDate };
}

export const reportService = {
  /**
   * Get portfolio summary from real loan data
   */
  async getPortfolioSummary(): Promise<PortfolioSummary> {
    reportLogger.info('Fetching portfolio summary');

    try {
      // Fetch loans by status in parallel
      const [
        activeLoansResponse,
        closedLoansResponse,
        allLoansResponse,
      ] = await Promise.all([
        api.get<PagedResponse<Loan>>('/v1/loans/status/ACTIVE', { params: { page: 0, size: 100 } }),
        api.get<PagedResponse<Loan>>('/v1/loans/status/CLOSED', { params: { page: 0, size: 100 } }),
        api.get<PagedResponse<Loan>>('/v1/loans', { params: { page: 0, size: 100 } }),
      ]);

      const activeLoans = activeLoansResponse.data.data.content || [];
      const closedLoans = closedLoansResponse.data.data.content || [];
      const allLoans = allLoansResponse.data.data.content || [];

      // Calculate totals
      let totalDisbursed = 0;
      let totalOutstanding = 0;
      let totalRepaid = 0;
      let par30Count = 0;
      let par60Count = 0;
      let par90Count = 0;

      // Process active loans
      activeLoans.forEach((loan: Loan) => {
        totalDisbursed += loan.principalAmount || 0;
        totalOutstanding += loan.outstandingPrincipal || 0;
        totalRepaid += loan.totalPaid || 0;

        // Check DPD (Days Past Due) for PAR calculation
        const dpd = loan.dpd || 0;
        if (dpd >= 30) par30Count++;
        if (dpd >= 60) par60Count++;
        if (dpd >= 90) par90Count++;
      });

      // Add closed loans to totals
      closedLoans.forEach((loan: Loan) => {
        totalDisbursed += loan.principalAmount || 0;
        totalRepaid += loan.totalPaid || 0;
      });

      const activeLoansCount = activeLoansResponse.data.data.totalElements || activeLoans.length;
      const closedLoansCount = closedLoansResponse.data.data.totalElements || closedLoans.length;
      const totalLoansCount = allLoansResponse.data.data.totalElements || allLoans.length;

      // Calculate PAR percentages
      const par30 = activeLoansCount > 0 ? Math.round((par30Count / activeLoansCount) * 100 * 10) / 10 : 0;
      const par60 = activeLoansCount > 0 ? Math.round((par60Count / activeLoansCount) * 100 * 10) / 10 : 0;
      const par90 = activeLoansCount > 0 ? Math.round((par90Count / activeLoansCount) * 100 * 10) / 10 : 0;

      const summary: PortfolioSummary = {
        totalDisbursed,
        totalOutstanding,
        totalRepaid,
        activeLoansCount,
        averageLoanSize: activeLoansCount > 0 ? Math.round(totalDisbursed / (activeLoansCount + closedLoansCount)) : 0,
        par30,
        par60,
        par90,
        totalLoansCount,
        closedLoansCount,
      };

      reportLogger.info('Portfolio summary fetched', summary);
      return summary;
    } catch (error) {
      reportLogger.error('Failed to fetch portfolio summary', { error });
      throw error;
    }
  },

  /**
   * Get collection summary for a date range
   */
  async getCollectionSummary(period: string = 'this_month'): Promise<CollectionSummary> {
    const { startDate, endDate } = getDateRange(period);
    reportLogger.info('Fetching collection summary', { period, startDate, endDate });

    try {
      // Fetch payments and active loans
      const [paymentsResponse, activeLoansResponse] = await Promise.all([
        api.get<PagedResponse<Payment>>('/v1/payments', { params: { page: 0, size: 100 } }),
        api.get<PagedResponse<Loan>>('/v1/loans/status/ACTIVE', { params: { page: 0, size: 100 } }),
      ]);

      const payments = paymentsResponse.data.data.content || [];
      const activeLoans = activeLoansResponse.data.data.content || [];

      // Filter payments within date range
      const periodPayments = payments.filter((p: Payment) => {
        const paymentDate = p.paymentDate?.split('T')[0];
        return paymentDate && paymentDate >= startDate && paymentDate <= endDate;
      });

      // Calculate actual collection
      let actualCollection = 0;
      periodPayments.forEach((payment: Payment) => {
        actualCollection += payment.amountPaid || 0;
      });

      // Calculate expected collection (sum of EMI amounts for active loans)
      // This is a simplified calculation - in a real app, you'd query the repayment schedule
      let expectedCollection = 0;
      activeLoans.forEach((loan: Loan) => {
        expectedCollection += loan.emiAmount || 0;
      });

      // Calculate overdue (outstanding interest + penalty)
      let overdueAmount = 0;
      activeLoans.forEach((loan: Loan) => {
        if ((loan.dpd || 0) > 0) {
          overdueAmount += (loan.outstandingInterest || 0) + (loan.outstandingPenalty || 0);
        }
      });

      const collectionEfficiency = expectedCollection > 0
        ? Math.round((actualCollection / expectedCollection) * 100 * 10) / 10
        : 100;

      const summary: CollectionSummary = {
        expectedCollection,
        actualCollection,
        collectionEfficiency: Math.min(collectionEfficiency, 100), // Cap at 100%
        overdueAmount,
        paymentsCount: periodPayments.length,
      };

      reportLogger.info('Collection summary fetched', summary);
      return summary;
    } catch (error) {
      reportLogger.error('Failed to fetch collection summary', { error });
      throw error;
    }
  },

  /**
   * Get daily collection data for the last N days
   */
  async getDailyCollections(days: number = 7): Promise<DailyCollection[]> {
    reportLogger.info('Fetching daily collections', { days });

    try {
      // Fetch recent payments
      const response = await api.get<PagedResponse<Payment>>('/v1/payments', {
        params: { page: 0, size: 100 }
      });

      const payments = response.data.data.content || [];

      // Create a map of date -> amount
      const dailyMap = new Map<string, { amount: number; count: number }>();

      // Initialize the last N days with 0
      const today = new Date();
      for (let i = days - 1; i >= 0; i--) {
        const date = new Date(today);
        date.setDate(today.getDate() - i);
        const dateStr = date.toISOString().split('T')[0];
        dailyMap.set(dateStr, { amount: 0, count: 0 });
      }

      // Aggregate payments by date
      payments.forEach((payment: Payment) => {
        const paymentDate = payment.paymentDate?.split('T')[0];
        if (paymentDate && dailyMap.has(paymentDate)) {
          const current = dailyMap.get(paymentDate)!;
          current.amount += payment.amountPaid || 0;
          current.count += 1;
        }
      });

      // Convert map to array
      const dailyData: DailyCollection[] = [];
      dailyMap.forEach((value, date) => {
        dailyData.push({
          date,
          amount: value.amount,
          count: value.count,
        });
      });

      // Sort by date ascending
      dailyData.sort((a, b) => a.date.localeCompare(b.date));

      reportLogger.info('Daily collections fetched', { count: dailyData.length });
      return dailyData;
    } catch (error) {
      reportLogger.error('Failed to fetch daily collections', { error });
      throw error;
    }
  },

  /**
   * Get loans grouped by status
   */
  async getLoansByStatus(): Promise<LoansByStatus[]> {
    reportLogger.info('Fetching loans by status');

    try {
      const statuses = ['DRAFT', 'PENDING_APPROVAL', 'APPROVED', 'ACTIVE', 'CLOSED'];

      const responses = await Promise.all(
        statuses.map(status =>
          api.get<PagedResponse<Loan>>(`/v1/loans/status/${status}`, { params: { page: 0, size: 1 } })
            .catch(() => ({ data: { data: { totalElements: 0, content: [] } } }))
        )
      );

      const result: LoansByStatus[] = [];

      for (let i = 0; i < statuses.length; i++) {
        const content = responses[i].data.data.content || [];
        let totalAmount = 0;

        // For accurate amounts, we'd need to fetch more data
        // This is simplified to use count * average
        if (content.length > 0) {
          totalAmount = content.reduce((sum: number, loan: Loan) => sum + (loan.principalAmount || 0), 0);
        }

        result.push({
          status: statuses[i],
          count: responses[i].data.data.totalElements || 0,
          amount: totalAmount,
        });
      }

      reportLogger.info('Loans by status fetched', { result });
      return result;
    } catch (error) {
      reportLogger.error('Failed to fetch loans by status', { error });
      throw error;
    }
  },
};

export default reportService;
