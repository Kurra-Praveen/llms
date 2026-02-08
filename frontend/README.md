# Loan Management Platform - Frontend

This is the React frontend for the Multi-Tenant Loan Management Platform. It is built with React 18, TypeScript, Vite, Tailwind CSS, and TanStack Query.

## Features

- **Authentication**: JWT-based auth with auto-refresh and protected routes.
- **Dashboard**: Real-time overview of portfolio performance.
- **Borrower Management**: Create, view, and manage borrower profiles.
- **Loan Management**: Full loan lifecycle (Application -> Approval -> Disbursement -> Repayment -> Closure).
- **Payment Processing**: Record and track payments with waterfall allocation support.
- **Reports**: Portfolio summaries and collection analytics.
- **Tenant Management**: Super Admin tools for onboarding new lenders.
- **Responsive Design**: Mobile-friendly UI with sidebar navigation.

## Prerequisites

- Node.js 18+
- npm or yarn

## Getting Started

1.  **Install dependencies:**

    ```bash
    npm install
    ```

2.  **Configure Environment:**

    Create a `.env` file in the root directory (optional, defaults are set in vite.config.ts):

    ```env
    VITE_API_URL=http://localhost:4044/api
    ```

3.  **Run Development Server:**

    ```bash
    npm run dev
    ```

    The app will be available at `http://localhost:3000`.

## Project Structure

```
src/
├── assets/         # Static assets
├── components/     # Reusable UI components
│   ├── forms/      # Complex form components (LoanForm, BorrowerForm)
│   ├── layout/     # Layout components (Sidebar, Header)
│   └── ui/         # Base UI elements (Button, Input, Card, Table)
├── contexts/       # React Contexts (AuthContext)
├── lib/            # Libraries and utilities
│   └── validations/ # Zod schemas for form validation
├── pages/          # Page components arranged by module
│   ├── auth/
│   ├── borrowers/
│   ├── dashboard/
│   ├── loans/
│   ├── payments/
│   ├── reports/
│   ├── settings/
│   └── tenants/
├── services/       # API service layers
├── types/          # TypeScript interface definitions
└── utils/          # Helper functions (logger, formatter)
```

## Key Technologies

- **React Router v6**: Client-side routing.
- **TanStack Query (React Query)**: Server state management and caching.
- **React Hook Form + Zod**: Form handling and validation.
- **Tailwind CSS**: Utility-first styling.
- **Headless UI**: Accessible UI components (Menu, Dialog).
- **Axios**: HTTP client with interceptors for auth tokens.

## Development Notes

- **Mocking**: The `reportService` currently uses mock data. Connect it to backend endpoints when available.
- **Logging**: A centralized logger is used (`utils/logger.ts`). Logs are visible in the browser console in development mode.
