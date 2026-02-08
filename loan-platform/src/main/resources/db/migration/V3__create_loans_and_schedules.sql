-- V3__create_loans_and_schedules.sql
-- Core loan management tables

-- Loan products/types configuration
CREATE TABLE loan_products (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    product_code VARCHAR(50) NOT NULL,
    product_name VARCHAR(255) NOT NULL,
    description TEXT,
    interest_type VARCHAR(30) NOT NULL,
    default_interest_rate NUMERIC(8,4) NOT NULL,
    min_interest_rate NUMERIC(8,4),
    max_interest_rate NUMERIC(8,4),
    min_principal NUMERIC(18,2) NOT NULL,
    max_principal NUMERIC(18,2) NOT NULL,
    min_tenure_months INTEGER NOT NULL,
    max_tenure_months INTEGER NOT NULL,
    repayment_frequency VARCHAR(20) NOT NULL DEFAULT 'MONTHLY',
    processing_fee_type VARCHAR(20) DEFAULT 'PERCENTAGE',
    processing_fee_value NUMERIC(8,4) DEFAULT 0,
    penalty_type VARCHAR(20) DEFAULT 'PERCENTAGE',
    penalty_rate NUMERIC(8,4) DEFAULT 0,
    grace_period_days INTEGER DEFAULT 0,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMPTZ DEFAULT NOW() NOT NULL,
    updated_at TIMESTAMPTZ DEFAULT NOW(),
    version BIGINT DEFAULT 0 NOT NULL,
    CONSTRAINT uk_loan_products_tenant_code UNIQUE (tenant_id, product_code)
);

CREATE INDEX idx_loan_products_tenant ON loan_products(tenant_id) WHERE is_active = TRUE;

-- Main loans table
CREATE TABLE loans (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    borrower_id UUID NOT NULL REFERENCES borrowers(id),
    loan_product_id UUID REFERENCES loan_products(id),
    loan_number VARCHAR(50) NOT NULL,

    -- Loan terms
    principal_amount NUMERIC(18,2) NOT NULL,
    interest_rate NUMERIC(8,4) NOT NULL,
    interest_type VARCHAR(30) NOT NULL,
    tenure_months INTEGER NOT NULL,
    repayment_frequency VARCHAR(20) NOT NULL DEFAULT 'MONTHLY',

    -- Fees and charges
    processing_fee NUMERIC(18,2) DEFAULT 0,
    other_charges NUMERIC(18,2) DEFAULT 0,

    -- Dates
    application_date DATE NOT NULL,
    approval_date DATE,
    disbursement_date DATE,
    first_payment_date DATE,
    maturity_date DATE,
    closure_date DATE,

    -- Calculated fields (updated on payments)
    emi_amount NUMERIC(18,2),
    total_interest NUMERIC(18,2),
    total_payable NUMERIC(18,2),
    outstanding_principal NUMERIC(18,2) DEFAULT 0,
    outstanding_interest NUMERIC(18,2) DEFAULT 0,
    outstanding_penalty NUMERIC(18,2) DEFAULT 0,
    total_paid NUMERIC(18,2) DEFAULT 0,
    principal_paid NUMERIC(18,2) DEFAULT 0,
    interest_paid NUMERIC(18,2) DEFAULT 0,
    penalty_paid NUMERIC(18,2) DEFAULT 0,

    -- Status tracking
    status VARCHAR(30) NOT NULL DEFAULT 'DRAFT',
    dpd INTEGER DEFAULT 0,
    dpd_bucket VARCHAR(20),
    is_npa BOOLEAN DEFAULT FALSE,
    npa_date DATE,

    -- Approval workflow
    requires_approval BOOLEAN DEFAULT FALSE,
    approved_by UUID,
    approval_notes TEXT,

    -- Collateral (optional)
    has_collateral BOOLEAN DEFAULT FALSE,
    collateral_type VARCHAR(100),
    collateral_value NUMERIC(18,2),
    collateral_description TEXT,

    -- Penalty configuration (can override product)
    penalty_type VARCHAR(20),
    penalty_rate NUMERIC(8,4),
    grace_period_days INTEGER DEFAULT 0,

    notes TEXT,
    metadata JSONB DEFAULT '{}',

    created_at TIMESTAMPTZ DEFAULT NOW() NOT NULL,
    updated_at TIMESTAMPTZ DEFAULT NOW(),
    version BIGINT DEFAULT 0 NOT NULL,
    created_by UUID,
    updated_by UUID,
    is_deleted BOOLEAN DEFAULT FALSE,
    deleted_at TIMESTAMPTZ,
    deleted_by UUID,

    CONSTRAINT uk_loans_tenant_number UNIQUE (tenant_id, loan_number)
);

CREATE INDEX idx_loans_tenant ON loans(tenant_id) WHERE is_deleted = FALSE;
CREATE INDEX idx_loans_borrower ON loans(borrower_id);
CREATE INDEX idx_loans_status ON loans(tenant_id, status);
CREATE INDEX idx_loans_dpd ON loans(tenant_id, dpd) WHERE status = 'ACTIVE';
CREATE INDEX idx_loans_disbursement ON loans(tenant_id, disbursement_date);
CREATE INDEX idx_loans_maturity ON loans(tenant_id, maturity_date);

-- Repayment schedules (immutable once loan is disbursed)
CREATE TABLE repayment_schedules (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    loan_id UUID NOT NULL REFERENCES loans(id) ON DELETE CASCADE,
    installment_number INTEGER NOT NULL,
    due_date DATE NOT NULL,

    -- Scheduled amounts
    principal_component NUMERIC(18,2) NOT NULL,
    interest_component NUMERIC(18,2) NOT NULL,
    installment_amount NUMERIC(18,2) NOT NULL,
    outstanding_after NUMERIC(18,2) NOT NULL,

    -- Actual payments applied
    principal_paid NUMERIC(18,2) DEFAULT 0,
    interest_paid NUMERIC(18,2) DEFAULT 0,
    penalty_paid NUMERIC(18,2) DEFAULT 0,
    total_paid NUMERIC(18,2) DEFAULT 0,

    -- Penalty tracking
    penalty_amount NUMERIC(18,2) DEFAULT 0,
    penalty_calculated_at TIMESTAMPTZ,

    -- Status
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    paid_date DATE,
    days_past_due INTEGER DEFAULT 0,

    created_at TIMESTAMPTZ DEFAULT NOW() NOT NULL,
    updated_at TIMESTAMPTZ DEFAULT NOW(),
    version BIGINT DEFAULT 0 NOT NULL,

    CONSTRAINT uk_schedule_loan_installment UNIQUE (loan_id, installment_number)
);

CREATE INDEX idx_schedules_loan ON repayment_schedules(loan_id);
CREATE INDEX idx_schedules_tenant_due ON repayment_schedules(tenant_id, due_date);
CREATE INDEX idx_schedules_status ON repayment_schedules(tenant_id, status) WHERE status IN ('PENDING', 'OVERDUE', 'PARTIAL');
CREATE INDEX idx_schedules_overdue ON repayment_schedules(loan_id) WHERE status = 'OVERDUE';

COMMENT ON TABLE loan_products IS 'Loan product templates with default terms';
COMMENT ON TABLE loans IS 'Individual loans issued to borrowers';
COMMENT ON TABLE repayment_schedules IS 'Amortization schedule - immutable once disbursed';
