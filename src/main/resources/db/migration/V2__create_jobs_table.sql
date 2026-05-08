-- V2__create_jobs_table.sql
-- Flyway runs this second. It MUST run after V1 because
-- jobs references the users table (foreign key).

-- First create the ENUM types.
-- An ENUM is a column that can only hold specific allowed values.
-- This prevents garbage data — status can ONLY be one of these 5 values.

CREATE TYPE job_type AS ENUM (
    'ONE_TIME',    -- runs exactly once at a specific datetime
    'RECURRING'    -- runs repeatedly on a cron schedule
);

CREATE TYPE job_status AS ENUM (
    'PENDING',     -- created but not yet run
    'RUNNING',     -- currently being executed by a worker
    'COMPLETED',   -- ran successfully
    'FAILED',      -- ran but threw an error and exhausted all retries
    'CANCELLED'    -- user manually cancelled it
);

CREATE TABLE jobs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    -- Which user owns this job.
    -- REFERENCES users(id) = foreign key — this job must belong to a real user.
    -- ON DELETE CASCADE = if the user is deleted, delete all their jobs too
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,

    -- Human readable name like "Send weekly report" or "Clean up temp files"
    name VARCHAR(255) NOT NULL,

    -- Short description of what this job does
    description TEXT,

    -- ONE_TIME or RECURRING
    type job_type NOT NULL,

    -- Current state of this job
    status job_status NOT NULL DEFAULT 'PENDING',

    -- JSONB stores arbitrary JSON data — this holds the job's input parameters.
    -- For example: {"endpoint": "https://api.example.com/report", "format": "pdf"}
    -- JSONB is binary JSON — faster to query than plain JSON
    payload JSONB,

    -- For RECURRING jobs: a cron expression like "0 9 * * MON" (every Monday 9am)
    -- NULL for ONE_TIME jobs
    cron_expression VARCHAR(100),

    -- For ONE_TIME jobs: the exact datetime to run this job
    -- NULL for RECURRING jobs (they use cron_expression instead)
    scheduled_at TIMESTAMP,

    -- How many times to retry if the job fails before giving up
    -- Default is 3 retries
    max_retries INTEGER NOT NULL DEFAULT 3,

    -- How many times we have already retried this job
    -- Starts at 0, increments on each failure
    retry_count INTEGER NOT NULL DEFAULT 0,

    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP
);

-- Index on user_id speeds up: GET /api/jobs (list all jobs for a user)
-- This query runs every time a user opens their dashboard
CREATE INDEX idx_jobs_user_id ON jobs(user_id);

-- Index on status speeds up filtering: show me all FAILED jobs
CREATE INDEX idx_jobs_status ON jobs(status);

-- Index on scheduled_at speeds up the Scheduler's query:
-- "give me all PENDING jobs due in the next 60 seconds"
CREATE INDEX idx_jobs_scheduled_at ON jobs(scheduled_at);