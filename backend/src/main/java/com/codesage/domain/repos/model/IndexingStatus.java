package com.codesage.domain.repos.model;

/**
 * Status of repository processing pipeline.
 */
public enum IndexingStatus {
    PENDING,
    INDEXING,
    INDEXED,
    FAILED
}
