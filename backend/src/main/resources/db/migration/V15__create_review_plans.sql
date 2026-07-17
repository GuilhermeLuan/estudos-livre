CREATE TABLE review_plan (
    id UUID PRIMARY KEY,
    owner_id UUID NOT NULL REFERENCES identity_user (id),
    subject_id UUID NOT NULL REFERENCES subject (id),
    content_id UUID NOT NULL REFERENCES content (id),
    source_session_id UUID REFERENCES study_session (id) ON DELETE SET NULL,
    initial_study_date DATE NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT review_plan_status_valid CHECK (status IN ('ACTIVE'))
);

CREATE UNIQUE INDEX review_plan_owner_content_active_unique_idx
    ON review_plan (owner_id, content_id)
    WHERE status = 'ACTIVE';

CREATE TABLE review_occurrence (
    id UUID PRIMARY KEY,
    plan_id UUID NOT NULL REFERENCES review_plan (id) ON DELETE CASCADE,
    interval_days INTEGER NOT NULL,
    due_date DATE NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'SCHEDULED',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT review_occurrence_interval_positive CHECK (interval_days > 0),
    CONSTRAINT review_occurrence_status_valid CHECK (status IN ('SCHEDULED')),
    CONSTRAINT review_occurrence_plan_interval_unique UNIQUE (plan_id, interval_days)
);

CREATE INDEX review_occurrence_queue_idx
    ON review_occurrence (status, due_date, plan_id);
