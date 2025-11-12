-- V16: Releases
-- Created: Phase 2, Section 2.3 - Releases API
-- Description: Adds releases and release-task associations

-- Releases table
-- Represents a version release for a project (similar to JIRA Releases)
CREATE TABLE releases (
    id BIGSERIAL PRIMARY KEY,
    project_id BIGINT NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    version VARCHAR(100),
    release_date DATE,
    status VARCHAR(50) NOT NULL DEFAULT 'PLANNED', -- PLANNED, IN_PROGRESS, RELEASED, ARCHIVED
    created_by INTEGER NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    released_at TIMESTAMP,
    archived_at TIMESTAMP,

    CONSTRAINT fk_release_project FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE,
    CONSTRAINT fk_release_created_by FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT uq_release_project_name UNIQUE (project_id, name)
);

-- Release-Task association table (many-to-many)
-- Links tasks to releases
CREATE TABLE release_tasks (
    id BIGSERIAL PRIMARY KEY,
    release_id BIGINT NOT NULL,
    task_id BIGINT NOT NULL,
    added_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    added_by INTEGER NOT NULL,

    CONSTRAINT fk_release_task_release FOREIGN KEY (release_id) REFERENCES releases(id) ON DELETE CASCADE,
    CONSTRAINT fk_release_task_task FOREIGN KEY (task_id) REFERENCES tasks(id) ON DELETE CASCADE,
    CONSTRAINT fk_release_task_added_by FOREIGN KEY (added_by) REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT uq_release_task UNIQUE (release_id, task_id)
);

-- Indexes for releases
CREATE INDEX idx_releases_project ON releases(project_id);
CREATE INDEX idx_releases_status ON releases(status);
CREATE INDEX idx_releases_release_date ON releases(release_date);
CREATE INDEX idx_releases_created_by ON releases(created_by);

-- Indexes for release_tasks
CREATE INDEX idx_release_tasks_release ON release_tasks(release_id);
CREATE INDEX idx_release_tasks_task ON release_tasks(task_id);

-- Comments
COMMENT ON TABLE releases IS 'Stores project releases/versions';
COMMENT ON TABLE release_tasks IS 'Many-to-many association between releases and tasks';
COMMENT ON COLUMN releases.status IS 'Release status: PLANNED, IN_PROGRESS, RELEASED, ARCHIVED';
