-- Add creator_username to store Git provider username for webhook-created branches
-- Following the same pattern as git_pull_requests.author_username and git_commits.author_name
ALTER TABLE git_branches ADD COLUMN creator_username VARCHAR(255);
