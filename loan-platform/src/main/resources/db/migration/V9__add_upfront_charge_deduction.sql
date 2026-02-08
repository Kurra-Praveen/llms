-- V9: Add upfront charge deduction fields to loans table
-- This migration adds support for deducting charges upfront from loan disbursement

ALTER TABLE loans ADD COLUMN IF NOT EXISTS deduct_charges_upfront BOOLEAN DEFAULT true;
ALTER TABLE loans ADD COLUMN IF NOT EXISTS total_charges_deducted DECIMAL(18,2) DEFAULT 0;
ALTER TABLE loans ADD COLUMN IF NOT EXISTS net_disbursement_amount DECIMAL(18,2);

-- Update existing disbursed loans to calculate net disbursement amount
UPDATE loans
SET total_charges_deducted = COALESCE(processing_fee, 0) + COALESCE(other_charges, 0),
    net_disbursement_amount = principal_amount - (COALESCE(processing_fee, 0) + COALESCE(other_charges, 0))
WHERE status IN ('ACTIVE', 'DISBURSED', 'CLOSED')
  AND disbursement_date IS NOT NULL;

-- For loans not yet disbursed, set net_disbursement_amount to null (will be calculated at disbursement)
UPDATE loans
SET net_disbursement_amount = NULL,
    total_charges_deducted = 0
WHERE status NOT IN ('ACTIVE', 'DISBURSED', 'CLOSED')
   OR disbursement_date IS NULL;

COMMENT ON COLUMN loans.deduct_charges_upfront IS 'If true, processing fee and other charges are deducted from disbursement amount';
COMMENT ON COLUMN loans.total_charges_deducted IS 'Total charges deducted upfront from disbursement';
COMMENT ON COLUMN loans.net_disbursement_amount IS 'Actual amount disbursed to borrower after deducting charges';
