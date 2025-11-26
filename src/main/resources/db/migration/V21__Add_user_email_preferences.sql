-- V21: Add user email preferences table
-- Stores per-user email notification settings with individual toggles for each notification type

CREATE TABLE user_email_preferences (
    id BIGSERIAL PRIMARY KEY,
    user_id INTEGER NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,

    -- Global toggle
    email_enabled BOOLEAN NOT NULL DEFAULT true,

    -- Task lifecycle notifications
    task_created BOOLEAN NOT NULL DEFAULT true,
    status_changed BOOLEAN NOT NULL DEFAULT true,
    priority_changed BOOLEAN NOT NULL DEFAULT true,
    due_date_changed BOOLEAN NOT NULL DEFAULT true,

    -- Assignment notifications
    task_assigned BOOLEAN NOT NULL DEFAULT true,
    task_unassigned BOOLEAN NOT NULL DEFAULT true,
    mentioned BOOLEAN NOT NULL DEFAULT true,

    -- Collaboration notifications
    comment_added BOOLEAN NOT NULL DEFAULT true,
    comment_reply BOOLEAN NOT NULL DEFAULT true,
    attachment_added BOOLEAN NOT NULL DEFAULT true,
    watcher_added BOOLEAN NOT NULL DEFAULT true,

    -- Audit fields
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP
);

-- Index for quick lookups by user
CREATE INDEX idx_email_preferences_user ON user_email_preferences(user_id);

-- Add comment for documentation
COMMENT ON TABLE user_email_preferences IS 'Per-user email notification preferences with individual toggles for each notification type';
COMMENT ON COLUMN user_email_preferences.email_enabled IS 'Master toggle - when false, no emails are sent regardless of individual settings';
