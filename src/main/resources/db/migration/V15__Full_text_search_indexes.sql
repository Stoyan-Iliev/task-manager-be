-- V15: Full-text search indexes and saved searches
-- Created: Phase 2, Section 2.2 - Search API
-- Description: Adds PostgreSQL full-text search indexes and saved searches functionality

-- Add full-text search index on tasks
-- Combines title and description for efficient text search
CREATE INDEX idx_tasks_fulltext_search ON tasks
USING gin(to_tsvector('english', COALESCE(title, '') || ' ' || COALESCE(description, '')));

-- Add full-text search index on projects
-- Combines name and description for efficient text search
CREATE INDEX idx_projects_fulltext_search ON projects
USING gin(to_tsvector('english', COALESCE(name, '') || ' ' || COALESCE(description, '')));

-- Add full-text search index on comments
-- Search within comment content
CREATE INDEX idx_comments_fulltext_search ON comments
USING gin(to_tsvector('english', COALESCE(content, '')));

-- Saved searches table
-- Allows users to save frequently used search queries
CREATE TABLE saved_searches (
    id BIGSERIAL PRIMARY KEY,
    user_id INTEGER NOT NULL,
    organization_id BIGINT NOT NULL,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    entity_type VARCHAR(50) NOT NULL, -- TASK, PROJECT, USER, GLOBAL
    query_params JSONB NOT NULL, -- Stores search filters as JSON
    is_shared BOOLEAN DEFAULT FALSE, -- Can be shared with team
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,

    CONSTRAINT fk_saved_search_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_saved_search_organization FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE CASCADE
);

-- Indexes for saved searches
CREATE INDEX idx_saved_searches_user ON saved_searches(user_id);
CREATE INDEX idx_saved_searches_org ON saved_searches(organization_id);
CREATE INDEX idx_saved_searches_shared ON saved_searches(is_shared) WHERE is_shared = true;

-- Add comment to explain the full-text search usage
COMMENT ON INDEX idx_tasks_fulltext_search IS 'Full-text search index on tasks (title + description)';
COMMENT ON INDEX idx_projects_fulltext_search IS 'Full-text search index on projects (name + description)';
COMMENT ON INDEX idx_comments_fulltext_search IS 'Full-text search index on comments (content)';
COMMENT ON TABLE saved_searches IS 'Stores user-saved search queries for quick access';
