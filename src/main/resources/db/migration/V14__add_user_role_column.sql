ALTER TABLE users ADD COLUMN user_role VARCHAR(255) NOT NULL DEFAULT 'ROLE_user' COMMENT 'User role for authorization, comma separated ,e.g., ROLE_user, ROLE_admin';
