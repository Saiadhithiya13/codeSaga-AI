package com.codesage.domain.repos.model;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

/**
 * JPA entity representing a GitHub contributor snapshot for a repository.
 */
@Entity
@Table(name = "repository_contributors", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"repository_id", "username"})
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RepositoryContributor {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false, columnDefinition = "UUID")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "repository_id", nullable = false)
    private Repository repository;

    @Column(name = "username", nullable = false, length = 128)
    private String username;

    @Column(name = "avatar_url", length = 512)
    private String avatarUrl;

    @Column(name = "contributions", nullable = false)
    @Builder.Default
    private Integer contributions = 0;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "TIMESTAMPTZ")
    private Instant createdAt;
}
