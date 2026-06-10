package com.codesage.domain.repos.model;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

/**
 * JPA entity representing a deterministic chunk of source code.
 */
@Entity
@Table(name = "code_chunks")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CodeChunk {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false, columnDefinition = "UUID")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "repository_file_id", nullable = false)
    private RepositoryFile repositoryFile;

    @Column(name = "chunk_index", nullable = false)
    private Integer chunkIndex;

    @Column(name = "start_line", nullable = false)
    private Integer startLine;

    @Column(name = "end_line", nullable = false)
    private Integer endLine;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "content_hash", nullable = false, length = 64)
    private String contentHash;

    @Column(name = "token_estimate", nullable = false)
    @Builder.Default
    private Integer tokenEstimate = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "embedding_status", nullable = false, length = 32)
    @Builder.Default
    private EmbeddingStatus embeddingStatus = EmbeddingStatus.PENDING;

    @Column(name = "embedded_at", columnDefinition = "TIMESTAMPTZ")
    private Instant embeddedAt;

    @Column(name = "retry_count", nullable = false)
    @Builder.Default
    private Integer retryCount = 0;

    @Column(name = "last_error", columnDefinition = "TEXT")
    private String lastError;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "TIMESTAMPTZ")
    private Instant createdAt;
}
