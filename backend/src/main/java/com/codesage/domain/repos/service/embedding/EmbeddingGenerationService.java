package com.codesage.domain.repos.service.embedding;

import com.codesage.domain.repos.model.CodeChunk;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Service responsible for generating embeddings using the AI model.
 */
@Log4j2
@Service
@RequiredArgsConstructor
public class EmbeddingGenerationService {

    private final EmbeddingModel embeddingModel;

    /**
     * Generates embeddings for a batch of text segments with retry support.
     * LangChain4j handles the batching depending on the model's limits.
     */
    @Retryable(
            retryFor = {Exception.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 2000, multiplier = 2)
    )
    public List<Embedding> generateEmbeddings(List<TextSegment> segments) {
        log.debug("Generating embeddings for batch of {} segments", segments.size());
        return embeddingModel.embedAll(segments).content();
    }
}
