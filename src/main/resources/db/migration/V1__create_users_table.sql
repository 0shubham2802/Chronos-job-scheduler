-- V1__create_users_table.sql
-- Flyway runs this first because it starts with V1.
-- The double underscore __ is required by Flyway — don't change it.

-- This extension gives us the gen_random_uuid() function
-- which generates a unique UUID for each row automatically
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

CREATE TABLE users (

    -- UUID is a unique 36-character ID like: 550e8400-e29b-41d4-a716-446655440000
    -- Much better than auto-increment integers because:
    -- 1. You can generate it on the client before hitting the DB
    -- 2. No one can guess the next ID (security)
    -- 3. Safe when merging data from multiple databases
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    -- The user's display name. VARCHAR(100) = max 100 characters.
    -- NOT NULL means this column is required — can never be empty
    name VARCHAR(100) NOT NULL,

    -- Email must be unique — no two users can share the same email
    -- This is how we identify users during login
    email VARCHAR(255) NOT NULL UNIQUE,

    -- We NEVER store plain passwords. We store the BCrypt hash.
    -- BCrypt hashes are always 60 characters long
    password_hash VARCHAR(255) NOT NULL,

    -- Automatically records when this user registered
    -- DEFAULT NOW() means PostgreSQL fills this in automatically
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),

    -- Tracks the last login time. NULL is fine — new users haven't logged in yet
    updated_at TIMESTAMP
);

-- An index on email speeds up the LOGIN query dramatically.
-- Every login does: SELECT * FROM users WHERE email = ?
-- Without an index, PostgreSQL scans every row. With an index, it's instant.
CREATE INDEX idx_users_email ON users(email);