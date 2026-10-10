-- V11__link_users_to_borrowers.sql
-- Link user accounts to borrower records for borrower self-service (mobile app portal)

ALTER TABLE users ADD COLUMN IF NOT EXISTS borrower_id UUID REFERENCES borrowers(id);

-- One borrower = at most one login account (partial unique index allows multiple NULLs for staff users)
CREATE UNIQUE INDEX IF NOT EXISTS uk_users_borrower_id ON users(borrower_id) WHERE borrower_id IS NOT NULL;

COMMENT ON COLUMN users.borrower_id IS 'Links a BORROWER-role user account to their borrower record for self-service portal access';
