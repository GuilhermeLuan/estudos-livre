ALTER TABLE study_session
    ADD COLUMN progress_cycle_id UUID REFERENCES study_cycle (id),
    ADD COLUMN progress_cycle_run_id UUID REFERENCES study_cycle_run (id);

ALTER TABLE study_cycle_run
    ADD COLUMN projection_abandoned BOOLEAN NOT NULL DEFAULT FALSE;

UPDATE study_session session
SET progress_cycle_id = COALESCE(
    session.cycle_id,
    (SELECT run_stage.cycle_id
     FROM study_session_credit credit
     JOIN study_cycle_run_stage run_stage ON run_stage.id = credit.run_stage_id
     WHERE credit.session_id = session.id
     LIMIT 1)
);

UPDATE study_session session
SET progress_cycle_run_id = COALESCE(
    session.cycle_run_id,
    (SELECT run_stage.run_id
     FROM study_session_credit credit
     JOIN study_cycle_run_stage run_stage ON run_stage.id = credit.run_stage_id
     WHERE credit.session_id = session.id
     LIMIT 1)
);

CREATE INDEX study_session_progress_cycle_chronology_idx
    ON study_session (progress_cycle_id, started_at, id)
    WHERE status = 'FINISHED';
