/**
 * Payment API Service
 */

import api from './api';
import { logger } from '@/utils/logger';
import type {
  ApiResponse,
  PagedResponse,
  Payment,
  RecordPaymentRequest,
} from '@/types';

const paymentLogger = logger.scope('PaymentService');

export interface PaymentFilters {
  page?: number;
  size?: number;
  sortBy?: string;
  sortDir?: 'asc' | 'desc';
  loanId?: string;
  status?: string;
}

export const paymentService = {
  /**
   * Get all payments with pagination
   */
  async getAll(filters: PaymentFilters = {}): Promise<PagedResponse<Payment>['data']> {
    paymentLogger.info('Fetching payments', { filters });

    const params = {
      page: filters.page ?? 0,
      size: filters.size ?? 20,
      sortBy: filters.sortBy ?? 'paymentDate',
      sortDir: filters.sortDir ?? 'desc',
    };

    const response = await api.get<PagedResponse<Payment>>('/v1/payments', { params });

    paymentLogger.info('Payments fetched', {
      total: response.data.data.totalElements,
      page: response.data.data.page,
    });

    return response.data.data;
  },

  /**
   * Get payment by ID
   */
  async getById(id: string): Promise<Payment> {
    paymentLogger.info('Fetching payment by ID', { id });

    const response = await api.get<ApiResponse<Payment>>(`/v1/payments/${id}`);

    paymentLogger.info('Payment fetched', {
      id: response.data.data.id,
      paymentNumber: response.data.data.paymentNumber,
      amount: response.data.data.amountPaid,
    });

    return response.data.data;
  },

  /**
   * Get payments for a loan
   */
  async getByLoanId(
    loanId: string,
    page = 0,
    size = 20
  ): Promise<PagedResponse<Payment>['data']> {
    paymentLogger.info('Fetching payments for loan', { loanId, page, size });

    const response = await api.get<PagedResponse<Payment>>(
      `/v1/payments/loan/${loanId}`,
      { params: { page, size } }
    );

    paymentLogger.info('Loan payments fetched', {
      loanId,
      total: response.data.data.totalElements,
    });

    return response.data.data;
  },

  /**
   * Record a new payment
   */
  async recordPayment(data: RecordPaymentRequest): Promise<Payment> {
    paymentLogger.info('Recording payment', {
      loanId: data.loanId,
      amount: data.amount,
      method: data.paymentMethod,
      idempotencyKey: data.idempotencyKey,
    });

    const response = await api.post<ApiResponse<Payment>>('/v1/payments', data);

    paymentLogger.info('Payment recorded', {
      id: response.data.data.id,
      paymentNumber: response.data.data.paymentNumber,
      receiptNumber: response.data.data.receiptNumber,
      principalPaid: response.data.data.principalPaid,
      interestPaid: response.data.data.interestPaid,
      penaltyPaid: response.data.data.penaltyPaid,
      excessAmount: response.data.data.excessAmount,
    });

    return response.data.data;
  },

  /**
   * Reverse a payment
   */
  async reverse(id: string, reason: string): Promise<Payment> {
    paymentLogger.info('Reversing payment', { id, reason });

    const response = await api.post<ApiResponse<Payment>>(
      `/v1/payments/${id}/reverse`,
      null,
      { params: { reason } }
    );

    paymentLogger.info('Payment reversed', {
      id,
      status: response.data.data.status,
    });

    return response.data.data;
  },

  /**
   * Generate unique idempotency key
   */
  generateIdempotencyKey(): string {
    const key = `pay_${Date.now()}_${Math.random().toString(36).substring(2, 15)}`;
    paymentLogger.debug('Generated idempotency key', { key });
    return key;
  },
};

export default paymentService;
