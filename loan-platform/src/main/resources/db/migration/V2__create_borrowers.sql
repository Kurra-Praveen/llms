-- V2__create_borrowers.sql
-- Borrower management tables

CREATE TABLE borrowers (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    borrower_code VARCHAR(50) NOT NULL,
    full_name VARCHAR(255) NOT NULL,
    date_of_birth DATE,
    gender VARCHAR(10),
    phone VARCHAR(20) NOT NULL,
    alternate_phone VARCHAR(20),
    email VARCHAR(255),
    id_type VARCHAR(50),
    id_number VARCHAR(100),
    address_line1 VARCHAR(255),
    address_line2 VARCHAR(255),
    city VARCHAR(100),
    state VARCHAR(100),
    postal_code VARCHAR(20),
    country VARCHAR(100) DEFAULT 'India',
    occupation VARCHAR(100),
    employer_name VARCHAR(255),
    monthly_income NUMERIC(18,2),
    credit_rating VARCHAR(20) DEFAULT 'UNRATED',
    risk_score INTEGER,
    risk_band VARCHAR(20),
    status VARCHAR(20) DEFAULT 'ACTIVE' NOT NULL,
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
    CONSTRAINT uk_borrowers_tenant_code UNIQUE (tenant_id, borrower_code)
);

CREATE INDEX idx_borrowers_tenant ON borrowers(tenant_id) WHERE is_deleted = FALSE;
CREATE INDEX idx_borrowers_phone ON borrowers(tenant_id, phone);
CREATE INDEX idx_borrowers_id_number ON borrowers(tenant_id, id_number) WHERE id_number IS NOT NULL;
CREATE INDEX idx_borrowers_status ON borrowers(tenant_id, status);
CREATE INDEX idx_borrowers_risk ON borrowers(tenant_id, risk_band);

-- Borrower documents
CREATE TABLE borrower_documents (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    borrower_id UUID NOT NULL REFERENCES borrowers(id) ON DELETE CASCADE,
    document_type VARCHAR(50) NOT NULL,
    document_name VARCHAR(255) NOT NULL,
    file_path VARCHAR(500) NOT NULL,
    file_size BIGINT,
    mime_type VARCHAR(100),
    verified BOOLEAN DEFAULT FALSE,
    verified_by UUID,
    verified_at TIMESTAMPTZ,
    notes TEXT,
    created_at TIMESTAMPTZ DEFAULT NOW() NOT NULL,
    updated_at TIMESTAMPTZ DEFAULT NOW(),
    version BIGINT DEFAULT 0 NOT NULL
);

CREATE INDEX idx_borrower_docs_borrower ON borrower_documents(borrower_id);
CREATE INDEX idx_borrower_docs_tenant ON borrower_documents(tenant_id);

-- Borrower communication log
CREATE TABLE borrower_communications (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    borrower_id UUID NOT NULL REFERENCES borrowers(id),
    communication_type VARCHAR(20) NOT NULL,
    direction VARCHAR(10) NOT NULL,
    subject VARCHAR(255),
    content TEXT,
    status VARCHAR(20) DEFAULT 'SENT',
    sent_at TIMESTAMPTZ,
    created_by UUID,
    created_at TIMESTAMPTZ DEFAULT NOW() NOT NULL
);

CREATE INDEX idx_borrower_comms_borrower ON borrower_communications(borrower_id, created_at DESC);

COMMENT ON TABLE borrowers IS 'Loan borrowers/customers per tenant';
COMMENT ON TABLE borrower_documents IS 'KYC and other documents uploaded for borrowers';
COMMENT ON TABLE borrower_communications IS 'Communication history with borrowers';
