-- ============================================================================
-- V20: Rename comment user_id column to author_id
-- ============================================================================
-- Purpose: Align column naming with frontend expectations (author instead of user)
-- Changes:
--   - Rename comments.user_id to comments.author_id
--   - Rename index idx_comment_user to idx_comment_author
-- ============================================================================

-- Rename the column
ALTER TABLE comments RENAME COLUMN user_id TO author_id;

-- Drop the old index
DROP INDEX IF EXISTS idx_comment_user;

-- Create new index with updated name
CREATE INDEX idx_comment_author ON comments(author_id);

-- Update column comment
COMMENT ON COLUMN comments.author_id IS 'User who wrote the comment (author)';
