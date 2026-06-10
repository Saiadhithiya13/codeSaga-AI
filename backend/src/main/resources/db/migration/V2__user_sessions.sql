-- ─────────────────────────────────────────────────────────────────────────────
-- CodeSage AI — V2 Auth: User Sessions
-- Tracks active refresh token families for revocation and theft detection.
-- Refresh tokens themselves live in Redis (session:refresh:{jti}).
-- This table enables: forced logout, device management, theft detection.
-- ─────────────────────────────────────────────────────────────────────────────

CREATE TABLE IF NOT EXISTS user_sessions (
    id              UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    jti             VARCHAR(64) NOT NULL UNIQUE,        -- JWT ID (refresh token identifier)
    token_family    UUID        NOT NULL DEFAULT gen_random_uuid(), -- Family for rotation chain
    device_hint     VARCHAR(256),                       -- User-Agent snippet (first 256 chars)
    ip_address      VARCHAR(45),                        -- IPv4 or IPv6
    expires_at      TIMESTAMPTZ NOT NULL,
    is_revoked      BOOLEAN     NOT NULL DEFAULT FALSE,
    revoked_reason  VARCHAR(64),                        -- LOGOUT, THEFT_DETECTED, EXPIRED
    last_used_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_user_sessions_user_id      ON user_sessions(user_id);
CREATE INDEX idx_user_sessions_jti          ON user_sessions(jti);
CREATE INDEX idx_user_sessions_token_family ON user_sessions(token_family);
CREATE INDEX idx_user_sessions_is_revoked   ON user_sessions(is_revoked) WHERE is_revoked = FALSE;
