-- V3__Organizations_and_members.sql
-- Multi-tenant organization support

-- Create organizations table
CREATE TABLE organizations (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    slug VARCHAR(100) NOT NULL,
    description TEXT,
    settings JSONB DEFAULT '{}',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    created_by INT REFERENCES users(id),
    updated_by INT REFERENCES users(id)
);

-- Create unique index on organization slug for URL-friendly names
CREATE UNIQUE INDEX idx_org_slug ON organizations(slug);

-- Create index for created_at for sorting
CREATE INDEX idx_org_created_at ON organizations(created_at);

-- Create organization_members table (join table with role)
CREATE TABLE organization_members (
    id BIGSERIAL PRIMARY KEY,
    user_id INT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    organization_id BIGINT NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    role VARCHAR(50) NOT NULL,
    joined_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    invited_by INT REFERENCES users(id)
);

-- Create unique index to prevent duplicate memberships
CREATE UNIQUE INDEX idx_org_member_unique ON organization_members(user_id, organization_id);

-- Create indexes for common queries
CREATE INDEX idx_org_member_user_id ON organization_members(user_id);
CREATE INDEX idx_org_member_org_id ON organization_members(organization_id);
CREATE INDEX idx_org_member_role ON organization_members(role);
