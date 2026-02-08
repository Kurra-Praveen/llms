/**
 * Main Layout Component
 * Wraps authenticated pages with sidebar, header, and content area
 */

import { useState } from 'react';
import { Outlet, Navigate } from 'react-router-dom';
import { Sidebar } from './Sidebar';
import { Header } from './Header';
import { useAuth } from '@/contexts/AuthContext';
import { LoadingScreen } from '@/components/ui';
import { logger } from '@/utils/logger';

const layoutLogger = logger.scope('MainLayout');

export function MainLayout() {
  const { isAuthenticated, isLoading } = useAuth();
  const [sidebarOpen, setSidebarOpen] = useState(false);

  layoutLogger.debug('MainLayout render', { isAuthenticated, isLoading, sidebarOpen });

  // Show loading screen while checking auth
  if (isLoading) {
    layoutLogger.debug('Showing loading screen');
    return <LoadingScreen message="Loading..." />;
  }

  // Redirect to login if not authenticated
  if (!isAuthenticated) {
    layoutLogger.info('User not authenticated, redirecting to login');
    return <Navigate to="/login" replace />;
  }

  return (
    <div className="min-h-screen flex bg-gray-50">
      {/* Sidebar */}
      <Sidebar isOpen={sidebarOpen} onClose={() => setSidebarOpen(false)} />

      {/* Main content area */}
      <div className="flex-1 flex flex-col min-w-0">
        {/* Header */}
        <Header onMenuClick={() => setSidebarOpen(true)} />

        {/* Page content */}
        <main className="flex-1 overflow-y-auto p-4 lg:p-6">
          <Outlet />
        </main>
      </div>
    </div>
  );
}

export default MainLayout;
