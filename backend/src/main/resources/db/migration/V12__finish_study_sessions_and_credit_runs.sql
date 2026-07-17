ALTER TABLE study_session
    ADD COLUMN effective_seconds BIGINT,
    ADD COLUMN finished_at TIMESTAMPTZ,
    ADD COLUMN version INTEGER NOT NULL DEFAULT 0,
    ADD CONSTRAINT study_session_effective_seconds_valid CHECK (
        effective_seconds IS NULL OR effective_seconds >= 0
    ),
    ADD CONSTRAINT study_session_finish_state_consistent CHECK (
        (status = 'FINISHED' AND effective_seconds IS NOT NULL AND finished_at IS NOT NULL)
        OR
        (status IN ('ACTIVE', 'PAUSED') AND effective_seconds IS NULL AND finished_at IS NULL)
    );

CREATE TABLE study_cycle_run_stage (
    id UUID PRIMARY KEY,
    run_id UUID NOT NULL REFERENCES study_cycle_run (id) ON DELETE CASCADE,
    cycle_id UUID NOT NULL REFERENCES study_cycle (id),
    source_stage_id UUID REFERENCES study_cycle_stage (id) ON DELETE SET NULL,
    subject_id UUID NOT NULL REFERENCES subject (id),
    subject_name VARCHAR(120) NOT NULL,
    position INTEGER NOT NULL,
    target_seconds BIGINT NOT NULL,
    credited_seconds BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT study_cycle_run_stage_position_positive CHECK (position > 0),
    CONSTRAINT study_cycle_run_stage_target_positive CHECK (target_seconds > 0),
    CONSTRAINT study_cycle_run_stage_credit_nonnegative CHECK (credited_seconds >= 0),
    CONSTRAINT study_cycle_run_stage_position_unique UNIQUE (run_id, position)
);

CREATE INDEX study_cycle_run_stage_subject_progress_idx
    ON study_cycle_run_stage (run_id, subject_id, position);

INSERT INTO study_cycle_run_stage (
    id, run_id, cycle_id, source_stage_id, subject_id, subject_name,
    position, target_seconds
)
SELECT gen_random_uuid(), run.id, run.cycle_id, stage.id, stage.subject_id,
       subject.name, stage.position, stage.target_minutes * 60::BIGINT
FROM study_cycle_run run
JOIN study_cycle_stage stage ON stage.cycle_id = run.cycle_id
JOIN subject ON subject.id = stage.subject_id;

CREATE TABLE study_session_credit (
    session_id UUID NOT NULL REFERENCES study_session (id) ON DELETE CASCADE,
    run_stage_id UUID NOT NULL REFERENCES study_cycle_run_stage (id) ON DELETE CASCADE,
    credited_seconds BIGINT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT study_session_credit_positive CHECK (credited_seconds > 0),
    CONSTRAINT study_session_credit_unique UNIQUE (session_id, run_stage_id)
);
