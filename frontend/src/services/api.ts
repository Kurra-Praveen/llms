/**
 * API Client with Axios
 * Includes request/response interceptors with comprehensive logging
 */

import axios, { type AxiosError, type AxiosInstance, type InternalAxiosRequestConfig, type AxiosResponse } from 'axios';
import { logger } from '@/utils/logger';
import type { ApiResponse } from '@/types';

const apiLogger = logger.scope('API');

// Create axios instance
const api: AxiosInstance = axios.create({
  baseURL: '/api',
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Generate unique request ID for tracing
function generateRequestId(): string {
  return `req_${Date.now()}_${Math.random().toString(36).substring(2, 9)}`;
}

// Request interceptor
api.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    const requestId = generateRequestId();
    config.headers['X-Request-ID'] = requestId;

    // Add auth token if available
    const token = localStorage.getItem('accessToken');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }

    // Log request details
    apiLogger.info(`➡️ ${config.method?.toUpperCase()} ${config.url}`, {
      requestId,
      method: config.method,
      url: config.url,
      params: config.params,
      data: config.data,
      headers: {
        Authorization: config.headers.Authorization ? '[REDACTED]' : undefined,
        'Content-Type': config.headers['Content-Type'],
      },
    });

    // Store start time for duration calculation
    (config as any)._startTime = Date.now();
    (config as any)._requestId = requestId;

    return config;
  },
  (error: AxiosError) => {
    apiLogger.error('Request configuration error', {
      message: error.message,
      config: error.config,
    });
    return Promise.reject(error);
  }
);

// Response interceptor
api.interceptors.response.use(
  (response: AxiosResponse) => {
    const config = response.config as any;
    const duration = Date.now() - (config._startTime || Date.now());
    const requestId = config._requestId;

    apiLogger.info(`⬅️ ${response.status} ${config.method?.toUpperCase()} ${config.url}`, {
      requestId,
      status: response.status,
      statusText: response.statusText,
      duration: `${duration}ms`,
      data: response.data,
    });

    return response;
  },
  (error: AxiosError<ApiResponse<unknown>>) => {
    const config = error.config as any;
    const duration = Date.now() - (config?._startTime || Date.now());
    const requestId = config?._requestId;

    const errorInfo = {
      requestId,
      status: error.response?.status,
      statusText: error.response?.statusText,
      duration: `${duration}ms`,
      message: error.message,
      errorCode: error.response?.data?.errorCode,
      errors: error.response?.data?.errors,
      data: error.response?.data,
    };

    // Log based on error type
    if (error.response) {
      // Server responded with error status
      if (error.response.status === 401) {
        apiLogger.warn(`🔒 Unauthorized - ${config?.url}`, errorInfo);
        // Handle token expiration
        handleUnauthorized();
      } else if (error.response.status === 403) {
        apiLogger.warn(`🚫 Forbidden - ${config?.url}`, errorInfo);
      } else if (error.response.status >= 500) {
        apiLogger.error(`💥 Server Error - ${config?.url}`, errorInfo);
      } else {
        apiLogger.warn(`⚠️ ${error.response.status} ${config?.url}`, errorInfo);
      }
    } else if (error.request) {
      // Request made but no response received
      apiLogger.error('📡 Network Error - No response received', {
        requestId,
        url: config?.url,
        message: error.message,
      });
    } else {
      // Error setting up the request
      apiLogger.error('❓ Request Setup Error', {
        message: error.message,
      });
    }

    return Promise.reject(error);
  }
);

/**
 * Handle 401 Unauthorized errors
 * Try to refresh token or redirect to login
 */
async function handleUnauthorized(): Promise<void> {
  apiLogger.info('Handling unauthorized response...');

  const refreshToken = localStorage.getItem('refreshToken');

  if (refreshToken) {
    try {
      apiLogger.info('Attempting token refresh...');
      const response = await axios.post('/api/v1/auth/refresh', {
        refreshToken,
      });

      if (response.data.success) {
        const { accessToken, refreshToken: newRefreshToken } = response.data.data;
        localStorage.setItem('accessToken', accessToken);
        localStorage.setItem('refreshToken', newRefreshToken);
        apiLogger.info('Token refreshed successfully');
        // Note: The failed request should be retried by the calling code
        return;
      }
    } catch (refreshError) {
      apiLogger.error('Token refresh failed', { error: refreshError });
    }
  }

  // Clear tokens and redirect to login
  apiLogger.info('Clearing auth state and redirecting to login');
  localStorage.removeItem('accessToken');
  localStorage.removeItem('refreshToken');
  localStorage.removeItem('user');

  // Only redirect if not already on login page
  if (!window.location.pathname.includes('/login')) {
    window.location.href = '/login';
  }
}

/**
 * Set auth token manually
 */
export function setAuthToken(token: string | null): void {
  if (token) {
    localStorage.setItem('accessToken', token);
    apiLogger.debug('Auth token set');
  } else {
    localStorage.removeItem('accessToken');
    apiLogger.debug('Auth token cleared');
  }
}

/**
 * Clear all auth data
 */
export function clearAuth(): void {
  localStorage.removeItem('accessToken');
  localStorage.removeItem('refreshToken');
  localStorage.removeItem('user');
  apiLogger.info('Auth data cleared');
}

export default api;
