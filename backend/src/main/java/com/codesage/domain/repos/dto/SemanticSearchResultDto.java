package com.codesage.domain.repos.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

@Schema(description = "Semantic Search Result DTO")
public record SemanticSearchResultDto(
        UUID chunkId,
        UUID repositoryFileId,
        String filePath,
        Integer chunkIndex,
        String language,
        String content,
        Double similarityScore
) {}
