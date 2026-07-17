ALTER TABLE review_occurrence
    DROP CONSTRAINT review_occurrence_status_valid,
    ADD COLUMN resolved_at TIMESTAMPTZ,
    ADD COLUMN completed_session_id UUID,
    ADD CONSTRAINT review_occurrence_status_valid CHECK (
        status IN ('SCHEDULED', 'COMPLETED', 'SKIPPED')
    ),
    ADD CONSTRAINT review_occurrence_resolution_valid CHECK (
        (status = 'SCHEDULED' AND resolved_at IS NULL AND completed_session_id IS NULL)
        OR
        (status = 'COMPLETED' AND resolved_at IS NOT NULL AND completed_session_id IS NOT NULL)
        OR
        (status = 'SKIPPED' AND resolved_at IS NOT NULL AND completed_session_id IS NULL)
    );

ALTER TABLE study_session
    ADD COLUMN review_occurrence_id UUID REFERENCES review_occurrence (id),
    DROP CONSTRAINT study_session_origin_valid,
    DROP CONSTRAINT study_session_cycle_context_valid,
    ADD CONSTRAINT study_session_origin_valid CHECK (
        origin IN ('CYCLE', 'FREE', 'MANUAL', 'REVIEW')
    ),
    ADD CONSTRAINT study_session_cycle_context_valid CHECK (
        (origin = 'CYCLE' AND cycle_id IS NOT NULL AND cycle_run_id IS NOT NULL AND cycle_stage_id IS NOT NULL)
        OR
        (origin IN ('FREE', 'MANUAL', 'REVIEW') AND cycle_id IS NULL AND cycle_run_id IS NULL AND cycle_stage_id IS NULL)
    ),
    ADD CONSTRAINT study_session_review_context_valid CHECK (
        (origin = 'REVIEW' AND review_occurrence_id IS NOT NULL)
        OR
        (origin <> 'REVIEW' AND review_occurrence_id IS NULL)
    ),
    ADD CONSTRAINT study_session_review_occurrence_unique UNIQUE (review_occurrence_id);
