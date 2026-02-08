/**
 * Login Page
 */

import React, { useState, useEffect } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { useAuth } from '@/contexts/AuthContext';
import { Button, Input, Card } from '@/components/ui';
import { logger } from '@/utils/logger';

const loginLogger = logger.scope('LoginPage');

// Validation schema
const loginSchema = z.object({
  email: z.string().email('Please enter a valid email address'),
  password: z.string().min(1, 'Password is required'),
});

type LoginFormData = z.infer<typeof loginSchema>;

export function LoginPage() {
  const { login, isAuthenticated, isLoading: authLoading, error: authError, clearError } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [isSubmitting, setIsSubmitting] = useState(false);

  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<LoginFormData>({
    resolver: zodResolver(loginSchema),
  });

  // Redirect if already authenticated
  useEffect(() => {
    if (isAuthenticated) {
      const from = (location.state as any)?.from?.pathname || '/dashboard';
      loginLogger.info('Already authenticated, redirecting', { to: from });
      navigate(from, { replace: true });
    }
  }, [isAuthenticated, navigate, location]);

  // Clear error on unmount
  useEffect(() => {
    return () => {
      clearError();
    };
  }, [clearError]);

  const onSubmit = async (data: LoginFormData) => {
    loginLogger.info('Login form submitted', { email: data.email });
    setIsSubmitting(true);

    try {
      await login(data);
      const from = (location.state as any)?.from?.pathname || '/dashboard';
      loginLogger.info('Login successful, navigating', { to: from });
      navigate(from, { replace: true });
    } catch (error: any) {
      loginLogger.error('Login failed', { error: error.message });
      // Error is handled by AuthContext
    } finally {
      setIsSubmitting(false);
    }
  };

  loginLogger.debug('LoginPage render', { authLoading, isSubmitting, hasErrors: !!authError });

  return (
    <div className="min-h-screen flex items-center justify-center bg-gray-100 py-12 px-4 sm:px-6 lg:px-8">
      <div className="max-w-md w-full space-y-8">
        {/* Logo and title */}
        <div className="text-center">
          <h1 className="text-3xl font-bold text-primary-600">LoanPlatform</h1>
          <h2 className="mt-4 text-2xl font-semibold text-gray-900">
            Sign in to your account
          </h2>
          <p className="mt-2 text-sm text-gray-600">
            Multi-Tenant Loan Management System
          </p>
        </div>

        {/* Login form */}
        <Card className="mt-8">
          <form onSubmit={handleSubmit(onSubmit)} className="space-y-6">
            {/* Error message */}
            {authError && (
              <div className="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded-lg text-sm">
                {authError}
              </div>
            )}

            {/* Email field */}
            <Input
              label="Email address"
              type="email"
              autoComplete="email"
              placeholder="you@example.com"
              error={errors.email?.message}
              {...register('email')}
            />

            {/* Password field */}
            <Input
              label="Password"
              type="password"
              autoComplete="current-password"
              placeholder="Enter your password"
              error={errors.password?.message}
              {...register('password')}
            />

            {/* Remember me & Forgot password */}
            <div className="flex items-center justify-between">
              <div className="flex items-center">
                <input
                  id="remember-me"
                  name="remember-me"
                  type="checkbox"
                  className="h-4 w-4 text-primary-600 focus:ring-primary-500 border-gray-300 rounded"
                />
                <label htmlFor="remember-me" className="ml-2 block text-sm text-gray-700">
                  Remember me
                </label>
              </div>

              <a
                href="#"
                className="text-sm text-primary-600 hover:text-primary-500"
                onClick={(e) => {
                  e.preventDefault();
                  loginLogger.debug('Forgot password clicked');
                }}
              >
                Forgot password?
              </a>
            </div>

            {/* Submit button */}
            <Button
              type="submit"
              fullWidth
              isLoading={isSubmitting || authLoading}
              disabled={isSubmitting || authLoading}
            >
              Sign in
            </Button>
          </form>
        </Card>

        {/* Demo credentials hint (remove in production) */}
        <div className="text-center text-sm text-gray-500 mt-4">
          <p>Demo credentials:</p>
          <p className="font-mono text-xs mt-1">
            admin@example.com / password123
          </p>
        </div>
      </div>
    </div>
  );
}

export default LoginPage;
