CREATE TABLE password_reset_token (
    token_hash CHAR(64) PRIMARY KEY,
    user_id UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT password_reset_token_user_unique UNIQUE (user_id),
    CONSTRAINT password_reset_token_user_fk FOREIGN KEY (user_id)
        REFERENCES identity_user(id) ON DELETE CASCADE,
    CONSTRAINT password_reset_token_expiration_after_creation CHECK (expires_at > created_at)
);

CREATE INDEX password_reset_token_expiry_idx ON password_reset_token (expires_at);
