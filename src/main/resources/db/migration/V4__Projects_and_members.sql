-- V4__Projects_and_members.sql
-- Project management with custom workflows

-- Create projects table
CREATE TABLE projects (
    id BIGSERIAL PRIMARY KEY,
    organization_id BIGINT NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    "key" VARCHAR(10) NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    default_status_id BIGINT,  -- FK added in V5 after task_statuses table exists
    settings JSONB DEFAULT '{}',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    created_by INT NOT NULL REFERENCES users(id),

    -- Ensure project key is unique within organization
    CONSTRAINT unique_project_key_per_org UNIQUE (organization_id, "key"),

    -- Validate project key format: 2-10 uppercase alphanumeric characters
    CONSTRAINT check_key_format CHECK ("key" ~ '^[A-Z0-9]{2,10}$')
);

-- Create indexes for common queries
CREATE INDEX idx_project_org ON projects(organization_id);
CREATE INDEX idx_project_created_by ON projects(created_by);
CREATE INDEX idx_project_created_at ON projects(created_at);

-- Create project_members table (join table with role-based access)
CREATE TABLE project_members (
    id BIGSERIAL PRIMARY KEY,
    user_id INT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    project_id BIGINT NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    role VARCHAR(50) NOT NULL,  -- PROJECT_OWNER, PROJECT_ADMIN, PROJECT_MEMBER, PROJECT_VIEWER
    added_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    added_by INT NOT NULL REFERENCES users(id),

    -- Ensure user can only be added to project once
    CONSTRAINT unique_project_membership UNIQUE (user_id, project_id)
);

-- Create indexes for project membership queries
CREATE INDEX idx_project_member_user ON project_members(user_id);
CREATE INDEX idx_project_member_project ON project_members(project_id);
CREATE INDEX idx_project_member_role ON project_members(role);
