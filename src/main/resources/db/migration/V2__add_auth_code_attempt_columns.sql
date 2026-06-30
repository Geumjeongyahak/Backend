ALTER TABLE user_credentials
    ADD COLUMN IF NOT EXISTS password_reset_requested_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS password_reset_failed_attempts INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS email_verification_failed_attempts INTEGER NOT NULL DEFAULT 0;
