ALTER TABLE user_credentials
    ADD COLUMN IF NOT EXISTS password_reset_token_hash VARCHAR(512),
    ADD COLUMN IF NOT EXISTS password_reset_token_expires_at TIMESTAMP;

CREATE INDEX IF NOT EXISTS idx_user_credentials_password_reset_token_hash
    ON user_credentials(password_reset_token_hash);
