-- ─────────────────────────────────────────────────────────────────────────────
-- CodeSage AI — V12 IndexingStatus pipeline states
--
-- Adds CHUNKED, EMBEDDING, EMBEDDING_FAILED to the indexing_status column.
-- The existing VARCHAR(32) column accepts these new string values without a
-- schema change (Postgres stores enum values as VARCHAR in our setup).
--
-- Also resets any stale PROCESSING chunks to FAILED so the retry scheduler
-- picks them up on the next run (leftover from JVM restarts mid-embedding).
-- ─────────────────────────────────────────────────────────────────────────────

-- Reset stale PROCESSING chunks → FAILED so the retry scheduler handles them.
-- These are chunks that were mid-flight when the JVM was killed.
UPDATE code_chunks
SET embedding_status = 'FAILED',
    last_error       = 'Reset by V12 migration: JVM was restarted during embedding'
WHERE embedding_status = 'PROCESSING';

-- Reset any repositories stuck in EMBEDDING or INDEXING state
-- (mid-flight when JVM restarted) back to CHUNKED so re-indexing can be triggered.
UPDATE repositories
SET indexing_status = 'CHUNKED'
WHERE indexing_status = 'EMBEDDING'
   OR indexing_status = 'INDEXING';
