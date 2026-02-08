-- V10: Add daily_fixed_amount field to loans table
-- This field stores the fixed rupee amount per ₹100 of principal per day for DAILY_FIXED interest type
-- Example: If daily_fixed_amount = 5 and principal = 1000, then daily interest = (1000/100) × 5 = ₹50/day

ALTER TABLE loans ADD COLUMN IF NOT EXISTS daily_fixed_amount DECIMAL(18,2);

COMMENT ON COLUMN loans.daily_fixed_amount IS 'Fixed rupee amount per ₹100 of principal per day for DAILY_FIXED interest type';
