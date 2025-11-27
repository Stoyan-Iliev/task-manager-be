-- ============================================================================
-- V23: Simplify activity_log - Remove partitions, use single table
-- ============================================================================
-- Purpose: Remove monthly partitioning complexity, add WORK_LOG support
-- ============================================================================

-- Drop the activity_feed view first (depends on activity_log)
DROP VIEW IF EXISTS activity_feed;

-- Drop the version function
DROP FUNCTION IF EXISTS get_next_version_number(VARCHAR(50), BIGINT);

-- Drop all partition tables and the parent partitioned table
DROP TABLE IF EXISTS activity_log CASCADE;

-- Recreate as a simple non-partitioned table with updated constraints
CREATE TABLE activity_log (
    id BIGSERIAL PRIMARY KEY,

    -- Multi-level context for scoping
    organization_id BIGINT NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    project_id BIGINT REFERENCES projects(id) ON DELETE CASCADE,
    task_id BIGINT REFERENCES tasks(id) ON DELETE CASCADE,

    -- What changed
    entity_type VARCHAR(50) NOT NULL,
    entity_id BIGINT NOT NULL,
    action VARCHAR(50) NOT NULL,

    -- Who made the change
    user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE RESTRICT,

    -- Field-level tracking (for UPDATE actions)
    field_name VARCHAR(100),
    old_value JSONB,
    new_value JSONB,

    -- Additional context
    metadata JSONB,

    -- Versioning for time-travel
    version_number INTEGER NOT NULL DEFAULT 1,

    -- When the change happened
    timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Action type validation (including work log actions)
    CONSTRAINT valid_action CHECK (action IN (
        'CREATED', 'UPDATED', 'DELETED',
        'STATUS_CHANGED', 'ASSIGNED', 'UNASSIGNED',
        'COMMENT_ADDED', 'COMMENT_EDITED', 'COMMENT_DELETED',
        'ATTACHMENT_ADDED', 'ATTACHMENT_DELETED',
        'LABEL_ADDED', 'LABEL_REMOVED',
        'SPRINT_ADDED', 'SPRINT_REMOVED',
        'WATCHER_ADDED', 'WATCHER_REMOVED',
        'WORK_LOGGED', 'WORK_LOG_UPDATED', 'WORK_LOG_DELETED'
    )),

    -- Entity type validation (including work log)
    CONSTRAINT valid_entity_type CHECK (entity_type IN (
        'ORGANIZATION', 'PROJECT', 'TASK', 'COMMENT', 'ATTACHMENT',
        'LABEL', 'SPRINT', 'PROJECT_MEMBER', 'ORGANIZATION_MEMBER', 'TASK_WATCHER',
        'WORK_LOG'
    ))
);

-- Indexes for common queries
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

-- Recreate the version number function
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

-- Recreate the activity feed view
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
    TO_CHAR(al.timestamp, 'YYYY-MM-DD HH24:MI:SS') AS timestamp_formatted,
    AGE(CURRENT_TIMESTAMP, al.timestamp) AS time_ago
FROM activity_log al
JOIN users u ON al.user_id = u.id
ORDER BY al.timestamp DESC;

-- Comments
COMMENT ON TABLE activity_log IS 'Comprehensive audit trail with versioning. Supports work log tracking.';
COMMENT ON FUNCTION get_next_version_number IS 'Returns next version number for entity versioning';
COMMENT ON VIEW activity_feed IS 'Human-readable view of activity log with user details';
