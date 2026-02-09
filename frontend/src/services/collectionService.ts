/**
 * Collection API Service
 */

import api from './api';
import { logger } from '@/utils/logger';
import type {
  ApiResponse,
  PagedResponse,
  CollectionCase,
  CollectionActivity,
  CreateCollectionActivityRequest,
} from '@/types';

const collectionLogger = logger.scope('CollectionService');

export interface CollectionFilters {
  page?: number;
  size?: number;
  status?: string;
  dpdBucket?: string;
}

export const collectionService = {
  /**
   * Get all collection cases with pagination
   */
  async getAll(filters: CollectionFilters = {}): Promise<PagedResponse<CollectionCase>['data']> {
    collectionLogger.info('Fetching collection cases', { filters });

    const params: Record<string, any> = {
      page: filters.page ?? 0,
      size: filters.size ?? 20,
    };
    if (filters.status) params.status = filters.status;
    if (filters.dpdBucket) params.dpdBucket = filters.dpdBucket;

    const response = await api.get<PagedResponse<CollectionCase>>('/v1/collections', { params });

    collectionLogger.info('Collection cases fetched', {
      total: response.data.data.totalElements,
      page: response.data.data.page,
    });

    return response.data.data;
  },

  /**
   * Get collection case by ID
   */
  async getById(id: string): Promise<CollectionCase> {
    collectionLogger.info('Fetching collection case by ID', { id });

    const response = await api.get<ApiResponse<CollectionCase>>(`/v1/collections/${id}`);

    collectionLogger.info('Collection case fetched', {
      id: response.data.data.id,
      caseNumber: response.data.data.caseNumber,
      status: response.data.data.status,
    });

    return response.data.data;
  },

  /**
   * Get collection case by loan ID
   */
  async getByLoanId(loanId: string): Promise<CollectionCase> {
    collectionLogger.info('Fetching collection case for loan', { loanId });

    const response = await api.get<ApiResponse<CollectionCase>>(`/v1/collections/loan/${loanId}`);

    return response.data.data;
  },

  /**
   * Create collection case for a loan
   */
  async createForLoan(loanId: string): Promise<CollectionCase> {
    collectionLogger.info('Creating collection case for loan', { loanId });

    const response = await api.post<ApiResponse<CollectionCase>>(`/v1/collections/loan/${loanId}`);

    collectionLogger.info('Collection case created', {
      id: response.data.data.id,
      caseNumber: response.data.data.caseNumber,
    });

    return response.data.data;
  },

  /**
   * Add activity to collection case
   */
  async addActivity(caseId: string, data: CreateCollectionActivityRequest): Promise<CollectionActivity> {
    collectionLogger.info('Adding activity to case', { caseId, activityType: data.activityType });

    const response = await api.post<ApiResponse<CollectionActivity>>(
      `/v1/collections/${caseId}/activities`,
      data
    );

    collectionLogger.info('Activity added', { id: response.data.data.id });

    return response.data.data;
  },

  /**
   * Assign case to agent
   */
  async assign(caseId: string, userId: string): Promise<CollectionCase> {
    collectionLogger.info('Assigning case', { caseId, userId });

    const response = await api.put<ApiResponse<CollectionCase>>(
      `/v1/collections/${caseId}/assign`,
      null,
      { params: { userId } }
    );

    collectionLogger.info('Case assigned', { caseId, userId });

    return response.data.data;
  },

  /**
   * Escalate case
   */
  async escalate(caseId: string, reason?: string): Promise<CollectionCase> {
    collectionLogger.info('Escalating case', { caseId, reason });

    const response = await api.put<ApiResponse<CollectionCase>>(
      `/v1/collections/${caseId}/escalate`,
      null,
      { params: { reason } }
    );

    collectionLogger.info('Case escalated', { caseId });

    return response.data.data;
  },

  /**
   * Resolve case
   */
  async resolve(caseId: string, resolutionType: string, notes?: string): Promise<CollectionCase> {
    collectionLogger.info('Resolving case', { caseId, resolutionType });

    const response = await api.put<ApiResponse<CollectionCase>>(
      `/v1/collections/${caseId}/resolve`,
      { resolutionType, notes }
    );

    collectionLogger.info('Case resolved', { caseId, resolutionType });

    return response.data.data;
  },
};

export default collectionService;
