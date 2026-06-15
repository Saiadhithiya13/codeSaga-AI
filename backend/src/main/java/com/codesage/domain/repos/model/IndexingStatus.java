package com.codesage.domain.repos.model;

/**
 * Status of repository processing pipeline.
 *
 * <p>State machine:
 * <pre>
 *   PENDING → INDEXING → CHUNKED → EMBEDDING → INDEXED
 *                      ↘ FAILED             ↘ EMBEDDING_FAILED
 * </pre>
 * <ul>
 *   <li>CHUNKED   — files downloaded, RepositoryFile + CodeChunk rows persisted</li>
 *   <li>EMBEDDING — vector embedding pipeline running asynchronously</li>
 *   <li>INDEXED   — all chunks embedded in ChromaDB; full RAG pipeline available</li>
 * </ul>
 */
public enum IndexingStatus {
    PENDING,
    INDEXING,
    CHUNKED,
    EMBEDDING,
    INDEXED,
    EMBEDDING_FAILED,
    FAILED
}
