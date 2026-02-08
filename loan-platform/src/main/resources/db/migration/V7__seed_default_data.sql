-- V7__seed_default_data.sql
-- Seed essential system data

-- Insert default ledger account types for new tenants
-- This will be copied when a new tenant is created

-- Create a system function to seed default ledger accounts for a new tenant
-- CREATE OR REPLACE FUNCTION seed_tenant_ledger_accounts(p_tenant_id UUID)
-- RETURNS VOID AS $$
-- BEGIN
--     -- Asset accounts
--     INSERT INTO ledger_accounts (tenant_id, account_code, account_name, account_type, is_system_account)
--     VALUES
--         (p_tenant_id, '1000', 'Cash and Bank', 'ASSET', TRUE),
--         (p_tenant_id, '1100', 'Loan Principal Outstanding', 'ASSET', TRUE),
--         (p_tenant_id, '1200', 'Interest Receivable', 'ASSET', TRUE),
--         (p_tenant_id, '1300', 'Penalty Receivable', 'ASSET', TRUE),
--         (p_tenant_id, '1400', 'Processing Fee Receivable', 'ASSET', TRUE);

--     -- Liability accounts
--     INSERT INTO ledger_accounts (tenant_id, account_code, account_name, account_type, is_system_account)
--     VALUES
--         (p_tenant_id, '2000', 'Advance Received', 'LIABILITY', TRUE),
--         (p_tenant_id, '2100', 'Security Deposit', 'LIABILITY', TRUE);

--     -- Income accounts
--     INSERT INTO ledger_accounts (tenant_id, account_code, account_name, account_type, is_system_account)
--     VALUES
--         (p_tenant_id, '4000', 'Interest Income', 'INCOME', TRUE),
--         (p_tenant_id, '4100', 'Penalty Income', 'INCOME', TRUE),
--         (p_tenant_id, '4200', 'Processing Fee Income', 'INCOME', TRUE),
--         (p_tenant_id, '4300', 'Other Income', 'INCOME', TRUE);

--     -- Expense accounts
--     INSERT INTO ledger_accounts (tenant_id, account_code, account_name, account_type, is_system_account)
--     VALUES
--         (p_tenant_id, '5000', 'Bad Debt Expense', 'EXPENSE', TRUE),
--         (p_tenant_id, '5100', 'Waiver Expense', 'EXPENSE', TRUE);
-- END;
-- $$ LANGUAGE plpgsql;

-- -- Create trigger to automatically seed ledger accounts for new tenants
-- CREATE OR REPLACE FUNCTION trigger_seed_tenant_accounts()
-- RETURNS TRIGGER AS $$
-- BEGIN
--     PERFORM seed_tenant_ledger_accounts(NEW.id);
--     RETURN NEW;
-- END;
-- $$ LANGUAGE plpgsql;

-- CREATE TRIGGER trg_tenant_after_insert
-- AFTER INSERT ON tenants
-- FOR EACH ROW
-- EXECUTE FUNCTION trigger_seed_tenant_accounts();

-- -- Insert system notification templates (tenant_id = NULL means system-wide)
-- INSERT INTO notification_templates (tenant_id, template_code, template_name, channel, subject, body_template, is_system_template, variables)
-- VALUES
--     (NULL, 'LOAN_DISBURSED', 'Loan Disbursement Notification', 'SMS', NULL,
--      'Dear {{borrower_name}}, your loan {{loan_number}} of {{currency}}{{principal_amount}} has been disbursed. EMI: {{currency}}{{emi_amount}}. First due: {{first_due_date}}.',
--      TRUE, '["borrower_name", "loan_number", "currency", "principal_amount", "emi_amount", "first_due_date"]'),

--     (NULL, 'PAYMENT_RECEIVED', 'Payment Receipt Notification', 'SMS', NULL,
--      'Dear {{borrower_name}}, payment of {{currency}}{{amount}} received for loan {{loan_number}}. Receipt: {{receipt_number}}. Outstanding: {{currency}}{{outstanding}}.',
--      TRUE, '["borrower_name", "currency", "amount", "loan_number", "receipt_number", "outstanding"]'),

--     (NULL, 'PAYMENT_DUE_REMINDER', 'Payment Due Reminder', 'SMS', NULL,
--      'Dear {{borrower_name}}, your EMI of {{currency}}{{emi_amount}} for loan {{loan_number}} is due on {{due_date}}. Please pay on time to avoid penalty.',
--      TRUE, '["borrower_name", "currency", "emi_amount", "loan_number", "due_date"]'),

--     (NULL, 'PAYMENT_OVERDUE', 'Payment Overdue Alert', 'SMS', NULL,
--      'Dear {{borrower_name}}, your EMI of {{currency}}{{emi_amount}} for loan {{loan_number}} is overdue by {{dpd}} days. Please pay immediately to avoid additional penalties.',
--      TRUE, '["borrower_name", "currency", "emi_amount", "loan_number", "dpd"]'),

--     (NULL, 'LOAN_CLOSED', 'Loan Closure Notification', 'SMS', NULL,
--      'Dear {{borrower_name}}, congratulations! Your loan {{loan_number}} has been fully repaid and closed. Thank you for your business.',
--      TRUE, '["borrower_name", "loan_number"]'),

--     (NULL, 'LOAN_DISBURSED_EMAIL', 'Loan Disbursement Email', 'EMAIL', 'Your Loan {{loan_number}} has been Disbursed',
--      E'Dear {{borrower_name}},\n\nWe are pleased to inform you that your loan has been disbursed.\n\nLoan Details:\n- Loan Number: {{loan_number}}\n- Principal Amount: {{currency}}{{principal_amount}}\n- Interest Rate: {{interest_rate}}% per annum\n- Tenure: {{tenure}} months\n- EMI Amount: {{currency}}{{emi_amount}}\n- First Due Date: {{first_due_date}}\n\nPlease ensure timely payments to maintain a good credit history.\n\nRegards,\n{{lender_name}}',
--      TRUE, '["borrower_name", "loan_number", "currency", "principal_amount", "interest_rate", "tenure", "emi_amount", "first_due_date", "lender_name"]');

-- COMMENT ON FUNCTION seed_tenant_ledger_accounts IS 'Seeds default chart of accounts for new tenants';
