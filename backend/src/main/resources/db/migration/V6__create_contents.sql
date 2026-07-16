CREATE TABLE content (
    id UUID PRIMARY KEY,
    subject_id UUID NOT NULL REFERENCES subject (id),
    name VARCHAR(120) NOT NULL,
    archived_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT content_name_normalized CHECK (
        name = REGEXP_REPLACE(BTRIM(name), '[[:space:]]+', ' ', 'g')
        AND CHAR_LENGTH(name) BETWEEN 1 AND 120
    )
);

CREATE UNIQUE INDEX content_subject_active_name_unique_idx
    ON content (subject_id, LOWER(name))
    WHERE archived_at IS NULL;

CREATE INDEX content_subject_archived_name_idx
    ON content (subject_id, archived_at, LOWER(name), id);
