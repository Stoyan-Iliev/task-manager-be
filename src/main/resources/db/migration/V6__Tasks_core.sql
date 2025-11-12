-- ============================================================================
-- V6: Core Task Management System
-- Description: Creates tasks table with auto-generated keys, watchers, and
--              comprehensive indexes for efficient querying.
-- ============================================================================

-- ============================================================================
-- TASKS TABLE
-- ============================================================================
CREATE TABLE tasks (
    id BIGSERIAL PRIMARY KEY,

    -- Multi-tenancy & relationships
    organization_id BIGINT NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    project_id BIGINT NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    sprint_id BIGINT REFERENCES sprints(id) ON DELETE SET NULL,
    parent_task_id BIGINT REFERENCES tasks(id) ON DELETE CASCADE,

    -- Identity (auto-generated via trigger)
    "key" VARCHAR(20) NOT NULL,  -- "PROJ-123" format

    -- Content
    title VARCHAR(500) NOT NULL,
    description TEXT,  -- Markdown-formatted

    -- Workflow
    status_id BIGINT NOT NULL REFERENCES task_statuses(id) ON DELETE RESTRICT,

    -- People
    assignee_id INTEGER REFERENCES users(id) ON DELETE SET NULL,
    reporter_id INTEGER NOT NULL REFERENCES users(id) ON DELETE RESTRICT,

    -- Classification
    type VARCHAR(20) NOT NULL DEFAULT 'TASK',
    priority VARCHAR(20) NOT NULL DEFAULT 'MEDIUM',

    -- Scheduling
    due_date DATE,
    estimated_hours DECIMAL(10, 2),
    logged_hours DECIMAL(10, 2) DEFAULT 0,
    story_points INTEGER,

    -- Completion tracking
    version_implemented_in BIGINT REFERENCES sprints(id) ON DELETE SET NULL,

    -- Custom fields (extensible, future-proof)
    custom_fields JSONB DEFAULT '{}',

    -- Audit fields
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    created_by INTEGER NOT NULL REFERENCES users(id),
    updated_by INTEGER REFERENCES users(id),

    -- Constraints
    CONSTRAINT unique_task_key UNIQUE (organization_id, "key"),
    CONSTRAINT check_task_type CHECK (type IN ('TASK', 'BUG', 'STORY', 'EPIC')),
    CONSTRAINT check_task_priority CHECK (priority IN ('LOWEST', 'LOW', 'MEDIUM', 'HIGH', 'HIGHEST')),
    CONSTRAINT check_estimated_hours CHECK (estimated_hours IS NULL OR estimated_hours >= 0),
    CONSTRAINT check_logged_hours CHECK (logged_hours >= 0),
    CONSTRAINT check_story_points CHECK (story_points IS NULL OR story_points > 0)
);

-- Performance indexes
CREATE INDEX idx_task_project_status ON tasks(project_id, status_id);
CREATE INDEX idx_task_assignee ON tasks(assignee_id);
CREATE INDEX idx_task_reporter ON tasks(reporter_id);
CREATE INDEX idx_task_sprint ON tasks(sprint_id);
CREATE INDEX idx_task_parent ON tasks(parent_task_id);
CREATE INDEX idx_task_due_date ON tasks(due_date);
CREATE INDEX idx_task_org_created ON tasks(organization_id, created_at DESC);
CREATE INDEX idx_task_key_lookup ON tasks(organization_id, "key");

-- GIN index for custom fields JSONB queries (for future advanced filtering)
-- Note: GIN indexes are PostgreSQL-specific and not supported by H2. Uncomment for PostgreSQL-only:
-- CREATE INDEX idx_task_custom_fields ON tasks USING GIN (custom_fields);

-- ============================================================================
-- TASK KEY GENERATION SYSTEM
-- ============================================================================

-- Sequence table to track next available key per project
CREATE TABLE task_key_sequences (
    project_id BIGINT PRIMARY KEY REFERENCES projects(id) ON DELETE CASCADE,
    next_sequence INTEGER NOT NULL DEFAULT 1
);

-- Function to generate next task key for a project
CREATE OR REPLACE FUNCTION generate_task_key(p_project_id BIGINT)
RETURNS VARCHAR AS $$
DECLARE
    project_key VARCHAR(10);
    next_num INTEGER;
    result_key VARCHAR(20);
BEGIN
    -- Get project key
    SELECT "key" INTO project_key FROM projects WHERE id = p_project_id;

    IF project_key IS NULL THEN
        RAISE EXCEPTION 'Project not found: %', p_project_id;
    END IF;

    -- Get next sequence number (with row-level lock to prevent race conditions)
    INSERT INTO task_key_sequences (project_id, next_sequence)
    VALUES (p_project_id, 1)
    ON CONFLICT (project_id) DO UPDATE
        SET next_sequence = task_key_sequences.next_sequence + 1
    RETURNING next_sequence INTO next_num;

    -- Format: PROJ-123
    result_key := project_key || '-' || next_num;

    RETURN result_key;
END;
$$ LANGUAGE plpgsql;

-- Trigger function to auto-generate task key on insert
CREATE OR REPLACE FUNCTION auto_generate_task_key()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW."key" IS NULL OR NEW."key" = '' THEN
        NEW."key" := generate_task_key(NEW.project_id);
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Apply trigger to tasks table
CREATE TRIGGER trigger_auto_task_key
BEFORE INSERT ON tasks
FOR EACH ROW
EXECUTE FUNCTION auto_generate_task_key();

-- Trigger to update updated_at timestamp
CREATE OR REPLACE FUNCTION update_task_timestamp()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at := CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_update_task_timestamp
BEFORE UPDATE ON tasks
FOR EACH ROW
EXECUTE FUNCTION update_task_timestamp();

-- ============================================================================
-- TASK WATCHERS (Notification System)
-- ============================================================================

-- Users who want notifications without being assigned
CREATE TABLE task_watchers (
    id BIGSERIAL PRIMARY KEY,
    task_id BIGINT NOT NULL REFERENCES tasks(id) ON DELETE CASCADE,
    user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    added_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    added_by INTEGER REFERENCES users(id),

    CONSTRAINT unique_watcher UNIQUE (task_id, user_id)
);

CREATE INDEX idx_watcher_task ON task_watchers(task_id);
CREATE INDEX idx_watcher_user ON task_watchers(user_id);

-- ============================================================================
-- COMMENTS
-- ============================================================================

-- Add comments about task key generation
COMMENT ON TABLE tasks IS 'Core task/issue entity with auto-generated keys (PROJ-123 format)';
COMMENT ON COLUMN tasks."key" IS 'Auto-generated unique key per organization (e.g., PROJ-123)';
COMMENT ON COLUMN tasks.custom_fields IS 'Extensible JSONB field for future custom field support';
COMMENT ON TABLE task_key_sequences IS 'Tracks next available sequence number per project for key generation';
COMMENT ON TABLE task_watchers IS 'Users who want to receive notifications about task changes';
