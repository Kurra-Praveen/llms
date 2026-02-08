-- Initial database setup script
-- This runs automatically when the PostgreSQL container starts for the first time

-- Enable UUID extension
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Grant privileges (if needed for different users)
-- GRANT ALL PRIVILEGES ON DATABASE loan_platform TO postgres;

-- Log that initialization is complete
DO $$
BEGIN
    RAISE NOTICE 'Database initialization complete';
END $$;
