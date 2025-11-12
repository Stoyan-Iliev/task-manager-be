-- V14__Sprint_enhancements.sql
-- Add missing columns to sprints table for enhanced sprint management

-- Add completed_at timestamp column
ALTER TABLE sprints ADD COLUMN completed_at TIMESTAMP;

-- Add completed_by user reference
ALTER TABLE sprints ADD COLUMN completed_by INT REFERENCES users(id);

-- Add capacity_hours for sprint capacity planning
ALTER TABLE sprints ADD COLUMN capacity_hours DECIMAL(10,2);

-- Add index for completed sprints queries
CREATE INDEX idx_sprint_completed ON sprints(project_id, completed_at);

-- Add check constraint to ensure completed_at is set when status is COMPLETED
-- Note: This is a soft constraint - we allow NULL for backwards compatibility
-- but enforce in application logic
ALTER TABLE sprints ADD CONSTRAINT check_completed_at_consistency
CHECK (
  (status = 'COMPLETED' AND completed_at IS NOT NULL) OR
  (status != 'COMPLETED')
);

-- Comments for documentation
COMMENT ON COLUMN sprints.completed_at IS 'Timestamp when sprint was marked as completed';
COMMENT ON COLUMN sprints.completed_by IS 'User who completed the sprint';
COMMENT ON COLUMN sprints.capacity_hours IS 'Total capacity hours available for the sprint';
