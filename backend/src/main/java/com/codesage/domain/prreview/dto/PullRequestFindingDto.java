package com.codesage.domain.prreview.dto;

import com.codesage.domain.prreview.model.PrReviewCategory;
import com.codesage.domain.prreview.model.PrReviewSeverity;

import java.util.UUID;

public record PullRequestFindingDto(
        UUID id,
        String filePath,
        PrReviewCategory category,
        PrReviewSeverity severity,
        Integer confidenceScore,
        String description,
        String recommendation
) {}
