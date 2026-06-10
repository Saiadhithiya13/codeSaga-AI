package com.codesage.domain.repos.model;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

/**
 * JPA entity representing a snapshot of repository metrics.
 * Captured during each sync to build historical trends for dashboards.
 */
@Entity
@Table(name = "repository_metrics")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RepositoryMetric {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false, columnDefinition = "UUID")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "repository_id", nullable = false)
    private Repository repository;

    @Column(name = "contributor_count", nullable = false)
    @Builder.Default
    private Integer contributorCount = 0;

    @Column(name = "open_pr_count", nullable = false)
    @Builder.Default
    private Integer openPrCount = 0;

    @Column(name = "open_issue_count", nullable = false)
    @Builder.Default
    private Integer openIssueCount = 0;

    @Column(name = "stars_count", nullable = false)
    @Builder.Default
    private Integer starsCount = 0;

    @Column(name = "forks_count", nullable = false)
    @Builder.Default
    private Integer forksCount = 0;

    @CreatedDate
    @Column(name = "recorded_at", nullable = false, updatable = false, columnDefinition = "TIMESTAMPTZ")
    private Instant recordedAt;
}
