-- Email notification queue for batched delivery via SendGrid
CREATE TABLE email_notification_queue (
    id BIGSERIAL PRIMARY KEY,
    task_id BIGINT NOT NULL REFERENCES tasks(id) ON DELETE CASCADE,
    recipient_user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    notification_id BIGINT REFERENCES notifications(id) ON DELETE SET NULL,
    notification_type VARCHAR(50) NOT NULL,
    notification_data JSONB NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    processed_at TIMESTAMP,
    sent_at TIMESTAMP,
    error_message TEXT,
    retry_count INTEGER DEFAULT 0,
    CONSTRAINT email_queue_status_check CHECK (status IN ('PENDING', 'SENT', 'FAILED'))
);

-- Indexes for efficient querying
CREATE INDEX idx_email_queue_task_recipient_status ON email_notification_queue(task_id, recipient_user_id, status);
CREATE INDEX idx_email_queue_status_created ON email_notification_queue(status, created_at);
CREATE INDEX idx_email_queue_notification ON email_notification_queue(notification_id);
CREATE INDEX idx_email_queue_pending_processing ON email_notification_queue(status, created_at) WHERE status = 'PENDING';

COMMENT ON TABLE email_notification_queue IS 'Queue for batched email notifications sent via SendGrid every minute';
COMMENT ON COLUMN email_notification_queue.notification_data IS 'JSONB containing task details (key, title, project name), actor info, and notification content';
COMMENT ON COLUMN email_notification_queue.status IS 'PENDING: queued and awaiting processing, SENT: successfully delivered, FAILED: delivery failed after retries';
COMMENT ON COLUMN email_notification_queue.retry_count IS 'Number of delivery attempts made for this notification';
