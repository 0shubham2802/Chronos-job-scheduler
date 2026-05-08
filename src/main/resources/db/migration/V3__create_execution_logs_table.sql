-- V3__create_execution_logs_table.sql
-- Every time a job runs (success or failure), we write a row here.
-- This gives us full history and auditability.

CREATE TYPE log_status AS ENUM (
    'SUCCESS',  -- job ran and completed without errors
    'FAILED'    -- job ran but threw an exception
);

CREATE TABLE execution_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    -- Which job this log belongs to
    -- ON DELETE CASCADE = if the job is deleted, delete its logs too
    job_id UUID NOT NULL REFERENCES jobs(id) ON DELETE CASCADE,

    -- Did this execution succeed or fail?
    status log_status NOT NULL,

    -- Which retry attempt was this? 1 = first try, 2 = first retry, etc.
    attempt INTEGER NOT NULL DEFAULT 1,

    -- If status is FAILED, store the full error message here.
    -- TEXT has no length limit — good for stack traces
    error_message TEXT,

    -- When did this execution actually start running?
    started_at TIMESTAMP NOT NULL DEFAULT NOW(),

    -- When did it finish? NULL if it's still running.
    ended_at TIMESTAMP,

    -- How long it took in milliseconds (ended_at - started_at)
    -- We calculate this when writing the log
    duration_ms BIGINT
);

-- Index on job_id speeds up: GET /api/jobs/{id}/logs
-- This fetches all logs for one specific job — runs every time
-- a user opens the Job Detail page
CREATE INDEX idx_execution_logs_job_id ON execution_logs(job_id);

-- Index to quickly find all recent failures across all jobs
-- Useful for the monitoring dashboard
CREATE INDEX idx_execution_logs_status ON execution_logs(status);