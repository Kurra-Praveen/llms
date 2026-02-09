-- V11__add_missing_collection_activity_columns.sql
-- Add missing columns for BaseEntity compatibility in collection_activities table

ALTER TABLE collection_activities
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ DEFAULT NOW(),
    ADD COLUMN IF NOT EXISTS version BIGINT DEFAULT 0 NOT NULL;
