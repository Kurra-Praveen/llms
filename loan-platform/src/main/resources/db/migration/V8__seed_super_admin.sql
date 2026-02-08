-- V8__seed_super_admin.sql
-- Create System Tenant and Super Admin User

DO $$
DECLARE
    v_tenant_id UUID;
    v_user_id UUID;
BEGIN
    -- Check if tenant already exists (idempotency)
    SELECT id INTO v_tenant_id FROM tenants WHERE business_code = 'SYS-ADMIN';

    IF v_tenant_id IS NULL THEN
        -- 1. Create System Tenant
        INSERT INTO tenants (
            business_name,
            business_code,
            contact_email,
            contact_phone,
            address,
            status,
            subscription_plan,
            max_borrowers,
            max_loans
        ) VALUES (
            'System Administrator',
            'SYS-ADMIN',
            'admin@loanplatform.com',
            '0000000000',
            'System Headquarters',
            'ACTIVE',
            'ENTERPRISE',
            1000000,
            1000000
        ) RETURNING id INTO v_tenant_id;
    END IF;

    -- Check if user already exists
    SELECT id INTO v_user_id FROM users WHERE email = 'admin@loanplatform.com';

    IF v_user_id IS NULL THEN
        -- 2. Create Super Admin User
        INSERT INTO users (
            tenant_id,
            email,
            password_hash,
            first_name,
            last_name,
            phone,
            role,
            status,
            email_verified,
            created_by
        ) VALUES (
            v_tenant_id,
            'admin@loanplatform.com',
            '$2a$10$6kCiP.LMq./4QX60GSUFu.z9T/c.aNxKQ7cn7mjYOQC6PhzU/XkNG', -- admin123
            'Super',
            'Admin',
            '0000000000',
            'SUPER_ADMIN',
            'ACTIVE',
            TRUE,
            NULL
        );
    END IF;
END $$;
