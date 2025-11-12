-- ============================================================================
-- V8: Comments with Threading Support
-- ============================================================================
-- Purpose: Enable task comments with optional threading (1 level deep)
-- Features:
--   - Markdown-formatted comments with @mentions
--   - Parent-child comment relationships (max 1 level)
--   - Edit tracking (edited flag + updated_at timestamp)
--   - Cascade delete when task deleted
--   - User cannot be deleted if they have comments (RESTRICT)
-- ============================================================================

CREATE TABLE comments (
    id BIGSERIAL PRIMARY KEY,
    task_id BIGINT NOT NULL REFERENCES tasks(id) ON DELETE CASCADE,
    user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    parent_comment_id BIGINT REFERENCES comments(id) ON DELETE CASCADE,

    content TEXT NOT NULL,  -- Markdown with @mentions support

    edited BOOLEAN NOT NULL DEFAULT FALSE,

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,

    CONSTRAINT comment_content_not_empty CHECK (LENGTH(TRIM(content)) > 0),
    CONSTRAINT comment_content_max_length CHECK (LENGTH(content) <= 5000)
);

-- Indexes for efficient querying
CREATE INDEX idx_comment_task_created ON comments(task_id, created_at DESC);
CREATE INDEX idx_comment_parent ON comments(parent_comment_id) WHERE parent_comment_id IS NOT NULL;
CREATE INDEX idx_comment_user ON comments(user_id);
CREATE INDEX idx_comment_task_parent_null ON comments(task_id) WHERE parent_comment_id IS NULL;  -- Top-level comments

-- Comments for documentation
COMMENT ON TABLE comments IS 'Task comments with optional threading (max 1 level deep). Supports Markdown and @mentions.';
COMMENT ON COLUMN comments.parent_comment_id IS 'Parent comment for threading. NULL = top-level comment. Max depth: 1 level (replies cannot have replies).';
COMMENT ON COLUMN comments.content IS 'Markdown-formatted text with @username mentions. Max 5000 characters.';
COMMENT ON COLUMN comments.edited IS 'TRUE if comment has been edited after creation.';
COMMENT ON COLUMN comments.updated_at IS 'Timestamp of last edit. NULL if never edited.';

-- Trigger to automatically set updated_at on UPDATE
CREATE OR REPLACE FUNCTION update_comment_timestamp()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    NEW.edited = TRUE;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_update_comment_timestamp
BEFORE UPDATE ON comments
FOR EACH ROW
WHEN (OLD.content IS DISTINCT FROM NEW.content)
EXECUTE FUNCTION update_comment_timestamp();
