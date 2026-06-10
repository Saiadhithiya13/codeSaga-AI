-- ─────────────────────────────────────────────────────────────────────────────
-- CodeSage AI — V4 Repository Ingestion
-- Adds indexing_status to repositories and tables for indexed files and chunks.
-- ─────────────────────────────────────────────────────────────────────────────

ALTER TABLE repositories 
ADD COLUMN indexing_status VARCHAR(32) NOT NULL DEFAULT 'PENDING';

CREATE TABLE IF NOT EXISTS repository_files (
    id                UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    repository_id     UUID         NOT NULL REFERENCES repositories(id) ON DELETE CASCADE,
    path              VARCHAR(1024) NOT NULL, -- e.g. "src/main/java/App.java"
    extension         VARCHAR(32),            -- e.g. "java"
    size_bytes        BIGINT       NOT NULL DEFAULT 0,
    sha_hash          VARCHAR(64)  NOT NULL,  -- file content hash to detect changes
    last_indexed_at   TIMESTAMPTZ,
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    
    CONSTRAINT uq_repo_file_path UNIQUE (repository_id, path)
);

CREATE INDEX idx_repository_files_repo_id ON repository_files(repository_id);
CREATE INDEX idx_repository_files_extension ON repository_files(extension);

CREATE TABLE IF NOT EXISTS code_chunks (
    id                 UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    repository_file_id UUID         NOT NULL REFERENCES repository_files(id) ON DELETE CASCADE,
    chunk_index        INTEGER      NOT NULL,
    start_line         INTEGER      NOT NULL,
    end_line           INTEGER      NOT NULL,
    content            TEXT         NOT NULL,
    content_hash       VARCHAR(64)  NOT NULL,
    token_estimate     INTEGER      NOT NULL DEFAULT 0,
    created_at         TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_code_chunks_file_id ON code_chunks(repository_file_id);
CREATE INDEX idx_code_chunks_content_hash ON code_chunks(content_hash);
