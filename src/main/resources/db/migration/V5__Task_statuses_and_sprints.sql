-- V5__Task_statuses_and_sprints.sql
-- Custom task statuses (per-project workflows) and sprint management

-- Create task_statuses table (project-scoped workflow customization)
CREATE TABLE task_statuses (
    id BIGSERIAL PRIMARY KEY,
    project_id BIGINT NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    name VARCHAR(100) NOT NULL,
    color VARCHAR(7),  -- Hex color code: #3b82f6
    order_index INTEGER NOT NULL,
    category VARCHAR(20) NOT NULL,  -- TODO, IN_PROGRESS, DONE
    is_default BOOLEAN DEFAULT false,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Validate color format (hex color)
    CONSTRAINT check_color_format CHECK (color ~ '^#[0-9a-fA-F]{6}$'),

    -- Validate category values
    CONSTRAINT check_category_values CHECK (category IN ('TODO', 'IN_PROGRESS', 'DONE'))
);

-- Create indexes for task status queries
CREATE INDEX idx_status_project ON task_statuses(project_id);
CREATE INDEX idx_status_order ON task_statuses(project_id, order_index);
CREATE INDEX idx_status_category ON task_statuses(project_id, category);

-- Now add FK from projects to task_statuses (circular dependency resolved)
ALTER TABLE projects
ADD CONSTRAINT fk_project_default_status
FOREIGN KEY (default_status_id) REFERENCES task_statuses(id) ON DELETE SET NULL;

-- Create sprints table (iterations/versions)
CREATE TABLE sprints (
    id BIGSERIAL PRIMARY KEY,
    project_id BIGINT NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    goal TEXT,
    start_date DATE,
    end_date DATE,
    status VARCHAR(20) NOT NULL DEFAULT 'PLANNED',  -- PLANNED, ACTIVE, COMPLETED, CANCELLED
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by INT NOT NULL REFERENCES users(id),

    -- Ensure end date is after start date
    CONSTRAINT check_sprint_dates CHECK (end_date IS NULL OR end_date >= start_date),

    -- Validate sprint status values
    CONSTRAINT check_sprint_status CHECK (status IN ('PLANNED', 'ACTIVE', 'COMPLETED', 'CANCELLED'))
);

-- Create indexes for sprint queries
CREATE INDEX idx_sprint_project ON sprints(project_id);
CREATE INDEX idx_sprint_status ON sprints(project_id, status);
CREATE INDEX idx_sprint_dates ON sprints(start_date, end_date);

-- Create status_templates table (pre-built workflow templates)
CREATE TABLE status_templates (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    description TEXT,
    statuses JSONB NOT NULL,  -- Array of status definitions
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Insert default status templates
INSERT INTO status_templates (name, description, statuses) VALUES
('Software Development', 'Typical software development workflow with code review and testing',
 '[
   {"name": "Backlog", "color": "#6b7280", "order": 0, "category": "TODO"},
   {"name": "To Do", "color": "#3b82f6", "order": 1, "category": "TODO"},
   {"name": "In Progress", "color": "#f59e0b", "order": 2, "category": "IN_PROGRESS"},
   {"name": "Code Review", "color": "#8b5cf6", "order": 3, "category": "IN_PROGRESS"},
   {"name": "Testing", "color": "#06b6d4", "order": 4, "category": "IN_PROGRESS"},
   {"name": "Done", "color": "#10b981", "order": 5, "category": "DONE"}
 ]'::jsonb),

('Marketing Campaign', 'Marketing and content creation workflow',
 '[
   {"name": "Ideas", "color": "#a855f7", "order": 0, "category": "TODO"},
   {"name": "Planning", "color": "#3b82f6", "order": 1, "category": "IN_PROGRESS"},
   {"name": "In Design", "color": "#f59e0b", "order": 2, "category": "IN_PROGRESS"},
   {"name": "Review", "color": "#06b6d4", "order": 3, "category": "IN_PROGRESS"},
   {"name": "Published", "color": "#10b981", "order": 4, "category": "DONE"}
 ]'::jsonb),

('Customer Support', 'Customer service and support ticket workflow',
 '[
   {"name": "New", "color": "#3b82f6", "order": 0, "category": "TODO"},
   {"name": "Assigned", "color": "#f59e0b", "order": 1, "category": "IN_PROGRESS"},
   {"name": "In Progress", "color": "#8b5cf6", "order": 2, "category": "IN_PROGRESS"},
   {"name": "Waiting for Customer", "color": "#f97316", "order": 3, "category": "IN_PROGRESS"},
   {"name": "Resolved", "color": "#10b981", "order": 4, "category": "DONE"}
 ]'::jsonb),

('Simple Kanban', 'Basic 3-column board for simple workflows',
 '[
   {"name": "To Do", "color": "#3b82f6", "order": 0, "category": "TODO"},
   {"name": "Doing", "color": "#f59e0b", "order": 1, "category": "IN_PROGRESS"},
   {"name": "Done", "color": "#10b981", "order": 2, "category": "DONE"}
 ]'::jsonb);

-- Create index for template name searches
CREATE INDEX idx_status_template_name ON status_templates(name);
