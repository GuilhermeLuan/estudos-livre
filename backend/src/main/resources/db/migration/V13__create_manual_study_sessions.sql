ALTER TABLE study_session
    ADD COLUMN notes TEXT,
    DROP CONSTRAINT study_session_origin_valid,
    DROP CONSTRAINT study_session_cycle_context_valid,
    ADD CONSTRAINT study_session_origin_valid CHECK (origin IN ('CYCLE', 'FREE', 'MANUAL')),
    ADD CONSTRAINT study_session_cycle_context_valid CHECK (
        (origin = 'CYCLE' AND cycle_id IS NOT NULL AND cycle_run_id IS NOT NULL AND cycle_stage_id IS NOT NULL)
        OR
        (origin IN ('FREE', 'MANUAL') AND cycle_id IS NULL AND cycle_run_id IS NULL AND cycle_stage_id IS NULL)
    ),
    ADD CONSTRAINT study_session_notes_length_valid CHECK (
        notes IS NULL OR CHAR_LENGTH(notes) <= 4000
    );
