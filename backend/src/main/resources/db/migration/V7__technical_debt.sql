-- ─────────────────────────────────────────────────────────────────────────────
-- CodeSage AI — V7 Technical Debt Analyzer
-- Adds technical debt reports and findings tables
-- ─────────────────────────────────────────────────────────────────────────────

CREATE TABLE technical_debt_reports (
    id UUID PRIMARY KEY,
    repository_id UUID NOT NULL REFERENCES repositories(id) ON DELETE CASCADE,
    overall_score INTEGER NOT NULL,
    maintainability_score INTEGER NOT NULL,
    complexity_score INTEGER NOT NULL,
    duplication_score INTEGER NOT NULL,
    ai_assessment TEXT,
    generated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_td_reports_repo_id ON technical_debt_reports(repository_id);

CREATE TABLE technical_debt_findings (
    id UUID PRIMARY KEY,
    report_id UUID NOT NULL REFERENCES technical_debt_reports(id) ON DELETE CASCADE,
    file_path VARCHAR(1024) NOT NULL,
    category VARCHAR(64) NOT NULL,
    severity VARCHAR(32) NOT NULL,
    description TEXT NOT NULL,
    recommendation TEXT NOT NULL
);

CREATE INDEX idx_td_findings_report_id ON technical_debt_findings(report_id);
