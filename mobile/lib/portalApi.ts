/**
 * Borrower portal API — mirrors the web app's portalService.
 * Every endpoint is automatically scoped to the logged-in borrower by the backend.
 */

import api from './api';
import type {
  ApiResponse,
  PagedData,
  Loan,
  RepaymentSchedule,
  Payment,
  PortalSummary,
  AppNotification,
} from './types';

export const portalApi = {
  async getSummary(): Promise<PortalSummary> {
    const res = await api.get<ApiResponse<PortalSummary>>('/v1/portal/summary');
    return res.data.data;
  },

  async getLoans(page = 0, size = 50): Promise<PagedData<Loan>> {
    const res = await api.get<ApiResponse<PagedData<Loan>>>('/v1/portal/loans', {
      params: { page, size },
    });
    return res.data.data;
  },

  async getLoan(loanId: string): Promise<Loan> {
    const res = await api.get<ApiResponse<Loan>>(`/v1/portal/loans/${loanId}`);
    return res.data.data;
  },

  async getSchedule(loanId: string): Promise<RepaymentSchedule[]> {
    const res = await api.get<ApiResponse<RepaymentSchedule[]>>(
      `/v1/portal/loans/${loanId}/schedule`
    );
    return res.data.data;
  },

  async getPayments(page = 0, size = 50): Promise<PagedData<Payment>> {
    const res = await api.get<ApiResponse<PagedData<Payment>>>('/v1/portal/payments', {
      params: { page, size },
    });
    return res.data.data;
  },
};

export const notificationApi = {
  async getAll(page = 0, size = 50): Promise<PagedData<AppNotification>> {
    const res = await api.get<ApiResponse<PagedData<AppNotification>>>('/v1/notifications', {
      params: { page, size },
    });
    return res.data.data;
  },

  async getUnreadCount(): Promise<number> {
    const res = await api.get<ApiResponse<{ unreadCount: number }>>(
      '/v1/notifications/unread-count'
    );
    return res.data.data.unreadCount;
  },

  async markAsRead(id: string): Promise<void> {
    await api.post(`/v1/notifications/${id}/read`);
  },
};
