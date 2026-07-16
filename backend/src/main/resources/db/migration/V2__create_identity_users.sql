CREATE TABLE identity_user (
    id UUID PRIMARY KEY,
    email VARCHAR(320) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    time_zone VARCHAR(255) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT identity_user_email_normalized CHECK (email = LOWER(BTRIM(email))),
    CONSTRAINT identity_user_email_unique UNIQUE (email)
);
