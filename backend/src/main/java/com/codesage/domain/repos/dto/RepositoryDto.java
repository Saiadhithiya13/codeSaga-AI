package com.codesage.domain.repos.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;

@Schema(description = "Repository public DTO")
public record RepositoryDto(
        UUID id,
        Long githubRepoId,
        String fullName,
        String name,
        String description,
        String language,
        Boolean isPrivate,
        String defaultBranch,
        Integer starsCount,
        Integer forksCount,
        Integer openIssuesCount,
        Instant lastSyncedAt,
        Instant createdAt
) {}
