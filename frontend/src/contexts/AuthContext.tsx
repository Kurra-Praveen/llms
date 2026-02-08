/**
 * Authentication Context
 * Provides auth state and methods throughout the app
 */

import React, { createContext, useContext, useEffect, useState, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { authService } from '@/services';
import { logger } from '@/utils/logger';
import type { User, LoginRequest, Role } from '@/types';

const authLogger = logger.scope('AuthContext');

interface AuthContextType {
  user: User | null;
  isAuthenticated: boolean;
  isLoading: boolean;
  error: string | null;
  login: (credentials: LoginRequest) => Promise<void>;
  logout: () => Promise<void>;
  refreshUser: () => Promise<void>;
  hasRole: (roles: Role | Role[]) => boolean;
  clearError: () => void;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

interface AuthProviderProps {
  children: React.ReactNode;
}

export function AuthProvider({ children }: AuthProviderProps) {
  const [user, setUser] = useState<User | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  // Initialize auth state from localStorage
  useEffect(() => {
    authLogger.info('Initializing auth state');

    const initAuth = async () => {
      try {
        const storedUser = authService.getStoredUser();
        const isAuth = authService.isAuthenticated();

        authLogger.debug('Initial auth check', {
          hasStoredUser: !!storedUser,
          isAuthenticated: isAuth,
        });

        if (storedUser && isAuth) {
          setUser(storedUser);
          authLogger.info('User restored from storage', {
            userId: storedUser.id,
            email: storedUser.email,
            role: storedUser.role,
          });

          // Optionally verify token is still valid by fetching current user
          try {
            const freshUser = await authService.getCurrentUser();
            setUser(freshUser);
            localStorage.setItem('user', JSON.stringify(freshUser));
            authLogger.info('User profile refreshed from server');
          } catch (refreshError) {
            authLogger.warn('Failed to refresh user profile, using cached data', {
              error: refreshError,
            });
          }
        } else {
          authLogger.info('No stored auth state found');
        }
      } catch (err) {
        authLogger.error('Error initializing auth state', { error: err });
      } finally {
        setIsLoading(false);
      }
    };

    initAuth();
  }, []);

  /**
   * Login with credentials
   */
  const login = useCallback(async (credentials: LoginRequest) => {
    authLogger.info('Login attempt', { email: credentials.email });
    setIsLoading(true);
    setError(null);

    try {
      const authData = await authService.login(credentials);
      setUser(authData.user);
      authLogger.info('Login successful in context', {
        userId: authData.user.id,
        role: authData.user.role,
      });
    } catch (err: any) {
      const errorMessage = err.response?.data?.message || err.message || 'Login failed';
      authLogger.error('Login failed', { error: errorMessage });
      setError(errorMessage);
      throw err;
    } finally {
      setIsLoading(false);
    }
  }, []);

  /**
   * Logout current user
   */
  const logout = useCallback(async () => {
    authLogger.info('Logout initiated');
    setIsLoading(true);

    try {
      await authService.logout();
      setUser(null);
      authLogger.info('Logout successful');
    } catch (err) {
      authLogger.error('Logout error', { error: err });
      // Still clear user state even if API call fails
      setUser(null);
    } finally {
      setIsLoading(false);
    }
  }, []);

  /**
   * Refresh user profile from server
   */
  const refreshUser = useCallback(async () => {
    authLogger.info('Refreshing user profile');

    if (!authService.isAuthenticated()) {
      authLogger.warn('Cannot refresh - not authenticated');
      return;
    }

    try {
      const freshUser = await authService.getCurrentUser();
      setUser(freshUser);
      localStorage.setItem('user', JSON.stringify(freshUser));
      authLogger.info('User profile refreshed');
    } catch (err) {
      authLogger.error('Failed to refresh user profile', { error: err });
      throw err;
    }
  }, []);

  /**
   * Check if user has specified role(s)
   */
  const hasRole = useCallback(
    (roles: Role | Role[]): boolean => {
      if (!user) {
        authLogger.debug('hasRole check - no user');
        return false;
      }

      const roleArray = Array.isArray(roles) ? roles : [roles];
      const hasRequiredRole = roleArray.includes(user.role);

      authLogger.debug('hasRole check', {
        userRole: user.role,
        requiredRoles: roleArray,
        hasRole: hasRequiredRole,
      });

      return hasRequiredRole;
    },
    [user]
  );

  /**
   * Clear error state
   */
  const clearError = useCallback(() => {
    setError(null);
  }, []);

  const value: AuthContextType = {
    user,
    isAuthenticated: !!user && authService.isAuthenticated(),
    isLoading,
    error,
    login,
    logout,
    refreshUser,
    hasRole,
    clearError,
  };

  authLogger.debug('AuthContext render', {
    isAuthenticated: value.isAuthenticated,
    isLoading: value.isLoading,
    hasUser: !!value.user,
  });

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

/**
 * Hook to use auth context
 */
export function useAuth(): AuthContextType {
  const context = useContext(AuthContext);

  if (context === undefined) {
    throw new Error('useAuth must be used within an AuthProvider');
  }

  return context;
}

/**
 * Hook for protected navigation
 */
export function useAuthNavigation() {
  const { isAuthenticated } = useAuth();
  const navigate = useNavigate();

  useEffect(() => {
    if (!isAuthenticated) {
      authLogger.info('User not authenticated, redirecting to login');
      navigate('/login', { replace: true });
    }
  }, [isAuthenticated, navigate]);
}

export default AuthContext;
