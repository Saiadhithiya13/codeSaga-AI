-- Add retry count and error message to code_chunks for robust dead-letter handling

ALTER TABLE code_chunks ADD COLUMN retry_count INT NOT NULL DEFAULT 0;
ALTER TABLE code_chunks ADD COLUMN last_error TEXT;
