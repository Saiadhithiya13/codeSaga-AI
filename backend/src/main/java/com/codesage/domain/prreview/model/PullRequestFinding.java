package com.codesage.domain.prreview.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.GenericGenerator;

import java.util.UUID;

@Entity
@Table(name = "pull_request_findings")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PullRequestFinding {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "review_id", nullable = false)
    private PullRequestReview review;

    @Column(name = "file_path", nullable = false, length = 1024)
    private String filePath;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 64)
    private PrReviewCategory category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private PrReviewSeverity severity;

    @Column(name = "confidence_score", nullable = false)
    @Builder.Default
    private Integer confidenceScore = 100;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String recommendation;
}
