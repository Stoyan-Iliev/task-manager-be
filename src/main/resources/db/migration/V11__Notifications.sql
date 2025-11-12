-- =====================================================
-- V11: Notifications System
-- =====================================================
-- Real-time notification system for task watchers.
-- Triggered by task changes, comments, and @mentions.
--
-- Features:
-- - Multi-level context (org → project → task)
-- - Read/unread tracking
-- - Notification types for different events
-- - Cleanup of old read notifications
-- =====================================================

CREATE TABLE notifications (
    id BIGSERIAL PRIMARY KEY,

    -- Multi-level context
    organization_id BIGINT NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    project_id BIGINT REFERENCES projects(id) ON DELETE CASCADE,
    task_id BIGINT REFERENCES tasks(id) ON DELETE CASCADE,

    -- Recipient
    user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,

    -- Notification details
    type VARCHAR(50) NOT NULL,  -- TASK_CREATED, STATUS_CHANGED, ASSIGNED, COMMENTED, etc.
    title VARCHAR(255) NOT NULL,
    message TEXT NOT NULL,

    -- Actor (who triggered the notification)
    actor_id INTEGER REFERENCES users(id) ON DELETE SET NULL,

    -- Related entities (for deep linking)
    related_entity_type VARCHAR(50),  -- TASK, COMMENT, ATTACHMENT
    related_entity_id BIGINT,

    -- Additional context
    metadata JSONB,

    -- Read tracking
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    read_at TIMESTAMP,

    -- Timestamps
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Indexes for performance
    CONSTRAINT valid_notification_type CHECK (type IN (
        'TASK_CREATED', 'TASK_ASSIGNED', 'TASK_UNASSIGNED',
        'STATUS_CHANGED', 'PRIORITY_CHANGED', 'DUE_DATE_CHANGED',
        'COMMENT_ADDED', 'COMMENT_REPLY', 'MENTIONED',
        'ATTACHMENT_ADDED', 'WATCHER_ADDED'
    ))
);

-- Indexes for fast queries
CREATE INDEX idx_notifications_user_unread ON notifications(user_id, is_read) WHERE is_read = FALSE;
CREATE INDEX idx_notifications_user_created ON notifications(user_id, created_at DESC);
CREATE INDEX idx_notifications_task ON notifications(task_id);
CREATE INDEX idx_notifications_project ON notifications(project_id);
CREATE INDEX idx_notifications_organization ON notifications(organization_id);
CREATE INDEX idx_notifications_created_at ON notifications(created_at DESC);

-- Comments
COMMENT ON TABLE notifications IS 'Real-time notifications for task watchers';
COMMENT ON COLUMN notifications.type IS 'Type of notification event';
COMMENT ON COLUMN notifications.actor_id IS 'User who triggered the notification';
COMMENT ON COLUMN notifications.metadata IS 'Additional context (JSONB)';
COMMENT ON COLUMN notifications.is_read IS 'Whether user has read the notification';
