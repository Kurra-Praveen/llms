/**
 * Borrower Portal API Service (self-service, /v1/portal/**)
 * Used by BORROWER-role users — every endpoint is automatically scoped
 * to the authenticated borrower's own data by the backend.
 */

import api from './api';
import { logger } from '@/utils/logger';
import type {
  ApiResponse,
  PagedResponse,
  Borrower,
  Loan,
  RepaymentSchedule,
  Payment,
  PortalSummary,
} from '@/types';

const portalLogger = logger.scope('PortalService');

export const portalService = {
  /** Get the logged-in borrower's profile */
  async getMyProfile(): Promise<Borrower> {
    portalLogger.info('Fetching portal profile');
    const response = await api.get<ApiResponse<Borrower>>('/v1/portal/me');
    return response.data.data;
  },

  /** Get dashboard summary: active loans, total outstanding, next EMI */
  async getMySummary(): Promise<PortalSummary> {
    portalLogger.info('Fetching portal summary');
    const response = await api.get<ApiResponse<PortalSummary>>('/v1/portal/summary');
    return response.data.data;
  },

  /** Get the borrower's loans (paginated) */
  async getMyLoans(page = 0, size = 20): Promise<PagedResponse<Loan>['data']> {
    portalLogger.info('Fetching portal loans', { page, size });
    const response = await api.get<PagedResponse<Loan>>('/v1/portal/loans', {
      params: { page, size },
    });
    return response.data.data;
  },

  /** Get one of the borrower's loans by ID */
  async getMyLoan(loanId: string): Promise<Loan> {
    portalLogger.info('Fetching portal loan', { loanId });
    const response = await api.get<ApiResponse<Loan>>(`/v1/portal/loans/${loanId}`);
    return response.data.data;
  },

  /** Get the repayment schedule for one of the borrower's loans */
  async getMyLoanSchedule(loanId: string): Promise<RepaymentSchedule[]> {
    portalLogger.info('Fetching portal loan schedule', { loanId });
    const response = await api.get<ApiResponse<RepaymentSchedule[]>>(
      `/v1/portal/loans/${loanId}/schedule`
    );
    return response.data.data;
  },

  /** Get the borrower's payment history (paginated) */
  async getMyPayments(page = 0, size = 20): Promise<PagedResponse<Payment>['data']> {
    portalLogger.info('Fetching portal payments', { page, size });
    const response = await api.get<PagedResponse<Payment>>('/v1/portal/payments', {
      params: { page, size },
    });
    return response.data.data;
  },

  /** Get one of the borrower's payments by ID */
  async getMyPayment(paymentId: string): Promise<Payment> {
    portalLogger.info('Fetching portal payment', { paymentId });
    const response = await api.get<ApiResponse<Payment>>(`/v1/portal/payments/${paymentId}`);
    return response.data.data;
  },
};

export default portalService;
