-- Initialize payment database schema
-- This file is executed when the PostgreSQL container starts

-- Create extensions
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Note: Tables are created by JPA/Hibernate based on entity definitions
-- This file can be used for additional initialization or seed data

-- Create indexes for better query performance (if not created by JPA)
-- These will be skipped if already exist from JPA annotations

-- Sample data for testing (commented out by default)
-- INSERT INTO users (id, username, password, email, roles, created_at, updated_at, version)
-- VALUES (
--     uuid_generate_v4(),
--     'admin',
--     '$2a$10$...',  -- bcrypt encoded password
--     'admin@example.com',
--     'ROLE_ADMIN,ROLE_USER',
--     NOW(),
--     NOW(),
--     0
-- );

