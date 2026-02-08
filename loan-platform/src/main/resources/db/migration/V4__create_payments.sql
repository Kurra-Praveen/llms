-- V4__create_payments.sql
-- Payment processing tables

-- Main payments table
CREATE TABLE payments (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    loan_id UUID NOT NULL REFERENCES loans(id),

    -- Payment identification
    payment_number VARCHAR(50) NOT NULL,
    idempotency_key VARCHAR(100),

    -- Payment details
    payment_date DATE NOT NULL,
    payment_time TIMESTAMPTZ DEFAULT NOW(),
    amount_paid NUMERIC(18,2) NOT NULL,
    payment_method VARCHAR(30) NOT NULL,
    reference_number VARCHAR(100),
    transaction_id VARCHAR(100),

    -- Allocation breakdown (computed by engine)
    principal_paid NUMERIC(18,2) DEFAULT 0,
    interest_paid NUMERIC(18,2) DEFAULT 0,
    penalty_paid NUMERIC(18,2) DEFAULT 0,
    fee_paid NUMERIC(18,2) DEFAULT 0,
    excess_amount NUMERIC(18,2) DEFAULT 0,

    -- Receipt
    receipt_number VARCHAR(50),
    receipt_generated BOOLEAN DEFAULT FALSE,

    -- Status
    status VARCHAR(20) NOT NULL DEFAULT 'COMPLETED',

    -- Reversal handling
    is_reversed BOOLEAN DEFAULT FALSE,
    reversed_at TIMESTAMPTZ,
    reversed_by UUID,
    reversal_reason TEXT,
    original_payment_id UUID REFERENCES payments(id),

    notes TEXT,
    metadata JSONB DEFAULT '{}',

    created_at TIMESTAMPTZ DEFAULT NOW() NOT NULL,
    updated_at TIMESTAMPTZ DEFAULT NOW(),
    version BIGINT DEFAULT 0 NOT NULL,
    created_by UUID,
    updated_by UUID,

    CONSTRAINT uk_payments_tenant_number UNIQUE (tenant_id, payment_number),
    CONSTRAINT uk_payments_idempotency UNIQUE (tenant_id, idempotency_key)
);

CREATE INDEX idx_payments_loan ON payments(loan_id, payment_date DESC);
CREATE INDEX idx_payments_tenant_date ON payments(tenant_id, payment_date DESC);
CREATE INDEX idx_payments_status ON payments(tenant_id, status);
CREATE INDEX idx_payments_reference ON payments(tenant_id, reference_number) WHERE reference_number IS NOT NULL;

-- Payment allocation details (tracks which schedule items received payment)
CREATE TABLE payment_allocations (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    payment_id UUID NOT NULL REFERENCES payments(id) ON DELETE CASCADE,
    schedule_id UUID NOT NULL REFERENCES repayment_schedules(id),

    principal_allocated NUMERIC(18,2) DEFAULT 0,
    interest_allocated NUMERIC(18,2) DEFAULT 0,
    penalty_allocated NUMERIC(18,2) DEFAULT 0,
    total_allocated NUMERIC(18,2) NOT NULL,

    created_at TIMESTAMPTZ DEFAULT NOW() NOT NULL
);

CREATE INDEX idx_allocations_payment ON payment_allocations(payment_id);
CREATE INDEX idx_allocations_schedule ON payment_allocations(schedule_id);

-- Pending payment approvals (for reversals, waivers)
CREATE TABLE payment_approval_requests (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    payment_id UUID REFERENCES payments(id),
    loan_id UUID NOT NULL REFERENCES loans(id),
    request_type VARCHAR(30) NOT NULL,
    amount NUMERIC(18,2),
    reason TEXT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    requested_by UUID NOT NULL,
    requested_at TIMESTAMPTZ DEFAULT NOW(),
    reviewed_by UUID,
    reviewed_at TIMESTAMPTZ,
    review_notes TEXT,
    created_at TIMESTAMPTZ DEFAULT NOW() NOT NULL,
    updated_at TIMESTAMPTZ DEFAULT NOW(),
    version BIGINT DEFAULT 0 NOT NULL
);

CREATE INDEX idx_payment_approvals_tenant ON payment_approval_requests(tenant_id, status);
CREATE INDEX idx_payment_approvals_loan ON payment_approval_requests(loan_id);

COMMENT ON TABLE payments IS 'All payment transactions with allocation tracking';
COMMENT ON TABLE payment_allocations IS 'Detailed allocation of payment to schedule items';
COMMENT ON TABLE payment_approval_requests IS 'Approval workflow for reversals and waivers';
