package com.codesage.domain.repos.service.embedding;

import com.codesage.domain.repos.dto.SemanticSearchResultDto;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/**
 * Service to execute semantic searches against a repository's ChromaDB collection.
 */
@Log4j2
@Service
@RequiredArgsConstructor
public class SemanticSearchService {

    private final EmbeddingModel embeddingModel;
    private final ChromaDbClient chromaDbClient;

    /**
     * Executes a semantic search query.
     */
    public List<SemanticSearchResultDto> search(UUID repositoryId, String query, int maxResults) {
        log.info("Executing semantic search for repository {}: '{}'", repositoryId, query);

        // 1. Generate embedding for query
        Embedding queryEmbedding = embeddingModel.embed(query).content();

        // 2. Search ChromaDB
        EmbeddingStore<TextSegment> store = chromaDbClient.getStoreForRepository(repositoryId);
        
        EmbeddingSearchRequest searchRequest = EmbeddingSearchRequest.builder()
                .queryEmbedding(queryEmbedding)
                .maxResults(maxResults)
                // We could add minScore if needed
                .build();

        List<EmbeddingMatch<TextSegment>> matches = store.search(searchRequest).matches();

        // 3. Map to DTOs
        return matches.stream()
                .map(this::toDto)
                .toList();
    }

    private SemanticSearchResultDto toDto(EmbeddingMatch<TextSegment> match) {
        Metadata metadata = match.embedded().metadata();
        return new SemanticSearchResultDto(
                UUID.fromString(metadata.getString("chunk_id")),
                UUID.fromString(metadata.getString("repository_file_id")),
                metadata.getString("file_path"),
                Integer.parseInt(metadata.getString("chunk_index")),
                metadata.getString("language"),
                match.embedded().text(),
                match.score()
        );
    }
}
