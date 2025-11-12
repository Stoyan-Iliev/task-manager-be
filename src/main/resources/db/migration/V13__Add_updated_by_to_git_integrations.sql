-- =============================================================================
-- Add updated_by column to git_integrations table
-- Migration: V13__Add_updated_by_to_git_integrations.sql
-- =============================================================================

ALTER TABLE git_integrations
ADD COLUMN updated_by BIGINT;

ALTER TABLE git_integrations
ADD CONSTRAINT fk_git_integration_updater FOREIGN KEY (updated_by)
    REFERENCES users(id);

CREATE INDEX idx_git_integration_updated_by ON git_integrations(updated_by);

COMMENT ON COLUMN git_integrations.updated_by IS 'User who last updated the integration';
