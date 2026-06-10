-- ─────────────────────────────────────────────────────────────────────────────
-- CodeSage AI — V1 Initial Schema
-- Architecture Spec: UUID PKs, TIMESTAMPTZ everywhere, no Hibernate DDL
-- Covers core tables to establish the baseline.
-- ─────────────────────────────────────────────────────────────────────────────

-- Enable UUID generation extension
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- ─────────────────────────────────────────────────────────────────────────────
-- users
-- Created by Sprint 2 (GitHub OAuth). Defined here so foreign keys work.
-- ─────────────────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS users (
    id                  UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    github_id           BIGINT      NOT NULL UNIQUE,
    login               VARCHAR(64) NOT NULL UNIQUE,
    name                VARCHAR(128),
    email               VARCHAR(256),
    avatar_url          TEXT,
    github_access_token TEXT,                       -- AES-256 encrypted (Sprint 3)
    role                VARCHAR(32) NOT NULL DEFAULT 'USER',
    is_active           BOOLEAN     NOT NULL DEFAULT TRUE,
    last_login_at       TIMESTAMPTZ,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_users_github_id ON users(github_id);
CREATE INDEX idx_users_login     ON users(login);

-- ─────────────────────────────────────────────────────────────────────────────
-- audit_log
-- Immutable audit trail for sensitive operations.
-- Architecture Spec §Security: Audit logging for sensitive operations.
-- ─────────────────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS audit_log (
    id                  UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id             UUID        REFERENCES users(id),
    action              VARCHAR(128) NOT NULL,   -- e.g. USER_LOGIN, REPO_CONNECTED
    entity_type         VARCHAR(64),
    entity_id           UUID,
    old_value           JSONB,
    new_value           JSONB,
    ip_address          VARCHAR(45),             -- IPv4 or IPv6
    user_agent          TEXT,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW()
    -- No updated_at — audit records are immutable
);

CREATE INDEX idx_audit_log_user_id     ON audit_log(user_id);
CREATE INDEX idx_audit_log_action      ON audit_log(action);
CREATE INDEX idx_audit_log_entity      ON audit_log(entity_type, entity_id);
CREATE INDEX idx_audit_log_created_at  ON audit_log(created_at DESC);
