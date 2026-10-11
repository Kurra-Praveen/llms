/**
 * Auth store — lightweight global auth state (no external state lib needed).
 * Wraps SecureStore + the auth endpoints.
 */

import { useCallback, useEffect, useState } from 'react';
import api, { setOnSessionExpired } from './api';
import { storage } from './storage';
import type { ApiResponse, AuthResponse, LoginRequest, User } from './types';

interface AuthState {
  user: User | null;
  isLoading: boolean;
  isAuthenticated: boolean;
}

// Module-level state + listeners (simple observable store)
let state: AuthState = { user: null, isLoading: true, isAuthenticated: false };
const listeners = new Set<() => void>();

function setState(next: Partial<AuthState>) {
  state = { ...state, ...next };
  listeners.forEach((l) => l());
}

export const authStore = {
  /** Restore session from SecureStore on app start */
  async initialize(): Promise<void> {
    const user = await storage.getUser();
    const token = await storage.getAccessToken();
    setState({
      user: token ? user : null,
      isAuthenticated: !!(token && user),
      isLoading: false,
    });
  },

  async login(credentials: LoginRequest): Promise<AuthResponse> {
    const response = await api.post<ApiResponse<AuthResponse>>('/v1/auth/login', credentials);
    if (!response.data.success) {
      throw new Error(response.data.message || 'Login failed');
    }
    const auth = response.data.data;
    await storage.saveAuth(auth.accessToken, auth.refreshToken, auth.user);
    setState({ user: auth.user, isAuthenticated: true });
    return auth;
  },

  async logout(): Promise<void> {
    try {
      await api.post('/v1/auth/logout');
    } catch {
      // Clear local state even if the API call fails
    }
    await storage.clear();
    setState({ user: null, isAuthenticated: false });
  },

  async handleSessionExpired(): Promise<void> {
    await storage.clear();
    setState({ user: null, isAuthenticated: false });
  },
};

// Wire the API client's session-expired hook to the store
setOnSessionExpired(() => {
  authStore.handleSessionExpired();
});

/** React hook to subscribe to auth state */
export function useAuth() {
  const [, forceRender] = useState(0);

  useEffect(() => {
    const listener = () => forceRender((n) => n + 1);
    listeners.add(listener);
    return () => {
      listeners.delete(listener);
    };
  }, []);

  const login = useCallback((c: LoginRequest) => authStore.login(c), []);
  const logout = useCallback(() => authStore.logout(), []);

  return { ...state, login, logout };
}
