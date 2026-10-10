-- V12__add_device_tokens_and_notification_read.sql
-- Device tokens for FCM push notifications + read tracking for in-app notification center

CREATE TABLE device_tokens (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token VARCHAR(500) NOT NULL,
    platform VARCHAR(20) NOT NULL DEFAULT 'ANDROID',
    is_active BOOLEAN DEFAULT TRUE NOT NULL,
    last_used_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ DEFAULT NOW() NOT NULL,
    updated_at TIMESTAMPTZ DEFAULT NOW(),
    CONSTRAINT uk_device_tokens_token UNIQUE (token)
);

CREATE INDEX idx_device_tokens_user ON device_tokens(user_id) WHERE is_active = TRUE;

-- Read tracking for the borrower notification center
ALTER TABLE notifications ADD COLUMN IF NOT EXISTS read_at TIMESTAMPTZ;

CREATE INDEX IF NOT EXISTS idx_notifications_recipient_unread
    ON notifications(recipient_id, tenant_id) WHERE read_at IS NULL;
