-- Add retry support fields to email notification queue
-- Enables exponential backoff retry for failed emails

-- Add next_retry_at column for scheduling retries
ALTER TABLE email_notification_queue
    ADD COLUMN next_retry_at TIMESTAMP;

-- Update status constraint to include PERMANENTLY_FAILED
ALTER TABLE email_notification_queue
    DROP CONSTRAINT IF EXISTS email_queue_status_check;

ALTER TABLE email_notification_queue
    ADD CONSTRAINT email_queue_status_check
    CHECK (status IN ('PENDING', 'SENT', 'FAILED', 'PERMANENTLY_FAILED'));

-- Create index for retry scheduler queries
-- Partial index on FAILED status for efficient retry lookups
CREATE INDEX idx_email_queue_retry
    ON email_notification_queue(status, next_retry_at)
    WHERE status = 'FAILED';

-- Comment on new column
COMMENT ON COLUMN email_notification_queue.next_retry_at IS
    'Timestamp when this notification is eligible for retry (exponential backoff)';
