/**
 * Dashboard Service
 * Aggregates data from various APIs for dashboard display
 */

import api from './api';
import { logger } from '@/utils/logger';
import type { PagedResponse, Loan, Borrower, Payment } from '@/types';

const dashboardLogger = logger.scope('DashboardService');

export interface DashboardStats {
  totalBorrowers: number;
  activeBorrowers: number;
  activeLoans: number;
  pendingApprovalLoans: number;
  totalDisbursed: number;
  totalOutstanding: number;
  overdueLoans: number;
  monthlyCollection: number;
  recentLoans: Loan[];
  recentPayments: Payment[];
}

export interface RecentActivity {
  id: string;
  type: 'loan_created' | 'loan_approved' | 'loan_disbursed' | 'payment_received' | 'borrower_added';
  description: string;
  time: string;
  timestamp: Date;
}

export const dashboardService = {
  /**
   * Get dashboard statistics
   */
  async getStats(): Promise<DashboardStats> {
    dashboardLogger.info('Fetching dashboard stats');

    try {
      // Fetch data in parallel for better performance
      const [
        borrowersResponse,
        activeLoansResponse,
        pendingLoansResponse,
        draftLoansResponse,
        recentPaymentsResponse,
      ] = await Promise.all([
        api.get<PagedResponse<Borrower>>('/v1/borrowers', { params: { page: 0, size: 1 } }),
        api.get<PagedResponse<Loan>>('/v1/loans/status/ACTIVE', { params: { page: 0, size: 5 } }),
        api.get<PagedResponse<Loan>>('/v1/loans/status/PENDING_APPROVAL', { params: { page: 0, size: 1 } }),
        api.get<PagedResponse<Loan>>('/v1/loans/status/DRAFT', { params: { page: 0, size: 1 } }),
        api.get<PagedResponse<Payment>>('/v1/payments', { params: { page: 0, size: 5 } }),
      ]);

      // Calculate totals from active loans
      const activeLoans = activeLoansResponse.data.data.content || [];
      let totalOutstanding = 0;
      let totalDisbursed = 0;

      activeLoans.forEach((loan: Loan) => {
        totalDisbursed += loan.principalAmount || 0;
        totalOutstanding += loan.outstandingPrincipal || 0;
      });

      // Count overdue loans (loans with overdue schedules)
      // For now, we'll estimate based on active loans - in a real app, you'd have an API for this
      const overdueLoans = 0; // Would need a dedicated API endpoint

      const stats: DashboardStats = {
        totalBorrowers: borrowersResponse.data.data.totalElements || 0,
        activeBorrowers: borrowersResponse.data.data.totalElements || 0, // Would need status filter
        activeLoans: activeLoansResponse.data.data.totalElements || 0,
        pendingApprovalLoans: (pendingLoansResponse.data.data.totalElements || 0) + (draftLoansResponse.data.data.totalElements || 0),
        totalDisbursed,
        totalOutstanding,
        overdueLoans,
        monthlyCollection: 0, // Would need a dedicated API
        recentLoans: activeLoans,
        recentPayments: recentPaymentsResponse.data.data.content || [],
      };

      dashboardLogger.info('Dashboard stats fetched', stats);
      return stats;
    } catch (error) {
      dashboardLogger.error('Failed to fetch dashboard stats', { error });
      throw error;
    }
  },

  /**
   * Get recent activity feed
   */
  async getRecentActivity(): Promise<RecentActivity[]> {
    dashboardLogger.info('Fetching recent activity');

    try {
      // Fetch recent loans and payments
      const [loansResponse, paymentsResponse] = await Promise.all([
        api.get<PagedResponse<Loan>>('/v1/loans', { params: { page: 0, size: 10, sortBy: 'createdAt', sortDir: 'desc' } }),
        api.get<PagedResponse<Payment>>('/v1/payments', { params: { page: 0, size: 10 } }),
      ]);

      const activities: RecentActivity[] = [];

      // Add loan activities
      const loans = loansResponse.data.data.content || [];
      loans.forEach((loan: Loan) => {
        let type: RecentActivity['type'] = 'loan_created';
        let description = '';

        if (loan.status === 'ACTIVE' || loan.status === 'DISBURSED') {
          type = 'loan_disbursed';
          description = `Loan #${loan.loanNumber} disbursed to ${loan.borrowerName}`;
        } else if (loan.status === 'APPROVED') {
          type = 'loan_approved';
          description = `Loan #${loan.loanNumber} approved for ${loan.borrowerName}`;
        } else {
          type = 'loan_created';
          description = `New loan application #${loan.loanNumber} created`;
        }

        activities.push({
          id: `loan-${loan.id}`,
          type,
          description,
          time: formatTimeAgo(new Date(loan.createdAt)),
          timestamp: new Date(loan.createdAt),
        });
      });

      // Add payment activities
      const payments = paymentsResponse.data.data.content || [];
      payments.forEach((payment: Payment) => {
        activities.push({
          id: `payment-${payment.id}`,
          type: 'payment_received',
          description: `Payment of ₹${payment.amountPaid?.toLocaleString()} received for Loan #${payment.loanNumber || 'N/A'}`,
          time: formatTimeAgo(new Date(payment.paymentDate)),
          timestamp: new Date(payment.paymentDate),
        });
      });

      // Sort by timestamp descending and take top 10
      activities.sort((a, b) => b.timestamp.getTime() - a.timestamp.getTime());
      const recentActivities = activities.slice(0, 10);

      dashboardLogger.info('Recent activity fetched', { count: recentActivities.length });
      return recentActivities;
    } catch (error) {
      dashboardLogger.error('Failed to fetch recent activity', { error });
      throw error;
    }
  },
};

/**
 * Format a date as "X time ago"
 */
function formatTimeAgo(date: Date): string {
  const now = new Date();
  const diffMs = now.getTime() - date.getTime();
  const diffSecs = Math.floor(diffMs / 1000);
  const diffMins = Math.floor(diffSecs / 60);
  const diffHours = Math.floor(diffMins / 60);
  const diffDays = Math.floor(diffHours / 24);

  if (diffSecs < 60) {
    return 'Just now';
  } else if (diffMins < 60) {
    return `${diffMins} minute${diffMins === 1 ? '' : 's'} ago`;
  } else if (diffHours < 24) {
    return `${diffHours} hour${diffHours === 1 ? '' : 's'} ago`;
  } else if (diffDays < 7) {
    return `${diffDays} day${diffDays === 1 ? '' : 's'} ago`;
  } else {
    return date.toLocaleDateString();
  }
}

export default dashboardService;
