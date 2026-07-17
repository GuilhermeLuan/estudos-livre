CREATE TABLE study_session (
    id UUID PRIMARY KEY,
    owner_id UUID NOT NULL REFERENCES identity_user (id),
    origin VARCHAR(16) NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
    subject_id UUID NOT NULL REFERENCES subject (id),
    content_id UUID REFERENCES content (id),
    cycle_id UUID REFERENCES study_cycle (id),
    cycle_run_id UUID REFERENCES study_cycle_run (id),
    cycle_stage_id UUID REFERENCES study_cycle_stage (id),
    started_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT study_session_origin_valid CHECK (origin IN ('CYCLE', 'FREE')),
    CONSTRAINT study_session_status_valid CHECK (status IN ('ACTIVE', 'PAUSED', 'FINISHED')),
    CONSTRAINT study_session_cycle_context_valid CHECK (
        (origin = 'CYCLE' AND cycle_id IS NOT NULL AND cycle_run_id IS NOT NULL AND cycle_stage_id IS NOT NULL)
        OR
        (origin = 'FREE' AND cycle_id IS NULL AND cycle_run_id IS NULL AND cycle_stage_id IS NULL)
    )
);

CREATE UNIQUE INDEX study_session_owner_open_unique_idx
    ON study_session (owner_id)
    WHERE status IN ('ACTIVE', 'PAUSED');

CREATE TABLE study_session_timer_segment (
    id UUID PRIMARY KEY,
    session_id UUID NOT NULL REFERENCES study_session (id) ON DELETE CASCADE,
    started_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ended_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT study_session_timer_segment_interval_valid CHECK (
        ended_at IS NULL OR ended_at >= started_at
    )
);

CREATE UNIQUE INDEX study_session_timer_segment_open_unique_idx
    ON study_session_timer_segment (session_id)
    WHERE ended_at IS NULL;
