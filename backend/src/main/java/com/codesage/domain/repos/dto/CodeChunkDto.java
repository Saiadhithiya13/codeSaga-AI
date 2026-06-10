package com.codesage.domain.repos.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

@Schema(description = "Code Chunk DTO")
public record CodeChunkDto(
        UUID id,
        Integer chunkIndex,
        Integer startLine,
        Integer endLine,
        String content,
        String contentHash,
        Integer tokenEstimate
) {}
