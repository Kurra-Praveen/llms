# Database Schema Documentation

Complete database schema reference for the Multi-Tenant Loan Management Platform.

## Overview

The platform uses PostgreSQL with the following design principles:
- **Multi-tenancy**: Row-level isolation using `tenant_id` column
- **UUID Primary Keys**: All tables use UUID for primary keys
- **Soft Deletes**: Critical entities support soft deletion
- **Optimistic Locking**: Version column for concurrent updates
- **Audit Trail**: Created/updated timestamps on all entities

---

## Core Tables

### 1. tenants

Stores tenant (microfinance institution) information.

```sql
CREATE TABLE tenants (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    business_name           VARCHAR(255) NOT NULL,
    business_code           VARCHAR(50) NOT NULL UNIQUE,
    contact_email           VARCHAR(255) NOT NULL,
    contact_phone           VARCHAR(20),
    address                 TEXT,
    status                  VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    subscription_plan       VARCHAR(50) DEFAULT 'BASIC',
    subscription_start_date DATE,
    subscription_end_date   DATE,
    max_borrowers           INTEGER DEFAULT 100,
    max_loans               INTEGER DEFAULT 500,
    settings                JSONB DEFAULT '{}',
    created_at              TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMP,
    version                 BIGINT DEFAULT 0
);

CREATE INDEX idx_tenants_business_code ON tenants(business_code);
CREATE INDEX idx_tenants_status ON tenants(status);
```

**Status Values:**
- `ACTIVE` - Tenant is operational
- `SUSPENDED` - Temporarily suspended
- `INACTIVE` - Deactivated

**Settings JSONB Example:**
```json
{
  "currency": "INR",
  "dateFormat": "dd/MM/yyyy",
  "defaultInterestType": "REDUCING_BALANCE",
  "enablePenalty": true,
  "penaltyRate": 2.0
}
```

---

### 2. users

Stores user accounts for authentication and authorization.

```sql
CREATE TABLE users (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id               UUID REFERENCES tenants(id),
    email                   VARCHAR(255) NOT NULL,
    password_hash           VARCHAR(255) NOT NULL,
    first_name              VARCHAR(100) NOT NULL,
    last_name               VARCHAR(100) NOT NULL,
    phone                   VARCHAR(20),
    role                    VARCHAR(50) NOT NULL,
    status                  VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    email_verified          BOOLEAN DEFAULT FALSE,
    last_login_at           TIMESTAMP,
    failed_login_attempts   INTEGER DEFAULT 0,
    locked_until            TIMESTAMP,
    password_changed_at     TIMESTAMP,
    created_by              UUID,
    updated_by              UUID,
    is_deleted              BOOLEAN DEFAULT FALSE,
    deleted_at              TIMESTAMP,
    deleted_by              UUID,
    created_at              TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMP,
    version                 BIGINT DEFAULT 0
);

CREATE UNIQUE INDEX idx_users_email ON users(email) WHERE is_deleted = FALSE;
CREATE INDEX idx_users_tenant_id ON users(tenant_id);
CREATE INDEX idx_users_role ON users(role);
CREATE INDEX idx_users_status ON users(status);
```

**Role Values:**
- `SUPER_ADMIN` - Platform administrator (no tenant_id)
- `LENDER_ADMIN` - Tenant administrator
- `LENDER_STAFF` - Loan officer / staff
- `BORROWER` - Borrower self-service access

**Status Values:**
- `ACTIVE` - Can log in
- `INACTIVE` - Account disabled
- `LOCKED` - Temporarily locked (too many failed attempts)
- `PENDING_VERIFICATION` - Email verification pending

---

### 3. refresh_tokens

Stores JWT refresh tokens for session management.

```sql
CREATE TABLE refresh_tokens (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID NOT NULL REFERENCES users(id),
    token           VARCHAR(500) NOT NULL UNIQUE,
    expires_at      TIMESTAMP NOT NULL,
    is_revoked      BOOLEAN DEFAULT FALSE,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens(user_id);
CREATE INDEX idx_refresh_tokens_token ON refresh_tokens(token);
```

---

### 4. borrowers

Stores borrower (customer) information.

```sql
CREATE TABLE borrowers (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id           UUID NOT NULL REFERENCES tenants(id),
    borrower_code       VARCHAR(50) NOT NULL,
    full_name           VARCHAR(255) NOT NULL,
    date_of_birth       DATE,
    gender              VARCHAR(20),
    phone               VARCHAR(20) NOT NULL,
    alternate_phone     VARCHAR(20),
    email               VARCHAR(255),
    id_type             VARCHAR(50),
    id_number           VARCHAR(100),
    address_line1       VARCHAR(255),
    address_line2       VARCHAR(255),
    city                VARCHAR(100),
    state               VARCHAR(100),
    postal_code         VARCHAR(20),
    country             VARCHAR(50) DEFAULT 'India',
    occupation          VARCHAR(100),
    employer_name       VARCHAR(255),
    monthly_income      DECIMAL(18,2),
    credit_rating       VARCHAR(20) DEFAULT 'UNRATED',
    risk_score          INTEGER,
    risk_band           VARCHAR(20) DEFAULT 'UNRATED',
    status              VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    notes               TEXT,
    metadata            JSONB DEFAULT '{}',
    created_by          UUID,
    updated_by          UUID,
    is_deleted          BOOLEAN DEFAULT FALSE,
    deleted_at          TIMESTAMP,
    deleted_by          UUID,
    created_at          TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP,
    version             BIGINT DEFAULT 0
);

CREATE UNIQUE INDEX idx_borrowers_code_tenant ON borrowers(tenant_id, borrower_code);
CREATE UNIQUE INDEX idx_borrowers_phone_tenant ON borrowers(tenant_id, phone) WHERE is_deleted = FALSE;
CREATE INDEX idx_borrowers_tenant_id ON borrowers(tenant_id);
CREATE INDEX idx_borrowers_status ON borrowers(status);
CREATE INDEX idx_borrowers_full_name ON borrowers(full_name);
```

**Status Values:**
- `ACTIVE` - Can apply for loans
- `BLOCKED` - Temporarily blocked
- `BLACKLISTED` - Permanently blocked
- `INACTIVE` - Soft deleted

**Risk Band Values:**
- `LOW` - Score 80-100
- `MEDIUM` - Score 50-79
- `HIGH` - Score 20-49
- `VERY_HIGH` - Score 0-19
- `UNRATED` - Not yet scored

---

### 5. loans

Stores loan applications and active loans.

```sql
CREATE TABLE loans (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id               UUID NOT NULL REFERENCES tenants(id),
    borrower_id             UUID NOT NULL REFERENCES borrowers(id),
    loan_product_id         UUID,
    loan_number             VARCHAR(50) NOT NULL,
    principal_amount        DECIMAL(18,2) NOT NULL,
    interest_rate           DECIMAL(8,4) NOT NULL,
    interest_type           VARCHAR(30) NOT NULL,
    tenure_months           INTEGER NOT NULL,
    repayment_frequency     VARCHAR(20) NOT NULL DEFAULT 'MONTHLY',
    processing_fee          DECIMAL(18,2) DEFAULT 0,
    other_charges           DECIMAL(18,2) DEFAULT 0,
    application_date        DATE NOT NULL,
    approval_date           DATE,
    disbursement_date       DATE,
    first_payment_date      DATE,
    maturity_date           DATE,
    closure_date            DATE,
    emi_amount              DECIMAL(18,2),
    total_interest          DECIMAL(18,2),
    total_payable           DECIMAL(18,2),
    outstanding_principal   DECIMAL(18,2) DEFAULT 0,
    outstanding_interest    DECIMAL(18,2) DEFAULT 0,
    outstanding_penalty     DECIMAL(18,2) DEFAULT 0,
    total_paid              DECIMAL(18,2) DEFAULT 0,
    principal_paid          DECIMAL(18,2) DEFAULT 0,
    interest_paid           DECIMAL(18,2) DEFAULT 0,
    penalty_paid            DECIMAL(18,2) DEFAULT 0,
    status                  VARCHAR(30) NOT NULL DEFAULT 'DRAFT',
    dpd                     INTEGER DEFAULT 0,
    dpd_bucket              VARCHAR(20),
    is_npa                  BOOLEAN DEFAULT FALSE,
    npa_date                DATE,
    requires_approval       BOOLEAN DEFAULT FALSE,
    approved_by             UUID,
    approval_notes          TEXT,
    has_collateral          BOOLEAN DEFAULT FALSE,
    collateral_type         VARCHAR(50),
    collateral_value        DECIMAL(18,2),
    collateral_description  TEXT,
    penalty_type            VARCHAR(30),
    penalty_rate            DECIMAL(8,4),
    grace_period_days       INTEGER DEFAULT 0,
    notes                   TEXT,
    metadata                JSONB DEFAULT '{}',
    created_by              UUID,
    updated_by              UUID,
    is_deleted              BOOLEAN DEFAULT FALSE,
    deleted_at              TIMESTAMP,
    deleted_by              UUID,
    created_at              TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMP,
    version                 BIGINT DEFAULT 0
);

CREATE UNIQUE INDEX idx_loans_number_tenant ON loans(tenant_id, loan_number);
CREATE INDEX idx_loans_tenant_id ON loans(tenant_id);
CREATE INDEX idx_loans_borrower_id ON loans(borrower_id);
CREATE INDEX idx_loans_status ON loans(status);
CREATE INDEX idx_loans_disbursement_date ON loans(disbursement_date);
CREATE INDEX idx_loans_maturity_date ON loans(maturity_date);
```

**Loan Status Values:**
- `DRAFT` - Initial creation
- `PENDING_APPROVAL` - Awaiting approval
- `APPROVED` - Approved, pending disbursement
- `REJECTED` - Application rejected
- `ACTIVE` - Disbursed and active
- `DISBURSED` - Recently disbursed (same as ACTIVE)
- `CLOSED` - Fully repaid
- `WRITTEN_OFF` - Bad debt write-off
- `CANCELLED` - Cancelled before disbursement

**Interest Type Values:**
- `FLAT` - Flat rate (simple on original principal)
- `REDUCING_BALANCE` - EMI on reducing balance
- `SIMPLE` - Simple interest
- `INTEREST_ONLY` - Interest payments only, principal at end
- `BULLET` - Single payment at maturity

**Repayment Frequency Values:**
- `DAILY`
- `WEEKLY`
- `BI_WEEKLY`
- `MONTHLY`
- `QUARTERLY`

---

### 6. repayment_schedules

Stores individual installment details for each loan.

```sql
CREATE TABLE repayment_schedules (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id               UUID NOT NULL REFERENCES tenants(id),
    loan_id                 UUID NOT NULL REFERENCES loans(id),
    installment_number      INTEGER NOT NULL,
    due_date                DATE NOT NULL,
    principal_component     DECIMAL(18,2) NOT NULL,
    interest_component      DECIMAL(18,2) NOT NULL,
    installment_amount      DECIMAL(18,2) NOT NULL,
    outstanding_after       DECIMAL(18,2) NOT NULL,
    principal_paid          DECIMAL(18,2) DEFAULT 0,
    interest_paid           DECIMAL(18,2) DEFAULT 0,
    penalty_paid            DECIMAL(18,2) DEFAULT 0,
    total_paid              DECIMAL(18,2) DEFAULT 0,
    penalty_amount          DECIMAL(18,2) DEFAULT 0,
    penalty_calculated_at   TIMESTAMP,
    status                  VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    paid_date               DATE,
    days_past_due           INTEGER DEFAULT 0,
    created_at              TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMP,
    version                 BIGINT DEFAULT 0
);

CREATE UNIQUE INDEX idx_schedules_loan_installment ON repayment_schedules(loan_id, installment_number);
CREATE INDEX idx_schedules_tenant_id ON repayment_schedules(tenant_id);
CREATE INDEX idx_schedules_loan_id ON repayment_schedules(loan_id);
CREATE INDEX idx_schedules_due_date ON repayment_schedules(due_date);
CREATE INDEX idx_schedules_status ON repayment_schedules(status);
```

**Schedule Status Values:**
- `PENDING` - Not yet due or unpaid
- `PARTIAL` - Partially paid
- `PAID` - Fully paid
- `OVERDUE` - Past due date
- `WAIVED` - Waived off

---

### 7. payments

Stores payment transactions.

```sql
CREATE TABLE payments (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id           UUID NOT NULL REFERENCES tenants(id),
    loan_id             UUID NOT NULL REFERENCES loans(id),
    payment_number      VARCHAR(50) NOT NULL,
    idempotency_key     VARCHAR(100),
    payment_date        DATE NOT NULL,
    payment_time        TIMESTAMP DEFAULT NOW(),
    amount_paid         DECIMAL(18,2) NOT NULL,
    payment_method      VARCHAR(30) NOT NULL,
    reference_number    VARCHAR(100),
    transaction_id      VARCHAR(100),
    principal_paid      DECIMAL(18,2) DEFAULT 0,
    interest_paid       DECIMAL(18,2) DEFAULT 0,
    penalty_paid        DECIMAL(18,2) DEFAULT 0,
    fee_paid            DECIMAL(18,2) DEFAULT 0,
    excess_amount       DECIMAL(18,2) DEFAULT 0,
    receipt_number      VARCHAR(50),
    receipt_generated   BOOLEAN DEFAULT FALSE,
    status              VARCHAR(20) NOT NULL DEFAULT 'COMPLETED',
    is_reversed         BOOLEAN DEFAULT FALSE,
    reversed_at         TIMESTAMP,
    reversed_by         UUID,
    reversal_reason     TEXT,
    original_payment_id UUID,
    notes               TEXT,
    metadata            JSONB DEFAULT '{}',
    created_by          UUID,
    updated_by          UUID,
    created_at          TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP,
    version             BIGINT DEFAULT 0
);

CREATE UNIQUE INDEX idx_payments_number_tenant ON payments(tenant_id, payment_number);
CREATE UNIQUE INDEX idx_payments_idempotency ON payments(tenant_id, idempotency_key)
    WHERE idempotency_key IS NOT NULL;
CREATE INDEX idx_payments_tenant_id ON payments(tenant_id);
CREATE INDEX idx_payments_loan_id ON payments(loan_id);
CREATE INDEX idx_payments_payment_date ON payments(payment_date);
CREATE INDEX idx_payments_status ON payments(status);
```

**Payment Method Values:**
- `CASH`
- `BANK_TRANSFER`
- `MOBILE_MONEY`
- `CHEQUE`
- `CARD`
- `WALLET`
- `OTHER`

**Payment Status Values:**
- `PENDING` - Awaiting confirmation
- `COMPLETED` - Successfully processed
- `FAILED` - Transaction failed
- `REVERSED` - Reversed/refunded

---

### 8. payment_allocations

Stores how each payment is allocated to schedule installments.

```sql
CREATE TABLE payment_allocations (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id               UUID NOT NULL REFERENCES tenants(id),
    payment_id              UUID NOT NULL REFERENCES payments(id),
    schedule_id             UUID NOT NULL REFERENCES repayment_schedules(id),
    principal_allocated     DECIMAL(18,2) DEFAULT 0,
    interest_allocated      DECIMAL(18,2) DEFAULT 0,
    penalty_allocated       DECIMAL(18,2) DEFAULT 0,
    total_allocated         DECIMAL(18,2) DEFAULT 0,
    created_at              TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_allocations_payment_id ON payment_allocations(payment_id);
CREATE INDEX idx_allocations_schedule_id ON payment_allocations(schedule_id);
```

---

### 9. ledger_accounts

Chart of accounts for double-entry bookkeeping.

```sql
CREATE TABLE ledger_accounts (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id           UUID NOT NULL REFERENCES tenants(id),
    account_code        VARCHAR(50) NOT NULL,
    account_name        VARCHAR(255) NOT NULL,
    account_type        VARCHAR(20) NOT NULL,
    parent_account_id   UUID REFERENCES ledger_accounts(id),
    description         TEXT,
    is_system_account   BOOLEAN DEFAULT FALSE,
    is_active           BOOLEAN DEFAULT TRUE,
    created_at          TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP,
    version             BIGINT DEFAULT 0
);

CREATE UNIQUE INDEX idx_ledger_accounts_code_tenant ON ledger_accounts(tenant_id, account_code);
CREATE INDEX idx_ledger_accounts_tenant_id ON ledger_accounts(tenant_id);
CREATE INDEX idx_ledger_accounts_type ON ledger_accounts(account_type);
```

**Account Type Values:**
- `ASSET` - Increases on debit
- `LIABILITY` - Increases on credit
- `INCOME` - Increases on credit
- `EXPENSE` - Increases on debit

---

### 10. ledger_transactions

Transaction headers for ledger entries.

```sql
CREATE TABLE ledger_transactions (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id           UUID NOT NULL REFERENCES tenants(id),
    transaction_number  VARCHAR(50) NOT NULL,
    transaction_type    VARCHAR(50) NOT NULL,
    reference_type      VARCHAR(50),
    reference_id        UUID,
    transaction_date    DATE NOT NULL,
    description         TEXT,
    total_amount        DECIMAL(18,2) NOT NULL,
    is_reversed         BOOLEAN DEFAULT FALSE,
    reversed_at         TIMESTAMP,
    reversed_by         UUID,
    original_transaction_id UUID,
    metadata            JSONB DEFAULT '{}',
    created_by          UUID,
    created_at          TIMESTAMP NOT NULL DEFAULT NOW(),
    version             BIGINT DEFAULT 0
);

CREATE INDEX idx_ledger_transactions_tenant_id ON ledger_transactions(tenant_id);
CREATE INDEX idx_ledger_transactions_reference ON ledger_transactions(reference_type, reference_id);
CREATE INDEX idx_ledger_transactions_date ON ledger_transactions(transaction_date);
```

**Transaction Type Values:**
- `DISBURSEMENT` - Loan disbursement
- `REPAYMENT` - Payment received
- `INTEREST_ACCRUAL` - Interest accrual
- `PENALTY_CHARGE` - Penalty applied
- `FEE_CHARGE` - Fee charged
- `WRITE_OFF` - Bad debt write-off
- `REVERSAL` - Transaction reversal

---

### 11. ledger_entries

Individual debit/credit entries for each transaction.

```sql
CREATE TABLE ledger_entries (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id           UUID NOT NULL REFERENCES tenants(id),
    transaction_id      UUID NOT NULL REFERENCES ledger_transactions(id),
    account_id          UUID NOT NULL REFERENCES ledger_accounts(id),
    entry_type          VARCHAR(10) NOT NULL,
    amount              DECIMAL(18,2) NOT NULL,
    description         TEXT,
    created_at          TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_ledger_entries_transaction_id ON ledger_entries(transaction_id);
CREATE INDEX idx_ledger_entries_account_id ON ledger_entries(account_id);
```

**Entry Type Values:**
- `DEBIT`
- `CREDIT`

---

### 12. notifications

Stores notification records.

```sql
CREATE TABLE notifications (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id           UUID NOT NULL REFERENCES tenants(id),
    user_id             UUID REFERENCES users(id),
    borrower_id         UUID REFERENCES borrowers(id),
    channel             VARCHAR(20) NOT NULL,
    template_code       VARCHAR(50),
    recipient           VARCHAR(255) NOT NULL,
    subject             VARCHAR(500),
    content             TEXT NOT NULL,
    status              VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    sent_at             TIMESTAMP,
    delivered_at        TIMESTAMP,
    error_message       TEXT,
    retry_count         INTEGER DEFAULT 0,
    metadata            JSONB DEFAULT '{}',
    created_at          TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP
);

CREATE INDEX idx_notifications_tenant_id ON notifications(tenant_id);
CREATE INDEX idx_notifications_status ON notifications(status);
CREATE INDEX idx_notifications_channel ON notifications(channel);
```

**Channel Values:**
- `EMAIL`
- `SMS`
- `PUSH`
- `IN_APP`

**Status Values:**
- `PENDING`
- `SENT`
- `DELIVERED`
- `FAILED`

---

### 13. audit_logs

Stores audit trail for sensitive operations.

```sql
CREATE TABLE audit_logs (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id           UUID,
    user_id             UUID,
    action              VARCHAR(100) NOT NULL,
    entity_type         VARCHAR(100),
    entity_id           UUID,
    old_values          JSONB,
    new_values          JSONB,
    ip_address          VARCHAR(50),
    user_agent          TEXT,
    metadata            JSONB DEFAULT '{}',
    created_at          TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_audit_logs_tenant_id ON audit_logs(tenant_id);
CREATE INDEX idx_audit_logs_user_id ON audit_logs(user_id);
CREATE INDEX idx_audit_logs_entity ON audit_logs(entity_type, entity_id);
CREATE INDEX idx_audit_logs_action ON audit_logs(action);
CREATE INDEX idx_audit_logs_created_at ON audit_logs(created_at);
```

---

## Entity Relationships

```
tenants
   │
   ├── users (1:N)
   │      └── refresh_tokens (1:N)
   │
   ├── borrowers (1:N)
   │      └── loans (1:N)
   │             ├── repayment_schedules (1:N)
   │             │      └── payment_allocations (1:N)
   │             └── payments (1:N)
   │                    └── payment_allocations (1:N)
   │
   ├── ledger_accounts (1:N)
   │      └── ledger_entries (1:N)
   │
   ├── ledger_transactions (1:N)
   │      └── ledger_entries (1:N)
   │
   ├── notifications (1:N)
   │
   └── audit_logs (1:N)
```

---

## Multi-Tenancy Implementation

The platform implements row-level multi-tenancy using:

1. **Tenant ID Column**: All tenant-specific tables have a `tenant_id` column
2. **Hibernate Filter**: `@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")`
3. **TenantContext**: ThreadLocal storage for current tenant ID
4. **TenantFilter**: Servlet filter that extracts tenant from JWT and sets TenantContext

```java
// Entity annotation
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
public class Borrower extends TenantAwareEntity { }

// Filter definition (on package-info.java)
@FilterDef(name = "tenantFilter", parameters = @ParamDef(name = "tenantId", type = UUID.class))
```

---

## Indexes Strategy

1. **Primary Keys**: UUID with clustered index
2. **Foreign Keys**: Indexed for join performance
3. **Tenant ID**: Indexed on all tenant-scoped tables
4. **Status Fields**: Indexed for filtering
5. **Date Fields**: Indexed for range queries
6. **Unique Constraints**: Compound indexes for business uniqueness
