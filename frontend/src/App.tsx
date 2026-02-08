/**
 * Main App Component with Routing
 */

import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { AuthProvider } from '@/contexts/AuthContext';
import { MainLayout } from '@/components/layout';
import { LoginPage } from '@/pages/auth/LoginPage';
import { DashboardPage } from '@/pages/dashboard/DashboardPage';
import {
  BorrowerListPage,
  BorrowerDetailsPage,
  AddBorrowerPage,
  EditBorrowerPage,
} from '@/pages/borrowers';
import { PaymentListPage, RecordPaymentPage } from '@/pages/payments';
import { ReportsPage } from '@/pages/reports';
import { SettingsPage } from '@/pages/settings';
import { TenantListPage, TenantOnboardingPage } from '@/pages/tenants';
import { logger } from '@/utils/logger';

const appLogger = logger.scope('App');

// Create a client for React Query
const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 1000 * 60 * 5, // 5 minutes
      retry: 1,
      refetchOnWindowFocus: false,
    },
  },
});

appLogger.info('App initializing', {
  environment: import.meta.env.MODE,
  baseUrl: import.meta.env.BASE_URL,
});

import { LoanListPage, LoanDetailsPage } from '@/pages/loans';
import { LoanForm } from '@/components/forms/LoanForm';

function NotFoundPage() {
  appLogger.warn('404 - Page not found');
  return (
    <div className="min-h-screen flex items-center justify-center">
      <div className="text-center">
        <h1 className="text-6xl font-bold text-gray-300">404</h1>
        <p className="text-xl text-gray-600 mt-4">Page not found</p>
        <a
          href="/dashboard"
          className="mt-4 inline-block text-primary-600 hover:text-primary-700"
        >
          Go to Dashboard
        </a>
      </div>
    </div>
  );
}

function App() {
  appLogger.info('App component rendering');

  return (
    <QueryClientProvider client={queryClient}>
      <BrowserRouter>
        <AuthProvider>
          <Routes>
            {/* Public routes */}
            <Route path="/login" element={<LoginPage />} />

            {/* Protected routes with layout */}
            <Route element={<MainLayout />}>
              <Route path="/dashboard" element={<DashboardPage />} />
              <Route path="/borrowers" element={<BorrowerListPage />} />
              <Route path="/borrowers/new" element={<AddBorrowerPage />} />
              <Route path="/borrowers/:id" element={<BorrowerDetailsPage />} />
              <Route path="/borrowers/:id/edit" element={<EditBorrowerPage />} />

              <Route path="/loans" element={<LoanListPage />} />
              <Route path="/loans/new" element={<LoanForm />} />
              <Route path="/loans/:id" element={<LoanDetailsPage />} />
              <Route path="/payments" element={<PaymentListPage />} />
              <Route path="/payments/new" element={<RecordPaymentPage />} />
              <Route path="/reports" element={<ReportsPage />} />
              <Route path="/tenants" element={<TenantListPage />} />
              <Route path="/tenants/new" element={<TenantOnboardingPage />} />
              <Route path="/settings" element={<SettingsPage />} />
            </Route>

            {/* Redirect root to dashboard */}
            <Route path="/" element={<Navigate to="/dashboard" replace />} />

            {/* 404 */}
            <Route path="*" element={<NotFoundPage />} />
          </Routes>
        </AuthProvider>
      </BrowserRouter>
    </QueryClientProvider>
  );
}

export default App;
