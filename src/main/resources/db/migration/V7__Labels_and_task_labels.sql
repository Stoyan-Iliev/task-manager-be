-- ============================================================================
-- V7: Labels and Task-Label Association
-- ============================================================================
-- Purpose: Organization-wide labels for categorization and filtering
-- Features:
--   - Colored labels for visual grouping
--   - Many-to-many task-label relationship
--   - Unique label names per organization
--   - Audit fields (created_at, created_by, added_at, added_by)
-- ============================================================================

-- Labels table (org-scoped, reusable across projects)
CREATE TABLE labels (
    id BIGSERIAL PRIMARY KEY,
    organization_id BIGINT NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    name VARCHAR(50) NOT NULL,
    color VARCHAR(7) NOT NULL DEFAULT '#3b82f6',  -- Hex color format (e.g., #ef4444 for red)
    description TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by INTEGER REFERENCES users(id),

    CONSTRAINT unique_label_per_org UNIQUE (organization_id, name),
    CONSTRAINT valid_hex_color CHECK (color ~ '^#[0-9A-Fa-f]{6}$')
);

CREATE INDEX idx_label_org ON labels(organization_id);
CREATE INDEX idx_label_name ON labels(organization_id, name);

-- Task-Label junction table (many-to-many)
CREATE TABLE task_labels (
    id BIGSERIAL PRIMARY KEY,
    task_id BIGINT NOT NULL REFERENCES tasks(id) ON DELETE CASCADE,
    label_id BIGINT NOT NULL REFERENCES labels(id) ON DELETE CASCADE,
    added_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    added_by INTEGER REFERENCES users(id),

    CONSTRAINT unique_task_label UNIQUE (task_id, label_id)
);

CREATE INDEX idx_task_labels_task ON task_labels(task_id);
CREATE INDEX idx_task_labels_label ON task_labels(label_id);
CREATE INDEX idx_task_labels_added ON task_labels(added_at DESC);

-- Comments for documentation
COMMENT ON TABLE labels IS 'Organization-wide reusable labels/tags for categorization (e.g., bug, feature, urgent)';
COMMENT ON COLUMN labels.color IS 'Hex color code for visual identification (e.g., #ef4444 for red bug labels)';
COMMENT ON TABLE task_labels IS 'Many-to-many relationship between tasks and labels with audit trail';
COMMENT ON COLUMN task_labels.added_by IS 'User who added this label to the task';

-- Sample labels for testing (optional - remove in production)
-- INSERT INTO labels (organization_id, name, color, description, created_by)
-- VALUES
--     (1, 'bug', '#ef4444', 'Bug reports and fixes', 1),
--     (1, 'feature', '#3b82f6', 'New features', 1),
--     (1, 'urgent', '#f59e0b', 'Urgent priority items', 1),
--     (1, 'technical-debt', '#8b5cf6', 'Technical debt to address', 1),
--     (1, 'frontend', '#10b981', 'Frontend-related tasks', 1),
--     (1, 'backend', '#06b6d4', 'Backend-related tasks', 1);
