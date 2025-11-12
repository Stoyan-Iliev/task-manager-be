-- ============================================================================
-- V10: Activity Log with Versioning and Time-Travel
-- ============================================================================
-- Purpose: Comprehensive audit trail tracking ALL changes with version history
-- Features:
--   - Track entity changes at field level (old/new values)
--   - Version numbering for state reconstruction
--   - JSONB for flexible value storage
--   - Monthly partitioning for performance
--   - Multi-level context (org → project → task)
--   - Support for time-travel queries
-- ============================================================================

-- Main activity log table (partitioned by timestamp)
CREATE TABLE activity_log (
    id BIGSERIAL NOT NULL,

    -- Multi-level context for scoping
    organization_id BIGINT NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    project_id BIGINT REFERENCES projects(id) ON DELETE CASCADE,
    task_id BIGINT REFERENCES tasks(id) ON DELETE CASCADE,

    -- What changed
    entity_type VARCHAR(50) NOT NULL,  -- TASK, COMMENT, ATTACHMENT, LABEL, etc.
    entity_id BIGINT NOT NULL,
    action VARCHAR(50) NOT NULL,       -- CREATED, UPDATED, DELETED, STATUS_CHANGED, etc.

    -- Who made the change
    user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE RESTRICT,

    -- Field-level tracking (for UPDATE actions)
    field_name VARCHAR(100),           -- e.g., 'status', 'assignee', 'title'
    old_value JSONB,                   -- Previous value
    new_value JSONB,                   -- New value

    -- Additional context
    metadata JSONB,                    -- Extra info (e.g., {"comment": "Updated via API"})

    -- Versioning for time-travel
    version_number INTEGER NOT NULL DEFAULT 1,

    -- When the change happened
    timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Action type validation
    CONSTRAINT valid_action CHECK (action IN (
        'CREATED', 'UPDATED', 'DELETED',
        'STATUS_CHANGED', 'ASSIGNED', 'UNASSIGNED',
        'COMMENT_ADDED', 'COMMENT_EDITED', 'COMMENT_DELETED',
        'ATTACHMENT_ADDED', 'ATTACHMENT_DELETED',
        'LABEL_ADDED', 'LABEL_REMOVED',
        'SPRINT_ADDED', 'SPRINT_REMOVED',
        'WATCHER_ADDED', 'WATCHER_REMOVED'
    )),

    -- Entity type validation
    CONSTRAINT valid_entity_type CHECK (entity_type IN (
        'ORGANIZATION', 'PROJECT', 'TASK', 'COMMENT', 'ATTACHMENT',
        'LABEL', 'SPRINT', 'PROJECT_MEMBER', 'ORGANIZATION_MEMBER', 'TASK_WATCHER'
    ))
) PARTITION BY RANGE (timestamp);

-- Create monthly partitions for 2025-2026
CREATE TABLE activity_log_2025_10 PARTITION OF activity_log
    FOR VALUES FROM ('2025-10-01') TO ('2025-11-01');

CREATE TABLE activity_log_2025_11 PARTITION OF activity_log
    FOR VALUES FROM ('2025-11-01') TO ('2025-12-01');

CREATE TABLE activity_log_2025_12 PARTITION OF activity_log
    FOR VALUES FROM ('2025-12-01') TO ('2026-01-01');

CREATE TABLE activity_log_2026_01 PARTITION OF activity_log
    FOR VALUES FROM ('2026-01-01') TO ('2026-02-01');

CREATE TABLE activity_log_2026_02 PARTITION OF activity_log
    FOR VALUES FROM ('2026-02-01') TO ('2026-03-01');

CREATE TABLE activity_log_2026_03 PARTITION OF activity_log
    FOR VALUES FROM ('2026-03-01') TO ('2026-04-01');

-- Indexes (created on each partition automatically)
CREATE INDEX idx_activity_task_ts ON activity_log(task_id, timestamp DESC);
CREATE INDEX idx_activity_project_ts ON activity_log(project_id, timestamp DESC) WHERE project_id IS NOT NULL;
CREATE INDEX idx_activity_org_ts ON activity_log(organization_id, timestamp DESC);
CREATE INDEX idx_activity_entity ON activity_log(entity_type, entity_id, timestamp DESC);
CREATE INDEX idx_activity_user ON activity_log(user_id, timestamp DESC);
CREATE INDEX idx_activity_action ON activity_log(action, timestamp DESC);
CREATE INDEX idx_activity_version ON activity_log(entity_type, entity_id, version_number);

-- JSONB GIN indexes for flexible querying
CREATE INDEX idx_activity_old_value_gin ON activity_log USING GIN (old_value) WHERE old_value IS NOT NULL;
CREATE INDEX idx_activity_new_value_gin ON activity_log USING GIN (new_value) WHERE new_value IS NOT NULL;
CREATE INDEX idx_activity_metadata_gin ON activity_log USING GIN (metadata) WHERE metadata IS NOT NULL;

-- Comments for documentation
COMMENT ON TABLE activity_log IS 'Comprehensive audit trail with versioning. Supports time-travel to reconstruct entity state at any version.';
COMMENT ON COLUMN activity_log.entity_type IS 'Type of entity that changed (TASK, COMMENT, etc.)';
COMMENT ON COLUMN activity_log.entity_id IS 'ID of the entity that changed';
COMMENT ON COLUMN activity_log.action IS 'Type of action performed (CREATED, UPDATED, etc.)';
COMMENT ON COLUMN activity_log.field_name IS 'For UPDATE actions: which field changed (e.g., "status", "assignee")';
COMMENT ON COLUMN activity_log.old_value IS 'Previous value in JSONB format';
COMMENT ON COLUMN activity_log.new_value IS 'New value in JSONB format';
COMMENT ON COLUMN activity_log.version_number IS 'Incremental version for entity state tracking (enables time-travel)';
COMMENT ON COLUMN activity_log.metadata IS 'Additional context (e.g., transition comment, bulk operation ID)';

-- Function to get next version number for an entity
CREATE OR REPLACE FUNCTION get_next_version_number(
    p_entity_type VARCHAR(50),
    p_entity_id BIGINT
) RETURNS INTEGER AS $$
DECLARE
    max_version INTEGER;
BEGIN
    SELECT COALESCE(MAX(version_number), 0) + 1
    INTO max_version
    FROM activity_log
    WHERE entity_type = p_entity_type
      AND entity_id = p_entity_id;

    RETURN max_version;
END;
$$ LANGUAGE plpgsql;

COMMENT ON FUNCTION get_next_version_number IS 'Returns next version number for entity versioning';

-- View for human-readable activity feed
CREATE VIEW activity_feed AS
SELECT
    al.id,
    al.organization_id,
    al.project_id,
    al.task_id,
    al.entity_type,
    al.entity_id,
    al.action,
    u.username AS performed_by,
    u.email AS performed_by_email,
    al.field_name,
    al.old_value,
    al.new_value,
    al.metadata,
    al.version_number,
    al.timestamp,
    -- Human-readable timestamp
    TO_CHAR(al.timestamp, 'YYYY-MM-DD HH24:MI:SS') AS timestamp_formatted,
    -- Relative time (PostgreSQL)
    AGE(CURRENT_TIMESTAMP, al.timestamp) AS time_ago
FROM activity_log al
JOIN users u ON al.user_id = u.id
ORDER BY al.timestamp DESC;

COMMENT ON VIEW activity_feed IS 'Human-readable view of activity log with user details and formatted timestamps';
