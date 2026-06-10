package com.codesage.domain.health.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Map;

@Schema(description = "Global diagnostic statistics for the admin dashboard")
public record DiagnosticsDto(
        @Schema(description = "Total number of connected repositories")
        long totalRepositories,
        
        @Schema(description = "Total number of indexed files")
        long totalFiles,
        
        @Schema(description = "Total number of code chunks")
        long totalChunks,
        
        @Schema(description = "Count of chunks by embedding status")
        Map<String, Long> chunkStatusCounts,
        
        @Schema(description = "Total number of ChromaDB collections")
        long chromaCollections
) {}
