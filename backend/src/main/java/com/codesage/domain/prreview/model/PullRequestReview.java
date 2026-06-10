package com.codesage.domain.prreview.model;

import com.codesage.domain.repos.model.Repository;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.GenericGenerator;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "pull_request_reviews")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PullRequestReview {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "repository_id", nullable = false)
    private Repository repository;

    @Column(name = "github_pr_id", nullable = false, length = 64)
    private String githubPrId;

    @Column(nullable = false, length = 512)
    private String title;

    @Column(name = "review_summary", nullable = false, columnDefinition = "TEXT")
    private String reviewSummary;

    @Column(name = "risk_score", nullable = false)
    private Integer riskScore;

    @OneToMany(mappedBy = "review", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<PullRequestFinding> findings = new ArrayList<>();

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "TIMESTAMPTZ")
    private Instant createdAt;

    public void addFinding(PullRequestFinding finding) {
        findings.add(finding);
        finding.setReview(this);
    }
}
