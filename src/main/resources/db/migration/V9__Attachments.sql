-- ============================================================================
-- V9: File Attachments
-- ============================================================================
-- Purpose: Enable file uploads to tasks via S3-compatible storage (MinIO)
-- Features:
--   - Store file metadata in database
--   - Actual files stored in object storage (MinIO/S3)
--   - Thumbnail generation for images
--   - File size and type validation
--   - Cascade delete when task deleted
--   - User cannot be deleted if they uploaded files (RESTRICT)
-- ============================================================================

CREATE TABLE attachments (
    id BIGSERIAL PRIMARY KEY,
    task_id BIGINT NOT NULL REFERENCES tasks(id) ON DELETE CASCADE,
    uploaded_by INTEGER NOT NULL REFERENCES users(id) ON DELETE RESTRICT,

    -- File metadata
    filename VARCHAR(255) NOT NULL,
    mime_type VARCHAR(100) NOT NULL,
    file_size_bytes BIGINT NOT NULL,

    -- Storage paths (S3/MinIO keys)
    storage_path VARCHAR(500) NOT NULL,  -- org-{orgId}/task-{taskId}/{uuid}-{filename}.ext
    thumbnail_path VARCHAR(500),          -- For images: {storage_path}_thumb.jpg

    uploaded_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT file_size_positive CHECK (file_size_bytes > 0),
    CONSTRAINT file_size_max CHECK (file_size_bytes <= 52428800),  -- 50 MB max
    CONSTRAINT filename_not_empty CHECK (LENGTH(TRIM(filename)) > 0)
);

-- Indexes for efficient querying
CREATE INDEX idx_attachment_task ON attachments(task_id, uploaded_at DESC);
CREATE INDEX idx_attachment_user ON attachments(uploaded_by);
CREATE INDEX idx_attachment_task_type ON attachments(task_id, mime_type);

-- Comments for documentation
COMMENT ON TABLE attachments IS 'File attachments stored in S3-compatible storage (MinIO). Metadata only in database.';
COMMENT ON COLUMN attachments.storage_path IS 'Object storage key in format: org-{orgId}/task-{taskId}/{uuid}-{sanitized_filename}';
COMMENT ON COLUMN attachments.thumbnail_path IS 'Thumbnail storage path for image files (200x200 JPEG). NULL for non-images.';
COMMENT ON COLUMN attachments.file_size_bytes IS 'File size in bytes. Max 50MB (52428800 bytes).';
COMMENT ON COLUMN attachments.mime_type IS 'MIME type for content type validation (e.g., image/png, application/pdf)';

-- Function to format file size for display (returns human-readable string)
CREATE OR REPLACE FUNCTION format_file_size(size_bytes BIGINT)
RETURNS TEXT AS $$
BEGIN
    IF size_bytes < 1024 THEN
        RETURN size_bytes || ' B';
    ELSIF size_bytes < 1048576 THEN
        RETURN ROUND(size_bytes / 1024.0, 2) || ' KB';
    ELSIF size_bytes < 1073741824 THEN
        RETURN ROUND(size_bytes / 1048576.0, 2) || ' MB';
    ELSE
        RETURN ROUND(size_bytes / 1073741824.0, 2) || ' GB';
    END IF;
END;
$$ LANGUAGE plpgsql IMMUTABLE;

COMMENT ON FUNCTION format_file_size IS 'Converts byte size to human-readable format (B, KB, MB, GB)';
