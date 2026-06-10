package com.codesage.domain.debt.dto;

import java.time.Instant;
import java.util.UUID;

public record TechnicalDebtReportDto(
        UUID id,
        UUID repositoryId,
        Integer overallScore,
        Integer maintainabilityScore,
        Integer complexityScore,
        Integer duplicationScore,
        String aiAssessment,
        Instant generatedAt
) {}
