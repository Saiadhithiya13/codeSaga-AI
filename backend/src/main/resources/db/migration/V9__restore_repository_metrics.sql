-- ─────────────────────────────────────────────────────────────────────────────
-- CodeSage AI — V9 Restore Repository Metrics
-- Restores the repository metrics table that was accidentally removed from V1.
-- ─────────────────────────────────────────────────────────────────────────────

CREATE TABLE IF NOT EXISTS repository_metrics (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    repository_id     UUID NOT NULL REFERENCES repositories(id) ON DELETE CASCADE,
    contributor_count INTEGER NOT NULL DEFAULT 0,
    open_pr_count     INTEGER NOT NULL DEFAULT 0,
    open_issue_count  INTEGER NOT NULL DEFAULT 0,
    stars_count       INTEGER NOT NULL DEFAULT 0,
    forks_count       INTEGER NOT NULL DEFAULT 0,
    recorded_at       TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_repo_metrics_repo_id
ON repository_metrics(repository_id);
