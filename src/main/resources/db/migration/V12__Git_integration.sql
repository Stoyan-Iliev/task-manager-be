-- =============================================================================
-- GIT INTEGRATION SCHEMA
-- Migration: V12__Git_integration.sql
-- Description: Complete Git integration with GitHub, GitLab, Bitbucket support
-- =============================================================================

-- -----------------------------------------------------------------------------
-- Git Integration: Stores repository connections
-- -----------------------------------------------------------------------------
CREATE TABLE git_integrations (
    id BIGSERIAL PRIMARY KEY,
    organization_id BIGINT NOT NULL,
    project_id BIGINT,

    provider VARCHAR(20) NOT NULL CHECK (provider IN ('GITHUB', 'GITLAB', 'BITBUCKET')),
    repository_url VARCHAR(500) NOT NULL,
    repository_owner VARCHAR(255) NOT NULL,
    repository_name VARCHAR(255) NOT NULL,
    repository_full_name VARCHAR(500) NOT NULL,

    -- Authentication (encrypted)
    access_token_encrypted TEXT NOT NULL,
    refresh_token_encrypted TEXT,
    token_expires_at TIMESTAMP,

    -- Webhook
    webhook_id VARCHAR(100),
    webhook_secret_encrypted TEXT,
    webhook_url TEXT,

    -- Settings
    auto_link_enabled BOOLEAN DEFAULT TRUE,
    smart_commits_enabled BOOLEAN DEFAULT TRUE,
    auto_close_on_merge BOOLEAN DEFAULT TRUE,
    branch_prefix VARCHAR(50),

    -- Metadata
    is_active BOOLEAN DEFAULT TRUE,
    last_sync_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_git_integration_org FOREIGN KEY (organization_id)
        REFERENCES organizations(id) ON DELETE CASCADE,
    CONSTRAINT fk_git_integration_project FOREIGN KEY (project_id)
        REFERENCES projects(id) ON DELETE CASCADE,
    CONSTRAINT fk_git_integration_creator FOREIGN KEY (created_by)
        REFERENCES users(id),
    CONSTRAINT uk_git_integration_repo UNIQUE (organization_id, repository_full_name)
);

CREATE INDEX idx_git_integration_org ON git_integrations(organization_id);
CREATE INDEX idx_git_integration_project ON git_integrations(project_id);
CREATE INDEX idx_git_integration_active ON git_integrations(is_active);

-- -----------------------------------------------------------------------------
-- Git Branch: Tracks branches linked to tasks
-- -----------------------------------------------------------------------------
CREATE TABLE git_branches (
    id BIGSERIAL PRIMARY KEY,
    git_integration_id BIGINT NOT NULL,
    task_id BIGINT NOT NULL,

    branch_name VARCHAR(255) NOT NULL,
    branch_ref VARCHAR(500),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'MERGED', 'DELETED')),

    -- Metadata
    created_from_ui BOOLEAN DEFAULT FALSE,
    head_commit_sha VARCHAR(40),
    base_branch VARCHAR(255),

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    merged_at TIMESTAMP,
    deleted_at TIMESTAMP,

    CONSTRAINT fk_git_branch_integration FOREIGN KEY (git_integration_id)
        REFERENCES git_integrations(id) ON DELETE CASCADE,
    CONSTRAINT fk_git_branch_task FOREIGN KEY (task_id)
        REFERENCES tasks(id) ON DELETE CASCADE,
    CONSTRAINT fk_git_branch_creator FOREIGN KEY (created_by)
        REFERENCES users(id),
    CONSTRAINT uk_git_branch_name UNIQUE (git_integration_id, branch_name)
);

CREATE INDEX idx_git_branch_task ON git_branches(task_id);
CREATE INDEX idx_git_branch_integration ON git_branches(git_integration_id);
CREATE INDEX idx_git_branch_status ON git_branches(status);

-- -----------------------------------------------------------------------------
-- Git Commit: Stores commit metadata
-- -----------------------------------------------------------------------------
CREATE TABLE git_commits (
    id BIGSERIAL PRIMARY KEY,
    git_integration_id BIGINT NOT NULL,

    commit_sha VARCHAR(40) NOT NULL,
    parent_sha VARCHAR(40),
    branch_name VARCHAR(255),

    -- Author info
    author_name VARCHAR(255),
    author_email VARCHAR(255),
    author_date TIMESTAMP,
    committer_name VARCHAR(255),
    committer_email VARCHAR(255),
    committer_date TIMESTAMP,

    -- Commit details
    message TEXT NOT NULL,
    message_body TEXT,

    -- Analysis (for future AI features)
    commit_type VARCHAR(50),
    lines_added INT,
    lines_deleted INT,
    files_changed INT,

    -- External links
    commit_url TEXT,

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_git_commit_integration FOREIGN KEY (git_integration_id)
        REFERENCES git_integrations(id) ON DELETE CASCADE,
    CONSTRAINT uk_git_commit_sha UNIQUE (git_integration_id, commit_sha)
);

CREATE INDEX idx_git_commit_sha ON git_commits(commit_sha);
CREATE INDEX idx_git_commit_integration ON git_commits(git_integration_id);
CREATE INDEX idx_git_commit_author ON git_commits(author_email);
CREATE INDEX idx_git_commit_date ON git_commits(author_date DESC);

-- Full-text search on commit messages
CREATE INDEX idx_git_commit_message_fts ON git_commits USING gin(to_tsvector('english', message));

-- -----------------------------------------------------------------------------
-- Git Commit Task Link: Many-to-many relationship
-- -----------------------------------------------------------------------------
CREATE TABLE git_commit_tasks (
    id BIGSERIAL PRIMARY KEY,
    git_commit_id BIGINT NOT NULL,
    task_id BIGINT NOT NULL,

    link_method VARCHAR(50) NOT NULL CHECK (link_method IN ('BRANCH_NAME', 'COMMIT_MESSAGE', 'PR_DESCRIPTION', 'MANUAL')),

    -- Smart commit commands executed
    smart_commands JSONB,

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_git_commit_task_commit FOREIGN KEY (git_commit_id)
        REFERENCES git_commits(id) ON DELETE CASCADE,
    CONSTRAINT fk_git_commit_task_task FOREIGN KEY (task_id)
        REFERENCES tasks(id) ON DELETE CASCADE,
    CONSTRAINT uk_git_commit_task UNIQUE (git_commit_id, task_id)
);

CREATE INDEX idx_commit_task_commit ON git_commit_tasks(git_commit_id);
CREATE INDEX idx_commit_task_task ON git_commit_tasks(task_id);

-- -----------------------------------------------------------------------------
-- Git Pull Request: Stores PR/MR metadata
-- -----------------------------------------------------------------------------
CREATE TABLE git_pull_requests (
    id BIGSERIAL PRIMARY KEY,
    git_integration_id BIGINT NOT NULL,
    git_branch_id BIGINT,

    pr_number INT NOT NULL,
    pr_title VARCHAR(500) NOT NULL,
    pr_description TEXT,
    pr_url TEXT,

    status VARCHAR(20) NOT NULL CHECK (status IN ('OPEN', 'DRAFT', 'APPROVED', 'MERGED', 'CLOSED')),

    -- Branch info
    source_branch VARCHAR(255),
    target_branch VARCHAR(255),
    head_commit_sha VARCHAR(40),

    -- Author
    author_username VARCHAR(255),
    author_name VARCHAR(255),
    author_email VARCHAR(255),

    -- Review info
    reviewers JSONB,
    approvals_count INT DEFAULT 0,
    required_approvals INT,

    -- CI/CD
    checks_status VARCHAR(20) CHECK (checks_status IN ('PENDING', 'SUCCESS', 'FAILURE', 'CANCELLED')),
    checks_count INT DEFAULT 0,
    checks_passed INT DEFAULT 0,
    checks_details JSONB,

    -- Merge info
    mergeable BOOLEAN,
    merged BOOLEAN DEFAULT FALSE,
    merged_at TIMESTAMP,
    merged_by VARCHAR(255),
    merge_commit_sha VARCHAR(40),

    -- Timestamps
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    closed_at TIMESTAMP,

    CONSTRAINT fk_git_pr_integration FOREIGN KEY (git_integration_id)
        REFERENCES git_integrations(id) ON DELETE CASCADE,
    CONSTRAINT fk_git_pr_branch FOREIGN KEY (git_branch_id)
        REFERENCES git_branches(id) ON DELETE SET NULL,
    CONSTRAINT uk_git_pr_number UNIQUE (git_integration_id, pr_number)
);

CREATE INDEX idx_git_pr_integration ON git_pull_requests(git_integration_id);
CREATE INDEX idx_git_pr_branch ON git_pull_requests(git_branch_id);
CREATE INDEX idx_git_pr_status ON git_pull_requests(status);
CREATE INDEX idx_git_pr_merged ON git_pull_requests(merged, merged_at DESC);

-- -----------------------------------------------------------------------------
-- Git PR Task Link: Many-to-many relationship
-- -----------------------------------------------------------------------------
CREATE TABLE git_pr_tasks (
    id BIGSERIAL PRIMARY KEY,
    git_pull_request_id BIGINT NOT NULL,
    task_id BIGINT NOT NULL,

    link_method VARCHAR(50) NOT NULL CHECK (link_method IN ('BRANCH_NAME', 'PR_TITLE', 'PR_DESCRIPTION', 'MANUAL')),

    -- Should this PR close the task when merged?
    closes_task BOOLEAN DEFAULT FALSE,

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_git_pr_task_pr FOREIGN KEY (git_pull_request_id)
        REFERENCES git_pull_requests(id) ON DELETE CASCADE,
    CONSTRAINT fk_git_pr_task_task FOREIGN KEY (task_id)
        REFERENCES tasks(id) ON DELETE CASCADE,
    CONSTRAINT uk_git_pr_task UNIQUE (git_pull_request_id, task_id)
);

CREATE INDEX idx_pr_task_pr ON git_pr_tasks(git_pull_request_id);
CREATE INDEX idx_pr_task_task ON git_pr_tasks(task_id);

-- -----------------------------------------------------------------------------
-- Git Deployment: Tracks deployments across environments
-- -----------------------------------------------------------------------------
CREATE TABLE git_deployments (
    id BIGSERIAL PRIMARY KEY,
    git_integration_id BIGINT NOT NULL,

    environment VARCHAR(50) NOT NULL CHECK (environment IN ('DEV', 'STAGING', 'PRODUCTION', 'CUSTOM')),
    environment_url TEXT,

    commit_sha VARCHAR(40) NOT NULL,
    commit_message TEXT,

    -- Deployment info
    deployment_id VARCHAR(100),
    deployment_status VARCHAR(20) NOT NULL CHECK (deployment_status IN ('PENDING', 'IN_PROGRESS', 'SUCCESS', 'FAILURE', 'CANCELLED')),

    -- Metadata
    deployed_by VARCHAR(255),
    deployment_tool VARCHAR(100),

    started_at TIMESTAMP NOT NULL,
    completed_at TIMESTAMP,

    -- Additional data
    metadata JSONB,

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_git_deployment_integration FOREIGN KEY (git_integration_id)
        REFERENCES git_integrations(id) ON DELETE CASCADE
);

CREATE INDEX idx_git_deployment_integration ON git_deployments(git_integration_id);
CREATE INDEX idx_git_deployment_commit ON git_deployments(commit_sha);
CREATE INDEX idx_git_deployment_env ON git_deployments(environment);
CREATE INDEX idx_git_deployment_time ON git_deployments(completed_at DESC);
CREATE INDEX idx_git_deployment_status ON git_deployments(deployment_status);

-- -----------------------------------------------------------------------------
-- Git Webhook Event: Log all received webhooks
-- -----------------------------------------------------------------------------
CREATE TABLE git_webhook_events (
    id BIGSERIAL PRIMARY KEY,
    git_integration_id BIGINT,

    provider VARCHAR(20) NOT NULL CHECK (provider IN ('GITHUB', 'GITLAB', 'BITBUCKET')),
    event_type VARCHAR(100) NOT NULL,
    event_action VARCHAR(100),

    payload JSONB NOT NULL,
    signature VARCHAR(500),

    -- Processing status
    processed BOOLEAN DEFAULT FALSE,
    processing_started_at TIMESTAMP,
    processing_completed_at TIMESTAMP,
    processing_error TEXT,
    retry_count INT DEFAULT 0,

    received_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_webhook_event_integration FOREIGN KEY (git_integration_id)
        REFERENCES git_integrations(id) ON DELETE SET NULL
);

CREATE INDEX idx_webhook_event_integration ON git_webhook_events(git_integration_id);
CREATE INDEX idx_webhook_event_type ON git_webhook_events(event_type);
CREATE INDEX idx_webhook_event_processed ON git_webhook_events(processed, received_at);
CREATE INDEX idx_webhook_event_retry ON git_webhook_events(processed, retry_count);
CREATE INDEX idx_webhook_event_received ON git_webhook_events(received_at DESC);

-- -----------------------------------------------------------------------------
-- Smart Commit Executions: Track executed commands for audit
-- -----------------------------------------------------------------------------
CREATE TABLE smart_commit_executions (
    id BIGSERIAL PRIMARY KEY,
    git_commit_id BIGINT NOT NULL,
    task_id BIGINT NOT NULL,

    command_type VARCHAR(50) NOT NULL CHECK (command_type IN ('TRANSITION', 'COMMENT', 'TIME', 'ASSIGN', 'LABEL', 'CUSTOM')),
    command_text TEXT NOT NULL,

    -- Execution result
    executed BOOLEAN DEFAULT FALSE,
    execution_error TEXT,

    -- Context
    executed_by BIGINT,
    executed_at TIMESTAMP,

    -- Audit trail reference (optional link to activity_log)
    -- Note: No FK constraint due to activity_log being a partitioned table
    activity_log_id BIGINT,

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_smart_commit_exec_commit FOREIGN KEY (git_commit_id)
        REFERENCES git_commits(id) ON DELETE CASCADE,
    CONSTRAINT fk_smart_commit_exec_task FOREIGN KEY (task_id)
        REFERENCES tasks(id) ON DELETE CASCADE,
    CONSTRAINT fk_smart_commit_exec_user FOREIGN KEY (executed_by)
        REFERENCES users(id)
);

CREATE INDEX idx_smart_commit_exec_commit ON smart_commit_executions(git_commit_id);
CREATE INDEX idx_smart_commit_exec_task ON smart_commit_executions(task_id);
CREATE INDEX idx_smart_commit_exec_type ON smart_commit_executions(command_type);

-- =============================================================================
-- UTILITY FUNCTIONS
-- =============================================================================

-- Function to automatically update updated_at column
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

COMMENT ON FUNCTION update_updated_at_column IS 'Automatically updates the updated_at timestamp on UPDATE';

-- =============================================================================
-- UPDATE TRIGGERS
-- =============================================================================

-- Update timestamps for git_integrations
CREATE TRIGGER git_integration_updated_at
    BEFORE UPDATE ON git_integrations
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Update timestamps for git_pull_requests
CREATE TRIGGER git_pr_updated_at
    BEFORE UPDATE ON git_pull_requests
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- =============================================================================
-- MATERIALIZED VIEW FOR DORA METRICS (Phase 3 - Prepared)
-- =============================================================================

CREATE MATERIALIZED VIEW dora_metrics_daily AS
SELECT
    git_integration_id,
    DATE(completed_at) AS metric_date,
    environment,
    COUNT(*) AS deployment_count,
    AVG(EXTRACT(EPOCH FROM (completed_at - started_at)) / 60) AS avg_deployment_duration_minutes,
    COUNT(CASE WHEN deployment_status = 'FAILURE' THEN 1 END) AS failed_deployments,
    COUNT(CASE WHEN deployment_status = 'SUCCESS' THEN 1 END) AS successful_deployments
FROM git_deployments
WHERE completed_at IS NOT NULL
GROUP BY git_integration_id, DATE(completed_at), environment;

CREATE UNIQUE INDEX idx_dora_metrics_daily_unique ON dora_metrics_daily(git_integration_id, metric_date, environment);
CREATE INDEX idx_dora_metrics_date ON dora_metrics_daily(metric_date DESC);

-- =============================================================================
-- COMMENTS
-- =============================================================================

COMMENT ON TABLE git_integrations IS 'Stores repository connections with authentication tokens (encrypted)';
COMMENT ON TABLE git_branches IS 'Tracks Git branches linked to tasks';
COMMENT ON TABLE git_commits IS 'Stores commit metadata for activity tracking';
COMMENT ON TABLE git_commit_tasks IS 'Many-to-many relationship between commits and tasks';
COMMENT ON TABLE git_pull_requests IS 'Stores pull/merge request metadata with review and CI status';
COMMENT ON TABLE git_pr_tasks IS 'Links pull requests to tasks they address';
COMMENT ON TABLE git_deployments IS 'Tracks deployments for DORA metrics and production visibility';
COMMENT ON TABLE git_webhook_events IS 'Logs all received webhooks for debugging and replay';
COMMENT ON TABLE smart_commit_executions IS 'Audit trail of smart commit command executions';
COMMENT ON MATERIALIZED VIEW dora_metrics_daily IS 'Pre-aggregated DORA metrics for fast querying';
