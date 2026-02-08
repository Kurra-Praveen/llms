-- V5__create_ledger.sql
-- Double-entry accounting ledger

-- Chart of accounts
CREATE TABLE ledger_accounts (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    account_code VARCHAR(20) NOT NULL,
    account_name VARCHAR(255) NOT NULL,
    account_type VARCHAR(20) NOT NULL,
    parent_account_id UUID REFERENCES ledger_accounts(id),
    description TEXT,
    is_system_account BOOLEAN DEFAULT FALSE,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMPTZ DEFAULT NOW() NOT NULL,
    updated_at TIMESTAMPTZ DEFAULT NOW(),
    version BIGINT DEFAULT 0 NOT NULL,
    CONSTRAINT uk_ledger_accounts_tenant_code UNIQUE (tenant_id, account_code)
);

CREATE INDEX idx_ledger_accounts_tenant ON ledger_accounts(tenant_id) WHERE is_active = TRUE;
CREATE INDEX idx_ledger_accounts_type ON ledger_accounts(tenant_id, account_type);

-- Ledger transactions (journal entries)
CREATE TABLE ledger_transactions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    transaction_number VARCHAR(50) NOT NULL,
    transaction_date DATE NOT NULL,
    transaction_type VARCHAR(50) NOT NULL,
    description TEXT NOT NULL,

    -- Reference to source transaction
    reference_type VARCHAR(50),
    reference_id UUID,

    -- Loan reference for easy querying
    loan_id UUID REFERENCES loans(id),

    total_debit NUMERIC(18,2) NOT NULL,
    total_credit NUMERIC(18,2) NOT NULL,

    is_reversed BOOLEAN DEFAULT FALSE,
    reversal_of UUID REFERENCES ledger_transactions(id),
    reversed_by UUID REFERENCES ledger_transactions(id),

    posted_by UUID,
    posted_at TIMESTAMPTZ DEFAULT NOW(),

    created_at TIMESTAMPTZ DEFAULT NOW() NOT NULL,

    CONSTRAINT uk_ledger_txn_tenant_number UNIQUE (tenant_id, transaction_number),
    CONSTRAINT ck_ledger_txn_balanced CHECK (total_debit = total_credit)
);

CREATE INDEX idx_ledger_txn_tenant ON ledger_transactions(tenant_id, transaction_date DESC);
CREATE INDEX idx_ledger_txn_loan ON ledger_transactions(loan_id) WHERE loan_id IS NOT NULL;
CREATE INDEX idx_ledger_txn_reference ON ledger_transactions(reference_type, reference_id);

-- Individual ledger entries (double-entry lines)
CREATE TABLE ledger_entries (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    transaction_id UUID NOT NULL REFERENCES ledger_transactions(id) ON DELETE CASCADE,
    account_id UUID NOT NULL REFERENCES ledger_accounts(id),
    entry_type VARCHAR(10) NOT NULL,
    amount NUMERIC(18,2) NOT NULL,
    running_balance NUMERIC(18,2),
    narration TEXT,
    created_at TIMESTAMPTZ DEFAULT NOW() NOT NULL,

    CONSTRAINT ck_entry_type CHECK (entry_type IN ('DEBIT', 'CREDIT')),
    CONSTRAINT ck_amount_positive CHECK (amount >= 0)
);

CREATE INDEX idx_ledger_entries_txn ON ledger_entries(transaction_id);
CREATE INDEX idx_ledger_entries_account ON ledger_entries(account_id, created_at DESC);
CREATE INDEX idx_ledger_entries_tenant ON ledger_entries(tenant_id);

-- Account balances (materialized for performance)
CREATE TABLE account_balances (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    account_id UUID NOT NULL REFERENCES ledger_accounts(id),
    balance_date DATE NOT NULL,
    debit_total NUMERIC(18,2) DEFAULT 0,
    credit_total NUMERIC(18,2) DEFAULT 0,
    closing_balance NUMERIC(18,2) DEFAULT 0,
    last_updated_at TIMESTAMPTZ DEFAULT NOW(),
    CONSTRAINT uk_account_balance_date UNIQUE (account_id, balance_date)
);

CREATE INDEX idx_account_balances_account ON account_balances(account_id, balance_date DESC);

COMMENT ON TABLE ledger_accounts IS 'Chart of accounts for double-entry bookkeeping';
COMMENT ON TABLE ledger_transactions IS 'Journal entries - must always balance';
COMMENT ON TABLE ledger_entries IS 'Individual debit/credit lines';
COMMENT ON TABLE account_balances IS 'Materialized daily account balances';
