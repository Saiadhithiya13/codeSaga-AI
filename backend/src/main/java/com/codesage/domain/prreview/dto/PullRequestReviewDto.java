package com.codesage.domain.prreview.dto;

import java.time.Instant;
import java.util.UUID;

public record PullRequestReviewDto(
        UUID id,
        UUID repositoryId,
        String githubPrId,
        String title,
        String reviewSummary,
        Integer riskScore,
        Instant createdAt
) {}
