-- ─────────────────────────────────────────────────────────────────────────────
-- CodeSage AI — V11 Fix embedding_status DEFAULT mismatch
--
-- Root cause: V5 set DEFAULT 'NOT_EMBEDDED' but the Java EmbeddingStatus
-- enum uses PENDING as its default (via @Builder.Default in CodeChunk).
-- The scheduled retry job queries for PENDING and FAILED statuses only,
-- so any rows that were inserted with NOT_EMBEDDED are silently skipped
-- by the embedding pipeline and never processed.
--
-- Fix: migrate all NOT_EMBEDDED rows to PENDING, then change the column
-- default to PENDING so new inserts are picked up immediately.
-- ─────────────────────────────────────────────────────────────────────────────

-- Step 1: Update existing NOT_EMBEDDED rows so they are picked up by the
--         scheduled retry job on the next run.
UPDATE code_chunks
SET embedding_status = 'PENDING'
WHERE embedding_status = 'NOT_EMBEDDED';

-- Step 2: Change the column default to PENDING to match the Java enum default.
ALTER TABLE code_chunks
    ALTER COLUMN embedding_status SET DEFAULT 'PENDING';
