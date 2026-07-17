CREATE TABLE study_cycle_run (
    id UUID PRIMARY KEY,
    cycle_id UUID NOT NULL REFERENCES study_cycle (id),
    run_number INTEGER NOT NULL,
    current_stage_position INTEGER NOT NULL,
    started_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT study_cycle_run_number_positive CHECK (run_number > 0),
    CONSTRAINT study_cycle_run_stage_position_positive CHECK (current_stage_position > 0),
    CONSTRAINT study_cycle_run_number_unique UNIQUE (cycle_id, run_number)
);

CREATE UNIQUE INDEX study_cycle_run_cycle_open_unique_idx
    ON study_cycle_run (cycle_id)
    WHERE completed_at IS NULL;

CREATE UNIQUE INDEX study_cycle_owner_active_unique_idx
    ON study_cycle (owner_id)
    WHERE status = 'ACTIVE';
