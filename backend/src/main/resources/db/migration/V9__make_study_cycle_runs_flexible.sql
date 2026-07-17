DROP INDEX study_cycle_run_cycle_open_unique_idx;

ALTER TABLE study_cycle_run
    ADD COLUMN status VARCHAR(24);

UPDATE study_cycle_run
SET status = CASE
    WHEN completed_at IS NULL THEN 'IN_PROGRESS'
    ELSE 'COMPLETED'
END;

ALTER TABLE study_cycle_run
    ALTER COLUMN status SET DEFAULT 'IN_PROGRESS',
    ALTER COLUMN status SET NOT NULL,
    DROP CONSTRAINT study_cycle_run_stage_position_positive,
    DROP COLUMN current_stage_position;

ALTER TABLE study_cycle_run
    RENAME COLUMN completed_at TO ended_at;

ALTER TABLE study_cycle_run
    ADD CONSTRAINT study_cycle_run_status_valid CHECK (
        status IN ('IN_PROGRESS', 'PAUSED', 'COMPLETED', 'ABANDONED')
    ),
    ADD CONSTRAINT study_cycle_run_end_state_consistent CHECK (
        (status IN ('IN_PROGRESS', 'PAUSED') AND ended_at IS NULL)
        OR (status IN ('COMPLETED', 'ABANDONED') AND ended_at IS NOT NULL)
    );

CREATE UNIQUE INDEX study_cycle_run_cycle_open_unique_idx
    ON study_cycle_run (cycle_id)
    WHERE status IN ('IN_PROGRESS', 'PAUSED');
