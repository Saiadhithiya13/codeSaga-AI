package com.codesage.domain.repos.repository;

import com.codesage.domain.repos.model.CodeChunk;
import com.codesage.domain.repos.model.EmbeddingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface CodeChunkRepository extends JpaRepository<CodeChunk, UUID> {

    List<CodeChunk> findByRepositoryFileIdOrderByChunkIndexAsc(UUID repositoryFileId);

    @Query("SELECT c FROM CodeChunk c WHERE c.repositoryFile.repository.id = :repositoryId ORDER BY c.createdAt ASC")
    List<CodeChunk> findByRepositoryId(@Param("repositoryId") UUID repositoryId);

    /**
     * Fetches chunks for embedding with JOIN FETCH to avoid LazyInitializationException.
     * The repositoryFile and repository associations are loaded eagerly within the calling session.
     */
    @Query("SELECT c FROM CodeChunk c JOIN FETCH c.repositoryFile rf JOIN FETCH rf.repository r WHERE r.id = :repositoryId AND c.embeddingStatus IN :statuses ORDER BY c.chunkIndex ASC")
    List<CodeChunk> findByRepositoryIdAndEmbeddingStatusIn(
            @Param("repositoryId") UUID repositoryId,
            @Param("statuses") List<EmbeddingStatus> statuses
    );

    /**
     * Fetches all PENDING/FAILED chunks globally with JOIN FETCH for the retry scheduler.
     */
    @Query("SELECT c FROM CodeChunk c JOIN FETCH c.repositoryFile rf JOIN FETCH rf.repository r WHERE c.embeddingStatus IN :statuses ORDER BY c.createdAt ASC")
    List<CodeChunk> findByEmbeddingStatusIn(
            @Param("statuses") List<EmbeddingStatus> statuses
    );

    // ─── Bulk mutation queries — avoid loading entities for pure status updates ─────────────────

    @Modifying
    @Query("UPDATE CodeChunk c SET c.embeddingStatus = :status WHERE c.id IN :ids")
    void markStatusForIds(@Param("ids") List<UUID> ids, @Param("status") EmbeddingStatus status);

    @Modifying
    @Query("UPDATE CodeChunk c SET c.embeddingStatus = 'COMPLETED', c.embeddedAt = :now, c.lastError = NULL WHERE c.id IN :ids")
    void markCompletedForIds(@Param("ids") List<UUID> ids, @Param("now") Instant now);

    /**
     * Increments retry_count and marks FAILED; promotes to DEAD_LETTER when maxRetries exceeded.
     */
    @Modifying
    @Query("""
            UPDATE CodeChunk c
            SET c.retryCount = c.retryCount + 1,
                c.lastError  = :error,
                c.embeddingStatus = CASE
                    WHEN c.retryCount + 1 >= :maxRetries THEN 'DEAD_LETTER'
                    ELSE 'FAILED'
                END
            WHERE c.id IN :ids
            """)
    void incrementRetryAndMarkFailed(
            @Param("ids") List<UUID> ids,
            @Param("error") String error,
            @Param("maxRetries") int maxRetries
    );

    @Modifying
    @Query("DELETE FROM CodeChunk c WHERE c.repositoryFile.id IN :fileIds")
    void deleteByRepositoryFileIdIn(@Param("fileIds") List<UUID> fileIds);
}
