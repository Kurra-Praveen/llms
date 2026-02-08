-- V6__create_collections_and_notifications.sql
-- Collections management and notification system

-- Collections/dunning tracking
CREATE TABLE collection_cases (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    loan_id UUID NOT NULL REFERENCES loans(id),
    case_number VARCHAR(50) NOT NULL,

    -- DPD tracking
    dpd_bucket VARCHAR(20) NOT NULL,
    dpd_days INTEGER NOT NULL,
    overdue_amount NUMERIC(18,2) NOT NULL,
    overdue_principal NUMERIC(18,2) DEFAULT 0,
    overdue_interest NUMERIC(18,2) DEFAULT 0,
    overdue_penalty NUMERIC(18,2) DEFAULT 0,

    -- Assignment
    assigned_to UUID REFERENCES users(id),
    assigned_at TIMESTAMPTZ,

    -- Status
    status VARCHAR(30) NOT NULL DEFAULT 'OPEN',
    priority VARCHAR(20) DEFAULT 'MEDIUM',

    -- Resolution
    resolution_type VARCHAR(30),
    resolved_at TIMESTAMPTZ,
    resolved_by UUID,
    resolution_notes TEXT,

    -- Promise to pay
    ptp_date DATE,
    ptp_amount NUMERIC(18,2),
    ptp_status VARCHAR(20),

    last_contact_date DATE,
    next_action_date DATE,
    next_action TEXT,

    created_at TIMESTAMPTZ DEFAULT NOW() NOT NULL,
    updated_at TIMESTAMPTZ DEFAULT NOW(),
    version BIGINT DEFAULT 0 NOT NULL,

    CONSTRAINT uk_collection_case_tenant UNIQUE (tenant_id, case_number)
);

CREATE INDEX idx_collection_cases_loan ON collection_cases(loan_id);
CREATE INDEX idx_collection_cases_tenant ON collection_cases(tenant_id, status);
CREATE INDEX idx_collection_cases_bucket ON collection_cases(tenant_id, dpd_bucket) WHERE status = 'OPEN';
CREATE INDEX idx_collection_cases_assigned ON collection_cases(assigned_to, status);

-- Collection activity log
CREATE TABLE collection_activities (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    collection_case_id UUID NOT NULL REFERENCES collection_cases(id) ON DELETE CASCADE,
    activity_type VARCHAR(50) NOT NULL,
    activity_date TIMESTAMPTZ DEFAULT NOW(),
    contact_method VARCHAR(30),
    contact_result VARCHAR(50),
    notes TEXT,
    ptp_date DATE,
    ptp_amount NUMERIC(18,2),
    next_action_date DATE,
    next_action TEXT,
    created_by UUID NOT NULL,
    created_at TIMESTAMPTZ DEFAULT NOW() NOT NULL
);

CREATE INDEX idx_collection_activities_case ON collection_activities(collection_case_id, activity_date DESC);

-- Notification templates
CREATE TABLE notification_templates (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id UUID REFERENCES tenants(id),
    template_code VARCHAR(50) NOT NULL,
    template_name VARCHAR(255) NOT NULL,
    channel VARCHAR(20) NOT NULL,
    subject VARCHAR(255),
    body_template TEXT NOT NULL,
    variables JSONB DEFAULT '[]',
    is_system_template BOOLEAN DEFAULT FALSE,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMPTZ DEFAULT NOW() NOT NULL,
    updated_at TIMESTAMPTZ DEFAULT NOW(),
    version BIGINT DEFAULT 0 NOT NULL,
    CONSTRAINT uk_notification_template UNIQUE (tenant_id, template_code)
);

CREATE INDEX idx_notification_templates_tenant ON notification_templates(tenant_id) WHERE is_active = TRUE;

-- Notifications sent
CREATE TABLE notifications (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    template_id UUID REFERENCES notification_templates(id),

    -- Target
    recipient_type VARCHAR(20) NOT NULL,
    recipient_id UUID,
    recipient_contact VARCHAR(255) NOT NULL,

    -- Content
    channel VARCHAR(20) NOT NULL,
    subject VARCHAR(255),
    content TEXT NOT NULL,

    -- Reference
    reference_type VARCHAR(50),
    reference_id UUID,
    loan_id UUID REFERENCES loans(id),

    -- Status
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    sent_at TIMESTAMPTZ,
    delivered_at TIMESTAMPTZ,
    failed_at TIMESTAMPTZ,
    failure_reason TEXT,
    retry_count INTEGER DEFAULT 0,

    -- External provider tracking
    external_id VARCHAR(255),
    provider_response JSONB,

    scheduled_for TIMESTAMPTZ,
    created_at TIMESTAMPTZ DEFAULT NOW() NOT NULL,
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX idx_notifications_tenant ON notifications(tenant_id, created_at DESC);
CREATE INDEX idx_notifications_status ON notifications(status, scheduled_for) WHERE status = 'PENDING';
CREATE INDEX idx_notifications_loan ON notifications(loan_id) WHERE loan_id IS NOT NULL;
CREATE INDEX idx_notifications_recipient ON notifications(recipient_id, channel);

-- Notification rules/triggers
CREATE TABLE notification_rules (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    rule_name VARCHAR(255) NOT NULL,
    trigger_event VARCHAR(50) NOT NULL,
    template_id UUID NOT NULL REFERENCES notification_templates(id),
    conditions JSONB DEFAULT '{}',
    timing_config JSONB DEFAULT '{}',
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMPTZ DEFAULT NOW() NOT NULL,
    updated_at TIMESTAMPTZ DEFAULT NOW(),
    version BIGINT DEFAULT 0 NOT NULL
);

CREATE INDEX idx_notification_rules_tenant ON notification_rules(tenant_id, trigger_event) WHERE is_active = TRUE;

COMMENT ON TABLE collection_cases IS 'Dunning cases for overdue loans';
COMMENT ON TABLE collection_activities IS 'Collection follow-up activity log';
COMMENT ON TABLE notifications IS 'All outbound notifications (SMS, Email, etc)';
COMMENT ON TABLE notification_rules IS 'Automated notification trigger rules';
