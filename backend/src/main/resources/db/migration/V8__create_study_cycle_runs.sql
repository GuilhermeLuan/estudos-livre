CREATE TABLE study_cycle_run (
    id UUID PRIMARY KEY,
    cycle_id UUID NOT NULL REFERENCES study_cycle (id),
    run_number INTEGER NOT NULL,
    status VARCHAR(24) NOT NULL DEFAULT 'IN_PROGRESS',
    started_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ended_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT study_cycle_run_number_positive CHECK (run_number > 0),
    CONSTRAINT study_cycle_run_status_valid CHECK (
        status IN ('IN_PROGRESS', 'PAUSED', 'COMPLETED', 'ABANDONED')
    ),
    CONSTRAINT study_cycle_run_end_state_consistent CHECK (
        (status IN ('IN_PROGRESS', 'PAUSED') AND ended_at IS NULL)
        OR (status IN ('COMPLETED', 'ABANDONED') AND ended_at IS NOT NULL)
    ),
    CONSTRAINT study_cycle_run_number_unique UNIQUE (cycle_id, run_number)
);

CREATE UNIQUE INDEX study_cycle_run_cycle_open_unique_idx
    ON study_cycle_run (cycle_id)
    WHERE status IN ('IN_PROGRESS', 'PAUSED');

CREATE UNIQUE INDEX study_cycle_owner_active_unique_idx
    ON study_cycle (owner_id)
    WHERE status = 'ACTIVE';
