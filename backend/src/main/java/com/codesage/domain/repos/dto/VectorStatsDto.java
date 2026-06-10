package com.codesage.domain.repos.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Vector Embeddings Stats")
public record VectorStatsDto(
        long totalChunks,
        long embeddedChunks,
        long pendingChunks,
        long failedChunks
) {}
