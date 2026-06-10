-- ─────────────────────────────────────────────────────────────────────────────
-- CodeSage AI — V8 AI Pull Request Reviewer
-- Adds PR review tables
-- ─────────────────────────────────────────────────────────────────────────────

CREATE TABLE pull_request_reviews (
    id UUID PRIMARY KEY,
    repository_id UUID NOT NULL REFERENCES repositories(id) ON DELETE CASCADE,
    github_pr_id VARCHAR(64) NOT NULL,
    title VARCHAR(512) NOT NULL,
    review_summary TEXT NOT NULL,
    risk_score INTEGER NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_pr_reviews_repo_id ON pull_request_reviews(repository_id);
CREATE INDEX idx_pr_reviews_github_pr_id ON pull_request_reviews(github_pr_id);

CREATE TABLE pull_request_findings (
    id UUID PRIMARY KEY,
    review_id UUID NOT NULL REFERENCES pull_request_reviews(id) ON DELETE CASCADE,
    file_path VARCHAR(1024) NOT NULL,
    category VARCHAR(64) NOT NULL,
    severity VARCHAR(32) NOT NULL,
    confidence_score INTEGER NOT NULL DEFAULT 100,
    description TEXT NOT NULL,
    recommendation TEXT NOT NULL
);

CREATE INDEX idx_pr_findings_review_id ON pull_request_findings(review_id);
