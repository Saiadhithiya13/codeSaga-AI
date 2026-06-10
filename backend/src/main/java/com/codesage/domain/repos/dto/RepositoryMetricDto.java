package com.codesage.domain.repos.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;

@Schema(description = "Repository Metric DTO")
public record RepositoryMetricDto(
        UUID id,
        Integer contributorCount,
        Integer openPrCount,
        Integer openIssueCount,
        Integer starsCount,
        Integer forksCount,
        Instant recordedAt
) {}
