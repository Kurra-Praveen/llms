/**
 * API client — Axios instance with Bearer token attachment and
 * automatic refresh-token retry on 401 (mirrors the web app's api.ts,
 * but backed by SecureStore instead of localStorage).
 */

import axios, { AxiosError, AxiosInstance, InternalAxiosRequestConfig } from 'axios';
import { config } from './config';
import { storage } from './storage';
import type { ApiResponse, AuthResponse } from './types';

export const api: AxiosInstance = axios.create({
  baseURL: config.apiUrl,
  timeout: 30000,
  headers: { 'Content-Type': 'application/json' },
});

// ---- Request: attach access token --------------------------------------
api.interceptors.request.use(async (reqConfig: InternalAxiosRequestConfig) => {
  const token = await storage.getAccessToken();
  if (token) {
    reqConfig.headers.Authorization = `Bearer ${token}`;
  }
  return reqConfig;
});

// ---- Response: refresh-and-retry on 401 --------------------------------
let refreshPromise: Promise<string | null> | null = null;

async function refreshAccessToken(): Promise<string | null> {
  // De-dupe concurrent refresh calls
  if (!refreshPromise) {
    refreshPromise = (async () => {
      const refreshToken = await storage.getRefreshToken();
      if (!refreshToken) return null;
      try {
        const response = await axios.post<ApiResponse<AuthResponse>>(
          `${config.apiUrl}/v1/auth/refresh`,
          { refreshToken }
        );
        if (response.data.success) {
          const auth = response.data.data;
          await storage.saveAuth(auth.accessToken, auth.refreshToken, auth.user);
          return auth.accessToken;
        }
      } catch {
        // fall through
      }
      return null;
    })().finally(() => {
      refreshPromise = null;
    });
  }
  return refreshPromise;
}

// Callback fired when the session is unrecoverable (set by the auth store)
let onSessionExpired: (() => void) | null = null;
export function setOnSessionExpired(callback: () => void) {
  onSessionExpired = callback;
}

api.interceptors.response.use(
  (response) => response,
  async (error: AxiosError) => {
    const originalRequest = error.config as InternalAxiosRequestConfig & { _retried?: boolean };

    if (error.response?.status === 401 && originalRequest && !originalRequest._retried) {
      originalRequest._retried = true;
      const newToken = await refreshAccessToken();
      if (newToken) {
        originalRequest.headers.Authorization = `Bearer ${newToken}`;
        return api(originalRequest);
      }
      // Refresh failed — session is dead
      await storage.clear();
      onSessionExpired?.();
    }
    return Promise.reject(error);
  }
);

export default api;
