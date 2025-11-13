ALTER TABLE users
ADD COLUMN first_name VARCHAR(50),
ADD COLUMN last_name VARCHAR(50),
ADD COLUMN avatar_url VARCHAR(500),
ADD COLUMN job_title VARCHAR(100),
ADD COLUMN department VARCHAR(100),
ADD COLUMN phone VARCHAR(20),
ADD COLUMN timezone VARCHAR(50) DEFAULT 'UTC',
ADD COLUMN language VARCHAR(10) DEFAULT 'en',
ADD COLUMN date_format VARCHAR(20) DEFAULT 'MM/DD/YYYY',
ADD COLUMN time_format VARCHAR(10) DEFAULT '12h',
ADD COLUMN bio TEXT;

CREATE INDEX idx_users_first_last_name ON users(first_name, last_name);
CREATE INDEX idx_users_department ON users(department);

COMMENT ON COLUMN users.first_name IS 'User first name';
COMMENT ON COLUMN users.last_name IS 'User last name';
COMMENT ON COLUMN users.avatar_url IS 'URL/path to user avatar image stored in MinIO/S3';
COMMENT ON COLUMN users.job_title IS 'User job title or role in the organization';
COMMENT ON COLUMN users.department IS 'Department or team the user belongs to';
COMMENT ON COLUMN users.phone IS 'User phone number';
COMMENT ON COLUMN users.timezone IS 'User preferred timezone (e.g., America/New_York, Europe/London)';
COMMENT ON COLUMN users.language IS 'User preferred language code (e.g., en, es, fr)';
COMMENT ON COLUMN users.date_format IS 'User preferred date format (e.g., MM/DD/YYYY, DD/MM/YYYY)';
COMMENT ON COLUMN users.time_format IS 'User preferred time format (12h or 24h)';
COMMENT ON COLUMN users.bio IS 'User biography or description';
