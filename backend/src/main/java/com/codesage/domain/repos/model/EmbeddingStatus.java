package com.codesage.domain.repos.model;

/**
 * Status of the vector embedding process for a code chunk.
 */
public enum EmbeddingStatus {
    PENDING,
    PROCESSING,
    COMPLETED,
    FAILED,
    DEAD_LETTER
}
