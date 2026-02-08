# Loan Management Platform - Application Summary

**Version:** 1.0.0
**Last Updated:** February 7, 2026
**Technology Stack:** Spring Boot 3.x (Backend) + React 18 with TypeScript (Frontend) + PostgreSQL

---

## Overview

A comprehensive, multi-tenant SaaS loan management platform designed for microfinance institutions, NBFCs, and local lenders. The platform provides end-to-end loan lifecycle management from borrower onboarding to loan closure with advanced features like multiple interest calculation types, flexible repayment schedules, and double-entry accounting.

---

## Architecture

### Backend (Spring Boot)
```
loan-platform/
├── src/main/java/com/loanplatform/
│   ├── auth/           # Authentication & User Management
│   ├── tenant/         # Multi-tenancy Module
│   ├── borrower/       # Borrower Management
│   ├── loan/           # Loan Lifecycle & Schedules
│   ├── payment/        # Payment Processing
│   ├── ledger/         # Double-Entry Accounting
│   ├── notification/   # Notifications (SMS, Email)
│   ├── audit/          # Audit Trail
│   └── common/         # Shared Utilities & Base Entities
└── src/main/resources/db/migration/  # Flyway Migrations (V1-V10)
```

### Frontend (React + TypeScript)
```
frontend/
├── src/
│   ├── pages/          # Route Pages
│   ├── components/     # Reusable UI Components
│   ├── services/       # API Service Layer
│   ├── lib/            # Utilities & Validations
│   ├── hooks/          # Custom React Hooks
│   └── types/          # TypeScript Type Definitions
```

---

## Core Modules

### 1. Multi-Tenancy
- **Complete tenant isolation** with row-level security
- Subscription-based model with configurable limits
- Tenant statuses: ACTIVE, SUSPENDED, INACTIVE
- JSONB settings for flexible configuration

### 2. Authentication & Authorization
- JWT-based authentication with refresh tokens
- Role-based access control:
  - `SUPER_ADMIN` - Platform administrator
  - `LENDER_ADMIN` - Tenant/lender administrator
  - `LENDER_STAFF` - Regular staff users
  - `BORROWER` - Borrower portal access (future)
- Account lockout after failed login attempts
- Password hashing with BCrypt

### 3. Borrower Management
- Comprehensive borrower profiles with KYC
- Contact information (phone, email, alternate phone)
- Identity verification (National ID, Passport, License, etc.)
- Address management
- Employment and income tracking
- Risk assessment (credit rating, risk score, risk band)
- Borrower statuses: ACTIVE, BLOCKED, BLACKLISTED, INACTIVE

### 4. Loan Management
**Interest Calculation Types:**
| Type | Description |
|------|-------------|
| FLAT | Fixed interest calculated on principal |
| REDUCING_BALANCE | EMI-based, interest on outstanding balance |
| SIMPLE | Simple interest (P × R × T) |
| INTEREST_ONLY | Only interest payments, principal at end |
| BULLET | Single payment at maturity |
| DAILY_FIXED | Fixed rupee amount per ₹100 of principal per day |

**Repayment Frequencies:**
- DAILY, WEEKLY, BI_WEEKLY, MONTHLY, QUARTERLY

**Loan Lifecycle:**
```
DRAFT → PENDING_APPROVAL → APPROVED → ACTIVE → CLOSED
                              ↓
                          REJECTED
                              ↓
         CANCELLED ← WRITTEN_OFF
```

**Key Features:**
- Automated EMI calculation
- Repayment schedule generation
- Processing fee and other charges
- Upfront charge deduction from disbursement
- Grace period support
- Collateral tracking
- DPD (Days Past Due) tracking
- NPA detection

### 5. Payment Processing
- Multiple payment methods (CASH, BANK_TRANSFER, UPI, CHEQUE, etc.)
- Idempotency key support for duplicate prevention
- Intelligent allocation engine: Penalty → Interest → Principal
- Payment allocation tracking to specific installments
- Receipt generation
- Payment reversal capability
- Excess payment handling
- Automatic loan closure when fully paid

### 6. Double-Entry Accounting (Ledger)
- Chart of accounts (ASSET, LIABILITY, INCOME, EXPENSE)
- Transaction and entry management
- Debit/Credit tracking
- System account flagging

### 7. Notifications
- Multi-channel: SMS, EMAIL, PUSH, IN_APP
- Template support
- Scheduled notifications
- Status tracking (PENDING, SENT, DELIVERED, FAILED)
- Retry mechanism

### 8. Audit Trail
- Comprehensive audit logging
- Before/after value tracking (JSONB)
- IP address and user agent logging
- Entity-based audit trail

---

## Database Schema

### Core Tables
| Table | Description |
|-------|-------------|
| `tenants` | Lender organizations |
| `users` | User accounts (all roles) |
| `refresh_tokens` | JWT refresh tokens |
| `borrowers` | Borrower profiles |
| `loans` | Loan master data |
| `repayment_schedules` | Installment details |
| `payments` | Payment transactions |
| `payment_allocations` | Payment to schedule mapping |
| `ledger_accounts` | Chart of accounts |
| `ledger_transactions` | Financial transactions |
| `ledger_entries` | Double-entry records |
| `notifications` | Notification queue |
| `audit_logs` | Audit trail |

### Database Migrations
| Version | Description |
|---------|-------------|
| V1 | Tenants, Users, Refresh Tokens, Audit Logs |
| V2 | Borrowers table |
| V3 | Loans and Repayment Schedules |
| V4 | Payments and Allocations |
| V5 | Ledger (Double-Entry Accounting) |
| V6 | Collections and Notifications |
| V7 | Default seed data |
| V8 | Super Admin user |
| V9 | Upfront charge deduction fields |
| V10 | Daily fixed amount field |

---

## API Endpoints

### Authentication (`/api/v1/auth`)
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/login` | User login |
| POST | `/refresh` | Refresh access token |
| POST | `/logout` | User logout |
| GET | `/me` | Get current user |

### Tenants (`/api/v1/tenants`)
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/` | List all tenants |
| POST | `/` | Create tenant |
| GET | `/{id}` | Get tenant details |
| PUT | `/{id}` | Update tenant |
| DELETE | `/{id}` | Delete tenant |

### Borrowers (`/api/v1/borrowers`)
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/` | List borrowers (paginated) |
| POST | `/` | Create borrower |
| GET | `/{id}` | Get borrower details |
| PUT | `/{id}` | Update borrower |
| GET | `/search` | Search borrowers |

### Loans (`/api/v1/loans`)
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/` | List loans (paginated) |
| POST | `/` | Create loan |
| GET | `/{id}` | Get loan details |
| POST | `/{id}/approve` | Approve loan |
| POST | `/{id}/reject` | Reject loan |
| POST | `/{id}/disburse` | Disburse loan |
| GET | `/{id}/schedule` | Get repayment schedule |
| POST | `/{id}/close` | Close loan |

### Payments (`/api/v1/payments`)
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/` | List payments |
| POST | `/` | Record payment |
| GET | `/{id}` | Get payment details |
| POST | `/{id}/reverse` | Reverse payment |
| GET | `/loan/{loanId}` | Payments by loan |

---

## Frontend Pages

| Page | Route | Description |
|------|-------|-------------|
| Login | `/login` | User authentication |
| Dashboard | `/` | KPIs and overview |
| Borrower List | `/borrowers` | All borrowers |
| Borrower Details | `/borrowers/:id` | Borrower profile |
| Add Borrower | `/borrowers/new` | Create borrower |
| Loan List | `/loans` | All loans |
| Loan Details | `/loans/:id` | Loan details with schedule |
| New Loan | `/loans/new` | Create loan application |
| Payment List | `/payments` | All payments |
| Record Payment | `/payments/new` | Record new payment |
| Reports | `/reports` | Reports dashboard |
| Portfolio Report | `/reports/portfolio` | Portfolio analytics |
| Collection Report | `/reports/collection` | Collection analysis |
| Tenant List | `/tenants` | Manage tenants (Super Admin) |
| Tenant Onboarding | `/tenants/new` | Onboard new lender |
| Settings | `/settings` | Application settings |

---

## Recent Enhancements (Session Work)

### 1. EMI Preview in Loan Form
- Real-time EMI calculation as user inputs loan details
- Shows monthly payment, total interest, total payable, number of payments
- Supports all interest types and repayment frequencies

### 2. Loan Disbursement Workflow
- Added "Approve Loan" button for DRAFT status
- Enhanced disburse modal with date pickers
- Loan summary display before disbursement

### 3. Dashboard with Real Data
- Replaced hardcoded data with API calls
- Real-time metrics: total borrowers, active loans, outstanding amount
- Clickable cards for navigation

### 4. Reports with Real Data
- Portfolio Report: loans by status, interest types, risk analysis
- Collection Report: payment trends, collection efficiency

### 5. Upfront Charge Deduction
- Option to deduct processing fee and other charges from disbursement
- Net disbursement amount calculation
- Charges breakdown in loan details

### 6. Daily Fixed Interest Type
- New interest type for local lending convenience
- Fixed rupee amount per ₹100 of principal per day
- Example: ₹5 rate on ₹10,000 loan = (10,000/100) × 5 = ₹500/day
- Clear UI showing rate and calculated daily interest

### 7. Bug Fixes
- Fixed LazyInitializationException in AuthService
- Fixed status handling (ACTIVE vs DISBURSED)
- Removed unused imports

---

## Technical Stack

### Backend
- **Framework:** Spring Boot 3.x
- **Language:** Java 17+
- **Database:** PostgreSQL 15+
- **ORM:** Hibernate/JPA with MapStruct
- **Migration:** Flyway
- **Security:** Spring Security + JWT
- **Build:** Maven

### Frontend
- **Framework:** React 18
- **Language:** TypeScript
- **State Management:** React Query (TanStack Query)
- **Routing:** React Router 6
- **Forms:** React Hook Form + Zod
- **Styling:** Tailwind CSS
- **Build:** Vite
- **Icons:** Heroicons

---

## Security Features
- JWT authentication with short-lived access tokens
- Refresh token rotation
- Password hashing (BCrypt)
- Role-based authorization
- Multi-tenant data isolation
- Account lockout after failed attempts
- Soft delete for data retention
- Optimistic locking with versioning
- Audit trail for compliance

---

## Contact

For questions or support, contact the development team.
