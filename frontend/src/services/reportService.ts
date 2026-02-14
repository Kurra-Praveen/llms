/**
 * Report Service
 * Aggregates data from various APIs for reporting
 */

import api from './api';
import { logger } from '@/utils/logger';
import type { ApiResponse } from '@/types';

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

export interface AgingBucket {
  bucket: string;
  label: string;
  minDpd: number;
  maxDpd: number | null;
  loansCount: number;
  outstandingPrincipal: number;
  outstandingInterest: number;
  outstandingPenalty: number;
  totalOutstanding: number;
  percentageOfPortfolio: number;
}

export interface AgingReport {
  buckets: AgingBucket[];
  totalLoans: number;
  totalOutstanding: number;
  generatedAt: string;
}

export const reportService = {
  /**
   * Get portfolio summary from server
   */
  async getPortfolioSummary(): Promise<PortfolioSummary> {
    reportLogger.info('Fetching portfolio summary');

    try {
      const response = await api.get<ApiResponse<PortfolioSummary>>('/v1/reports/portfolio-summary');
      const summary = response.data.data;

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
    reportLogger.info('Fetching collection summary', { period });

    try {
      const response = await api.get<ApiResponse<CollectionSummary>>('/v1/reports/collection-summary', {
        params: { period }
      });
      const summary = response.data.data;

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
      const response = await api.get<ApiResponse<DailyCollection[]>>('/v1/reports/daily-collections', {
        params: { days }
      });
      const data = response.data.data;

      reportLogger.info('Daily collections fetched', { count: data.length });
      return data;
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
      const response = await api.get<ApiResponse<LoansByStatus[]>>('/v1/reports/loans-by-status');
      const data = response.data.data;

      reportLogger.info('Loans by status fetched', { count: data.length });
      return data;
    } catch (error) {
      reportLogger.error('Failed to fetch loans by status', { error });
      throw error;
    }
  },

  /**
   * Get aging report - loans grouped by DPD buckets
   */
  async getAgingReport(): Promise<AgingReport> {
    reportLogger.info('Fetching aging report');

    try {
      const response = await api.get<ApiResponse<AgingReport>>('/v1/reports/aging');
      const report = response.data.data;

      reportLogger.info('Aging report generated', {
        totalLoans: report.totalLoans,
        totalOutstanding: report.totalOutstanding,
      });

      return report;
    } catch (error) {
      reportLogger.error('Failed to fetch aging report', { error });
      throw error;
    }
  },
};

export default reportService;
