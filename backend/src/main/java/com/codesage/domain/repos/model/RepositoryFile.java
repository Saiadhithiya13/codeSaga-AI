package com.codesage.domain.repos.model;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

/**
 * JPA entity representing a scanned and indexed file within a repository.
 */
@Entity
@Table(name = "repository_files", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"repository_id", "path"})
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RepositoryFile {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false, columnDefinition = "UUID")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "repository_id", nullable = false)
    private Repository repository;

    @Column(name = "path", nullable = false, length = 1024)
    private String path;

    @Column(name = "extension", length = 32)
    private String extension;

    @Column(name = "size_bytes", nullable = false)
    @Builder.Default
    private Long sizeBytes = 0L;

    @Column(name = "sha_hash", nullable = false, length = 64)
    private String shaHash;

    @Column(name = "last_indexed_at", columnDefinition = "TIMESTAMPTZ")
    private Instant lastIndexedAt;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "TIMESTAMPTZ")
    private Instant createdAt;
}
