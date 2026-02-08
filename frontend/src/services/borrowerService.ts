/**
 * Borrower API Service
 */

import api from './api';
import { logger } from '@/utils/logger';
import type {
  ApiResponse,
  PagedResponse,
  Borrower,
  CreateBorrowerRequest,
} from '@/types';

const borrowerLogger = logger.scope('BorrowerService');

export interface BorrowerFilters {
  page?: number;
  size?: number;
  sortBy?: string;
  sortDir?: 'asc' | 'desc';
  status?: string;
  search?: string;
}

export const borrowerService = {
  /**
   * Get all borrowers with pagination
   */
  async getAll(filters: BorrowerFilters = {}): Promise<PagedResponse<Borrower>['data']> {
    borrowerLogger.info('Fetching borrowers', { filters });

    const params = {
      page: filters.page ?? 0,
      size: filters.size ?? 20,
      sortBy: filters.sortBy ?? 'createdAt',
      sortDir: filters.sortDir ?? 'desc',
    };

    const response = await api.get<PagedResponse<Borrower>>('/v1/borrowers', { params });

    borrowerLogger.info('Borrowers fetched', {
      total: response.data.data.totalElements,
      page: response.data.data.page,
    });

    return response.data.data;
  },

  /**
   * Get borrower by ID
   */
  async getById(id: string): Promise<Borrower> {
    borrowerLogger.info('Fetching borrower by ID', { id });

    const response = await api.get<ApiResponse<Borrower>>(`/v1/borrowers/${id}`);

    borrowerLogger.info('Borrower fetched', {
      id: response.data.data.id,
      name: response.data.data.fullName,
    });

    return response.data.data;
  },

  /**
   * Get borrower by code
   */
  async getByCode(code: string): Promise<Borrower> {
    borrowerLogger.info('Fetching borrower by code', { code });

    const response = await api.get<ApiResponse<Borrower>>(`/v1/borrowers/code/${code}`);

    return response.data.data;
  },

  /**
   * Create new borrower
   */
  async create(data: CreateBorrowerRequest): Promise<Borrower> {
    borrowerLogger.info('Creating borrower', { name: data.fullName, phone: data.phone });

    const response = await api.post<ApiResponse<Borrower>>('/v1/borrowers', data);

    borrowerLogger.info('Borrower created', {
      id: response.data.data.id,
      code: response.data.data.borrowerCode,
    });

    return response.data.data;
  },

  /**
   * Update borrower
   */
  async update(id: string, data: Partial<CreateBorrowerRequest>): Promise<Borrower> {
    borrowerLogger.info('Updating borrower', { id, data });

    const response = await api.put<ApiResponse<Borrower>>(`/v1/borrowers/${id}`, data);

    borrowerLogger.info('Borrower updated', { id });

    return response.data.data;
  },

  /**
   * Block borrower
   */
  async block(id: string): Promise<Borrower> {
    borrowerLogger.info('Blocking borrower', { id });

    const response = await api.post<ApiResponse<Borrower>>(`/v1/borrowers/${id}/block`);

    borrowerLogger.info('Borrower blocked', { id });

    return response.data.data;
  },

  /**
   * Activate borrower
   */
  async activate(id: string): Promise<Borrower> {
    borrowerLogger.info('Activating borrower', { id });

    const response = await api.post<ApiResponse<Borrower>>(`/v1/borrowers/${id}/activate`);

    borrowerLogger.info('Borrower activated', { id });

    return response.data.data;
  },

  /**
   * Delete borrower (soft delete)
   */
  async delete(id: string): Promise<void> {
    borrowerLogger.info('Deleting borrower', { id });

    await api.delete(`/v1/borrowers/${id}`);

    borrowerLogger.info('Borrower deleted', { id });
  },

  /**
   * Search borrowers
   */
  async search(query: string, page = 0, size = 20): Promise<PagedResponse<Borrower>['data']> {
    borrowerLogger.info('Searching borrowers', { query, page, size });

    const response = await api.get<PagedResponse<Borrower>>('/v1/borrowers/search', {
      params: { q: query, page, size },
    });

    borrowerLogger.info('Search results', {
      query,
      total: response.data.data.totalElements,
    });

    return response.data.data;
  },

  /**
   * Get borrowers by status
   */
  async getByStatus(
    status: string,
    page = 0,
    size = 20
  ): Promise<PagedResponse<Borrower>['data']> {
    borrowerLogger.info('Fetching borrowers by status', { status, page, size });

    const response = await api.get<PagedResponse<Borrower>>(
      `/v1/borrowers/status/${status}`,
      { params: { page, size } }
    );

    return response.data.data;
  },
};

export default borrowerService;
