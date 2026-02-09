/**
 * Loading Spinner Component
 */

import React from 'react';

interface SpinnerProps {
  size?: 'sm' | 'md' | 'lg' | 'xl';
  className?: string;
  fullScreen?: boolean;
  text?: string;
}

const sizeClasses = {
  sm: 'h-4 w-4 border-2',
  md: 'h-6 w-6 border-2',
  lg: 'h-8 w-8 border-2',
  xl: 'h-12 w-12 border-3',
};

export function Spinner({ size = 'md', className = '', fullScreen = false, text }: SpinnerProps) {
  const spinner = (
    <div
      className={`
        animate-spin rounded-full
        border-gray-300 border-t-primary-600
        ${sizeClasses[size]}
        ${className}
      `}
      role="status"
      aria-label="Loading"
    />
  );

  if (fullScreen) {
    return (
      <div className="min-h-screen flex flex-col items-center justify-center bg-gray-50">
        {spinner}
        {text && <p className="mt-4 text-gray-600">{text}</p>}
      </div>
    );
  }

  if (text) {
    return (
      <div className="flex flex-col items-center justify-center">
        {spinner}
        <p className="mt-2 text-gray-600 text-sm">{text}</p>
      </div>
    );
  }

  return spinner;
}

/**
 * Full page loading state
 */
interface LoadingScreenProps {
  message?: string;
}

export function LoadingScreen({ message = 'Loading...' }: LoadingScreenProps) {
  return (
    <div className="min-h-screen flex flex-col items-center justify-center bg-gray-50">
      <Spinner size="xl" />
      <p className="mt-4 text-gray-600">{message}</p>
    </div>
  );
}

/**
 * Overlay loading state for sections
 */
interface LoadingOverlayProps {
  isLoading: boolean;
  children: React.ReactNode;
}

export function LoadingOverlay({ isLoading, children }: LoadingOverlayProps) {
  return (
    <div className="relative">
      {children}
      {isLoading && (
        <div className="absolute inset-0 bg-white/70 flex items-center justify-center z-10">
          <Spinner size="lg" />
        </div>
      )}
    </div>
  );
}

export default Spinner;
