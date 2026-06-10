package com.codesage.domain.repos.model;

import com.codesage.domain.auth.model.User;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

/**
 * JPA entity representing a connected GitHub repository.
 */
@Entity
@Table(name = "repositories", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_id", "github_repo_id"})
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Repository {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false, columnDefinition = "UUID")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "github_repo_id", nullable = false)
    private Long githubRepoId;

    @Column(name = "full_name", nullable = false, length = 255)
    private String fullName;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "language", length = 64)
    private String language;

    @Column(name = "is_private", nullable = false)
    @Builder.Default
    private Boolean isPrivate = false;

    @Column(name = "default_branch", nullable = false, length = 64)
    @Builder.Default
    private String defaultBranch = "main";

    @Column(name = "stars_count", nullable = false)
    @Builder.Default
    private Integer starsCount = 0;

    @Column(name = "forks_count", nullable = false)
    @Builder.Default
    private Integer forksCount = 0;

    @Column(name = "open_issues_count", nullable = false)
    @Builder.Default
    private Integer openIssuesCount = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "indexing_status", nullable = false, length = 32)
    @Builder.Default
    private IndexingStatus indexingStatus = IndexingStatus.PENDING;

    @Column(name = "last_synced_at", columnDefinition = "TIMESTAMPTZ")
    private Instant lastSyncedAt;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "TIMESTAMPTZ")
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false, columnDefinition = "TIMESTAMPTZ")
    private Instant updatedAt;
}
