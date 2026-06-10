package com.codesage.domain.repos.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;

@Schema(description = "Repository File DTO")
public record RepositoryFileDto(
        UUID id,
        String path,
        String extension,
        Long sizeBytes,
        String shaHash,
        Instant lastIndexedAt
) {}
