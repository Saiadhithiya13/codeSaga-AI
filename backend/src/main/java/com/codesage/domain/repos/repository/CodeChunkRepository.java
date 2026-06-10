package com.codesage.domain.repos.repository;

import com.codesage.domain.repos.model.CodeChunk;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface CodeChunkRepository extends JpaRepository<CodeChunk, UUID> {
    
    List<CodeChunk> findByRepositoryFileIdOrderByChunkIndexAsc(UUID repositoryFileId);

    @Query("SELECT c FROM CodeChunk c WHERE c.repositoryFile.repository.id = :repositoryId ORDER BY c.createdAt ASC")
    List<CodeChunk> findByRepositoryId(@Param("repositoryId") UUID repositoryId);

    @Query("SELECT c FROM CodeChunk c JOIN FETCH c.repositoryFile rf JOIN FETCH rf.repository r WHERE r.id = :repositoryId AND c.embeddingStatus IN :statuses ORDER BY c.chunkIndex ASC")
    List<CodeChunk> findByRepositoryIdAndEmbeddingStatusIn(
            @Param("repositoryId") UUID repositoryId, 
            @Param("statuses") List<com.codesage.domain.repos.model.EmbeddingStatus> statuses
    );

    @Query("SELECT c FROM CodeChunk c JOIN FETCH c.repositoryFile rf JOIN FETCH rf.repository r WHERE c.embeddingStatus IN :statuses ORDER BY c.createdAt ASC")
    List<CodeChunk> findByEmbeddingStatusIn(
            @Param("statuses") List<com.codesage.domain.repos.model.EmbeddingStatus> statuses
    );
}
