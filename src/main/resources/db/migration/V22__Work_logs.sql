-- Work Logs table for time tracking
-- Stores time entries logged by users against tasks

CREATE TABLE work_logs (
    id BIGSERIAL PRIMARY KEY,

    -- Task reference
    task_id BIGINT NOT NULL REFERENCES tasks(id) ON DELETE CASCADE,

    -- Who logged the time
    author_id INTEGER NOT NULL REFERENCES users(id) ON DELETE RESTRICT,

    -- Time logged in minutes (allows for precise tracking)
    time_spent_minutes INTEGER NOT NULL CHECK (time_spent_minutes > 0),

    -- When the work was performed (defaults to log creation date)
    work_date DATE NOT NULL DEFAULT CURRENT_DATE,

    -- Optional description of work performed
    description TEXT,

    -- Audit fields
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,

    -- Track if this was logged via smart commit
    source VARCHAR(50) NOT NULL DEFAULT 'MANUAL'
);

-- Indexes for common queries
CREATE INDEX idx_work_logs_task_id ON work_logs(task_id);
CREATE INDEX idx_work_logs_author_id ON work_logs(author_id);
CREATE INDEX idx_work_logs_work_date ON work_logs(work_date);
CREATE INDEX idx_work_logs_task_date ON work_logs(task_id, work_date);

-- Add comment for documentation
COMMENT ON TABLE work_logs IS 'Time tracking entries for tasks';
COMMENT ON COLUMN work_logs.time_spent_minutes IS 'Time spent in minutes. Use minutes for precision without floating point issues.';
COMMENT ON COLUMN work_logs.source IS 'How the entry was created: MANUAL, SMART_COMMIT, API';
