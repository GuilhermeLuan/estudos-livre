CREATE TABLE subject (
    id UUID PRIMARY KEY,
    owner_id UUID NOT NULL REFERENCES identity_user (id),
    name VARCHAR(120) NOT NULL,
    archived_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT subject_name_normalized CHECK (
        name = BTRIM(name)
        AND CHAR_LENGTH(name) BETWEEN 1 AND 120
    )
);

CREATE INDEX subject_owner_archived_name_idx
    ON subject (owner_id, archived_at, LOWER(name), id);
