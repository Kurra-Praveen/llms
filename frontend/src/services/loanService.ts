/**
 * Loan API Service
 */

import api from './api';
import { logger } from '@/utils/logger';
import type {
  ApiResponse,
  PagedResponse,
  Loan,
  CreateLoanRequest,
  DisburseLoanRequest,
  RepaymentSchedule,
} from '@/types';

const loanLogger = logger.scope('LoanService');

export interface LoanFilters {
  page?: number;
  size?: number;
  sortBy?: string;
  sortDir?: 'asc' | 'desc';
  status?: string;
  borrowerId?: string;
}

export const loanService = {
  /**
   * Get all loans with pagination
   */
  async getAll(filters: LoanFilters = {}): Promise<PagedResponse<Loan>['data']> {
    loanLogger.info('Fetching loans', { filters });

    const params = {
      page: filters.page ?? 0,
      size: filters.size ?? 20,
      sortBy: filters.sortBy ?? 'createdAt',
      sortDir: filters.sortDir ?? 'desc',
    };

    const response = await api.get<PagedResponse<Loan>>('/v1/loans', { params });

    loanLogger.info('Loans fetched', {
      total: response.data.data.totalElements,
      page: response.data.data.page,
    });

    return response.data.data;
  },

  /**
   * Get loan by ID
   */
  async getById(id: string): Promise<Loan> {
    loanLogger.info('Fetching loan by ID', { id });

    const response = await api.get<ApiResponse<Loan>>(`/v1/loans/${id}`);

    loanLogger.info('Loan fetched', {
      id: response.data.data.id,
      loanNumber: response.data.data.loanNumber,
      status: response.data.data.status,
    });

    return response.data.data;
  },

  /**
   * Get loan by loan number
   */
  async getByLoanNumber(loanNumber: string): Promise<Loan> {
    loanLogger.info('Fetching loan by number', { loanNumber });

    const response = await api.get<ApiResponse<Loan>>(`/v1/loans/number/${loanNumber}`);

    return response.data.data;
  },

  /**
   * Get loans for a borrower
   */
  async getByBorrowerId(
    borrowerId: string,
    page = 0,
    size = 20
  ): Promise<PagedResponse<Loan>['data']> {
    loanLogger.info('Fetching loans for borrower', { borrowerId, page, size });

    const response = await api.get<PagedResponse<Loan>>(
      `/v1/loans/borrower/${borrowerId}`,
      { params: { page, size } }
    );

    loanLogger.info('Borrower loans fetched', {
      borrowerId,
      total: response.data.data.totalElements,
    });

    return response.data.data;
  },

  /**
   * Get loans by status
   */
  async getByStatus(
    status: string,
    page = 0,
    size = 20
  ): Promise<PagedResponse<Loan>['data']> {
    loanLogger.info('Fetching loans by status', { status, page, size });

    const response = await api.get<PagedResponse<Loan>>(
      `/v1/loans/status/${status}`,
      { params: { page, size } }
    );

    return response.data.data;
  },

  /**
   * Create new loan
   */
  async create(data: CreateLoanRequest): Promise<Loan> {
    loanLogger.info('Creating loan', {
      borrowerId: data.borrowerId,
      principal: data.principalAmount,
      interestType: data.interestType,
      tenure: data.tenureMonths,
    });

    const response = await api.post<ApiResponse<Loan>>('/v1/loans', data);

    loanLogger.info('Loan created', {
      id: response.data.data.id,
      loanNumber: response.data.data.loanNumber,
      emi: response.data.data.emiAmount,
    });

    return response.data.data;
  },

  /**
   * Approve loan
   */
  async approve(id: string, notes?: string): Promise<Loan> {
    loanLogger.info('Approving loan', { id, notes });

    const response = await api.post<ApiResponse<Loan>>(
      `/v1/loans/${id}/approve`,
      null,
      { params: { notes } }
    );

    loanLogger.info('Loan approved', { id, status: response.data.data.status });

    return response.data.data;
  },

  /**
   * Reject loan
   */
  async reject(id: string, notes?: string): Promise<Loan> {
    loanLogger.info('Rejecting loan', { id, notes });

    const response = await api.post<ApiResponse<Loan>>(
      `/v1/loans/${id}/reject`,
      null,
      { params: { notes } }
    );

    loanLogger.info('Loan rejected', { id });

    return response.data.data;
  },

  /**
   * Disburse loan
   */
  async disburse(id: string, data: DisburseLoanRequest): Promise<Loan> {
    loanLogger.info('Disbursing loan', {
      id,
      disbursementDate: data.disbursementDate,
      firstPaymentDate: data.firstPaymentDate,
    });

    const response = await api.post<ApiResponse<Loan>>(`/v1/loans/${id}/disburse`, data);

    loanLogger.info('Loan disbursed', {
      id,
      status: response.data.data.status,
      maturityDate: response.data.data.maturityDate,
    });

    return response.data.data;
  },

  /**
   * Get repayment schedule for a loan
   */
  async getSchedule(loanId: string): Promise<RepaymentSchedule[]> {
    loanLogger.info('Fetching repayment schedule', { loanId });

    const response = await api.get<ApiResponse<RepaymentSchedule[]>>(
      `/v1/loans/${loanId}/schedule`
    );

    loanLogger.info('Schedule fetched', {
      loanId,
      installments: response.data.data.length,
    });

    return response.data.data;
  },

  /**
   * Close loan
   */
  async close(id: string): Promise<Loan> {
    loanLogger.info('Closing loan', { id });

    const response = await api.post<ApiResponse<Loan>>(`/v1/loans/${id}/close`);

    loanLogger.info('Loan closed', { id });

    return response.data.data;
  },
};

export default loanService;
