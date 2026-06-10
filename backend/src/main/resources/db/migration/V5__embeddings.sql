-- ─────────────────────────────────────────────────────────────────────────────
-- CodeSage AI — V5 Embeddings & Vector Search Foundation
-- Adds embedding tracking fields to code_chunks
-- ─────────────────────────────────────────────────────────────────────────────

ALTER TABLE code_chunks 
ADD COLUMN embedding_status VARCHAR(32) NOT NULL DEFAULT 'NOT_EMBEDDED',
ADD COLUMN embedded_at TIMESTAMPTZ;

CREATE INDEX idx_code_chunks_embedding_status ON code_chunks(embedding_status);
