ALTER TABLE user_credentials
    ADD COLUMN IF NOT EXISTS email_verification_token_hash VARCHAR(512),
    ADD COLUMN IF NOT EXISTS email_verification_token_expires_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS email_verification_requested_at TIMESTAMP;
