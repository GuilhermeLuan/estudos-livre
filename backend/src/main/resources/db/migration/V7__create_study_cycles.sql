CREATE TABLE study_cycle (
    id UUID PRIMARY KEY,
    owner_id UUID NOT NULL REFERENCES identity_user (id),
    name VARCHAR(120) NOT NULL,
    mode VARCHAR(16) NOT NULL DEFAULT 'CUSTOM',
    status VARCHAR(16) NOT NULL DEFAULT 'DRAFT',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT study_cycle_name_normalized CHECK (
        name = BTRIM(name)
        AND CHAR_LENGTH(name) BETWEEN 1 AND 120
    ),
    CONSTRAINT study_cycle_mode_valid CHECK (mode IN ('CUSTOM', 'SUGGESTED')),
    CONSTRAINT study_cycle_status_valid CHECK (status IN ('DRAFT', 'INACTIVE', 'ACTIVE'))
);

CREATE INDEX study_cycle_owner_name_idx
    ON study_cycle (owner_id, LOWER(name), id);

CREATE TABLE study_cycle_stage (
    id UUID PRIMARY KEY,
    cycle_id UUID NOT NULL REFERENCES study_cycle (id),
    subject_id UUID NOT NULL REFERENCES subject (id),
    position INTEGER NOT NULL,
    target_minutes INTEGER NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT study_cycle_stage_position_positive CHECK (position > 0),
    CONSTRAINT study_cycle_stage_target_valid CHECK (
        target_minutes > 0
        AND MOD(target_minutes, 5) = 0
    ),
    CONSTRAINT study_cycle_stage_position_unique UNIQUE (cycle_id, position)
);

CREATE INDEX study_cycle_stage_cycle_position_idx
    ON study_cycle_stage (cycle_id, position);
