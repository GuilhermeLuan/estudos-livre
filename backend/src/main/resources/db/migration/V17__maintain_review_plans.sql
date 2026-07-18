ALTER TABLE review_plan
    ADD COLUMN version INTEGER NOT NULL DEFAULT 0;

ALTER TABLE review_plan
    DROP CONSTRAINT review_plan_status_valid,
    ADD CONSTRAINT review_plan_status_valid CHECK (status IN ('ACTIVE', 'CANCELED'));

ALTER TABLE review_occurrence
    DROP CONSTRAINT review_occurrence_status_valid,
    DROP CONSTRAINT review_occurrence_resolution_valid,
    ADD CONSTRAINT review_occurrence_status_valid CHECK (
        status IN ('SCHEDULED', 'COMPLETED', 'SKIPPED', 'CANCELED')
    ),
    ADD CONSTRAINT review_occurrence_resolution_valid CHECK (
        (status = 'SCHEDULED' AND resolved_at IS NULL AND completed_session_id IS NULL)
        OR
        (status = 'COMPLETED' AND resolved_at IS NOT NULL AND completed_session_id IS NOT NULL)
        OR
        (status IN ('SKIPPED', 'CANCELED') AND resolved_at IS NOT NULL AND completed_session_id IS NULL)
    );

CREATE INDEX review_plan_owner_status_updated_idx
    ON review_plan (owner_id, status, updated_at DESC);
