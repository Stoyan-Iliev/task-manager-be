-- V25__Add_project_type.sql
-- Add project type column to projects table

ALTER TABLE projects
ADD COLUMN type VARCHAR(20) NOT NULL DEFAULT 'SOFTWARE';

-- Add check constraint for valid project types
ALTER TABLE projects
ADD CONSTRAINT check_project_type CHECK (type IN ('SOFTWARE', 'MARKETING', 'BUSINESS', 'OPERATIONS'));

-- Create index for filtering by type
CREATE INDEX idx_project_type ON projects(type);
