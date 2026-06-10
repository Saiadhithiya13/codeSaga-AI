-- ─────────────────────────────────────────────────────────────────────────────
-- CodeSage AI — V3 Repository Integration
-- Adds tables for connected GitHub repositories, contributor snapshots, and metrics.
-- ─────────────────────────────────────────────────────────────────────────────

CREATE TABLE IF NOT EXISTS repositories (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id           UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    github_repo_id    BIGINT NOT NULL,
    full_name         VARCHAR(255) NOT NULL,
    name              VARCHAR(255) NOT NULL,
    description       TEXT,
    language          VARCHAR(64),
    is_private        BOOLEAN NOT NULL DEFAULT FALSE,
    default_branch    VARCHAR(64) NOT NULL DEFAULT 'main',
    stars_count       INTEGER NOT NULL DEFAULT 0,
    forks_count       INTEGER NOT NULL DEFAULT 0,
    open_issues_count INTEGER NOT NULL DEFAULT 0,
    last_synced_at    TIMESTAMPTZ,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_user_github_repo UNIQUE (user_id, github_repo_id)
);

CREATE INDEX IF NOT EXISTS idx_repositories_user_id
ON repositories(user_id);

CREATE INDEX IF NOT EXISTS idx_repositories_full_name
ON repositories(full_name);

CREATE TABLE IF NOT EXISTS repository_contributors (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    repository_id   UUID NOT NULL REFERENCES repositories(id) ON DELETE CASCADE,
    username        VARCHAR(128) NOT NULL,
    avatar_url      VARCHAR(512),
    contributions   INTEGER NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_repo_contributor UNIQUE (repository_id, username)
);

CREATE INDEX IF NOT EXISTS idx_repo_contributors_repo_id
ON repository_contributors(repository_id);
