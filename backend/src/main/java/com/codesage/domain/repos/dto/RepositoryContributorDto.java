package com.codesage.domain.repos.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

@Schema(description = "Repository Contributor DTO")
public record RepositoryContributorDto(
        UUID id,
        String username,
        String avatarUrl,
        Integer contributions
) {}
