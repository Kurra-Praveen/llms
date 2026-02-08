/**
 * Authentication API Service
 */

import api, { clearAuth } from './api';
import { logger } from '@/utils/logger';
import type { ApiResponse, AuthResponse, LoginRequest, User } from '@/types';

const authLogger = logger.scope('AuthService');

export const authService = {
  /**
   * Login with email and password
   */
  async login(credentials: LoginRequest): Promise<AuthResponse> {
    authLogger.info('Attempting login', { email: credentials.email });

    const response = await api.post<ApiResponse<AuthResponse>>(
      '/v1/auth/login',
      credentials
    );

    if (response.data.success) {
      const authData = response.data.data;

      // Store tokens
      localStorage.setItem('accessToken', authData.accessToken);
      localStorage.setItem('refreshToken', authData.refreshToken);
      localStorage.setItem('user', JSON.stringify(authData.user));

      authLogger.info('Login successful', {
        userId: authData.user.id,
        email: authData.user.email,
        role: authData.user.role,
      });

      return authData;
    }

    throw new Error(response.data.message || 'Login failed');
  },

  /**
   * Refresh access token
   */
  async refreshToken(): Promise<AuthResponse> {
    authLogger.info('Refreshing access token');

    const refreshToken = localStorage.getItem('refreshToken');
    if (!refreshToken) {
      throw new Error('No refresh token available');
    }

    const response = await api.post<ApiResponse<AuthResponse>>(
      '/v1/auth/refresh',
      { refreshToken }
    );

    if (response.data.success) {
      const authData = response.data.data;
      localStorage.setItem('accessToken', authData.accessToken);
      localStorage.setItem('refreshToken', authData.refreshToken);

      authLogger.info('Token refresh successful');
      return authData;
    }

    throw new Error(response.data.message || 'Token refresh failed');
  },

  /**
   * Logout current user
   */
  async logout(): Promise<void> {
    authLogger.info('Logging out');

    try {
      await api.post<ApiResponse<null>>('/v1/auth/logout');
      authLogger.info('Logout API call successful');
    } catch (error) {
      authLogger.warn('Logout API call failed, clearing local state anyway', { error });
    }

    clearAuth();
  },

  /**
   * Get current user profile
   */
  async getCurrentUser(): Promise<User> {
    authLogger.info('Fetching current user profile');

    const response = await api.get<ApiResponse<User>>('/v1/auth/me');

    if (response.data.success) {
      authLogger.info('Current user fetched', { userId: response.data.data.id });
      return response.data.data;
    }

    throw new Error(response.data.message || 'Failed to fetch user');
  },

  /**
   * Check if user is authenticated
   */
  isAuthenticated(): boolean {
    const token = localStorage.getItem('accessToken');
    const hasToken = !!token;
    authLogger.debug('Checking authentication status', { isAuthenticated: hasToken });
    return hasToken;
  },

  /**
   * Get stored user from localStorage
   */
  getStoredUser(): User | null {
    try {
      const userJson = localStorage.getItem('user');
      if (userJson) {
        return JSON.parse(userJson);
      }
    } catch (error) {
      authLogger.warn('Failed to parse stored user', { error });
    }
    return null;
  },
};

export default authService;
