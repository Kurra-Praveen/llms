# Loan Management Platform - Frontend

React frontend for the Multi-Tenant Loan Management Platform. Built with React 19, TypeScript, Vite, Tailwind CSS v4, and TanStack Query.

## Features

### Staff (LENDER_ADMIN / LENDER_STAFF / SUPER_ADMIN)
- **Authentication**: JWT-based auth with auto-refresh and protected routes.
- **Dashboard**: Real-time overview of portfolio performance.
- **Borrower Management**: Create, view, and manage borrower profiles — including **Create Login** to provision a borrower self-service account.
- **Loan Management**: Full loan lifecycle (Application → Approval → Disbursement → Repayment → Closure).
- **Payment Processing**: Record and track payments with waterfall allocation support.
- **Reports**: Portfolio summaries and collection analytics.
- **Tenant Management**: Super Admin tools for onboarding new lenders.

### Borrower Self-Service Portal (BORROWER role)
- **Portal Dashboard** (`/portal`): Active loans, total outstanding, next EMI due with overdue alerts.
- **My Loans** (`/portal/loans`): Card-based loan list with per-loan outstanding/paid breakdown.
- **Loan Details** (`/portal/loans/:id`): Loan summary + full repayment schedule.
- **Payment History** (`/portal/payments`): All payments with receipts.
- **Notifications** (`/notifications`): In-app notification center with tap-to-mark-read.

### Shared
- **Notification Bell**: Live unread-count badge in the header (polls every 30s).
- **Role-Based Routing**: Borrowers land on `/portal`; staff land on `/dashboard`. Navigation is role-gated.
- **Responsive Design**: Mobile-friendly UI with sidebar navigation.

## Prerequisites

- Node.js 18+
- npm

## Getting Started

1.  **Install dependencies:**

    ```bash
    npm install
    ```

2.  **Configure Environment:**

    The dev server proxies `/api` → `http://localhost:4044` (see `vite.config.ts`). No `.env` needed for local dev.

3.  **Run Development Server:**

    ```bash
    npm run dev
    ```

    The app will be available at `http://localhost:3000`.

4.  **Start the backend** (required for login/data):

    ```bash
    cd ../loan-platform
    ./docker.sh start
    mvn spring-boot:run -Dspring-boot.run.profiles=dev
    ```

## Project Structure

```
src/
├── assets/         # Static assets
├── components/     # Reusable UI components
│   ├── forms/      # Complex form components (LoanForm, BorrowerForm, PaymentForm)
│   ├── layout/     # Layout components (Sidebar, Header, MainLayout)
│   └── ui/         # Base UI elements (Button, Input, Card, Table, Badge, Spinner)
├── contexts/       # React Contexts (AuthContext)
├── lib/            # Libraries and utilities
│   └── validations/ # Zod schemas for form validation
├── pages/          # Page components arranged by module
│   ├── auth/       # Login
│   ├── borrowers/  # Staff: borrower management
│   ├── dashboard/  # Staff: portfolio dashboard
│   ├── loans/      # Staff: loan management
│   ├── payments/   # Staff: payment recording
│   ├── portal/     # Borrower: self-service portal (dashboard, loans, payments, notifications)
│   ├── reports/    # Staff: reports
│   ├── settings/   # Shared: settings
│   └── tenants/    # Super Admin: tenant management
├── services/       # API service layers
│   ├── api.ts              # Axios instance + interceptors (auth, refresh, logging)
│   ├── authService.ts      # Login, refresh, logout, me, createUserAccount
│   ├── borrowerService.ts  # Staff borrower CRUD
│   ├── loanService.ts      # Staff loan CRUD + lifecycle
│   ├── paymentService.ts   # Staff payment recording
│   ├── portalService.ts    # Borrower self-service (/v1/portal/**)
│   ├── notificationService.ts # Notification center (/v1/notifications)
│   ├── deviceService.ts    # Push device registration (/v1/devices)
│   ├── dashboardService.ts # Staff dashboard stats
│   ├── reportService.ts    # Reports
│   └── tenantService.ts    # Tenant management
├── types/          # TypeScript interface definitions (shared domain models)
└── utils/          # Helper functions (logger)
```

## Key Technologies

- **React 19** with TypeScript (strict mode)
- **React Router v7**: Client-side routing with role-based guards
- **TanStack Query (React Query)**: Server state management and caching
- **React Hook Form + Zod**: Form handling and validation
- **Tailwind CSS v4**: Utility-first styling
- **Headless UI**: Accessible UI components (Menu, Dialog)
- **Axios**: HTTP client with interceptors for auth tokens and 401 refresh

## Roles & Access

| Role | Landing | Can Access |
|------|---------|-----------|
| `SUPER_ADMIN` | `/dashboard` | Everything + tenant management |
| `LENDER_ADMIN` | `/dashboard` | Borrowers, loans, payments, reports, create borrower logins |
| `LENDER_STAFF` | `/dashboard` | Borrowers, loans, payments, reports (no approve/disburse) |
| `BORROWER` | `/portal` | Own profile, loans, schedules, payments, notifications only |

## Development Notes

- **Logging**: Centralized logger (`utils/logger.ts`). Logs visible in browser console in dev mode.
- **API errors**: Handled globally in `services/api.ts` — 401 triggers token refresh, then logout+redirect on failure.
- **Known pre-existing issues**: 33 TypeScript errors in `components/forms/*` (zod v4 / react-hook-form type mismatches) — unrelated to the portal work, tracked for a separate cleanup pass.

## Related Documentation

- **Mobile readiness & roadmap**: `../docx/MOBILE_READINESS_PLAN.md` (living tracker)
- **Backend API**: `../loan-platform/docs/API_DOCUMENTATION.md`
- **Backend architecture**: `../loan-platform/docs/ARCHITECTURE.md`
