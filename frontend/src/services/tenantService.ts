import api from './api';
import { logger } from '@/utils/logger';
import type {
  ApiResponse,
  PagedResponse,
  Tenant,
  CreateTenantRequest,
} from '@/types';

const tenantLogger = logger.scope('TenantService');

export const tenantService = {
  async getAll(page = 0, size = 20): Promise<PagedResponse<Tenant>['data']> {
    tenantLogger.info('Fetching tenants', { page, size });
    const response = await api.get<PagedResponse<Tenant>>('/v1/tenants', {
      params: { page, size },
    });
    return response.data.data;
  },

  async getById(id: string): Promise<Tenant> {
    const response = await api.get<ApiResponse<Tenant>>(`/v1/tenants/${id}`);
    return response.data.data;
  },

  async create(data: CreateTenantRequest): Promise<Tenant> {
    tenantLogger.info('Creating tenant', { name: data.businessName });
    const response = await api.post<ApiResponse<Tenant>>('/v1/tenants', data);
    return response.data.data;
  },

  async suspend(id: string): Promise<void> {
    await api.post(`/v1/tenants/${id}/suspend`);
  },

  async activate(id: string): Promise<void> {
    await api.post(`/v1/tenants/${id}/activate`);
  }
};
