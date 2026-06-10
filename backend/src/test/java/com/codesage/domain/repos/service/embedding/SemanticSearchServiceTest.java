package com.codesage.domain.repos.service.embedding;

import com.codesage.domain.repos.dto.SemanticSearchResultDto;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.EmbeddingStore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SemanticSearchServiceTest {

    @Mock
    private EmbeddingModel embeddingModel;

    @Mock
    private ChromaDbClient chromaDbClient;

    @Mock
    private EmbeddingStore<TextSegment> embeddingStore;

    @InjectMocks
    private SemanticSearchService searchService;

    @Test
    void search_returnsMappedResults() {
        // Arrange
        UUID repoId = UUID.randomUUID();
        String query = "test query";

        Embedding mockEmbedding = new Embedding(new float[]{0.1f, 0.2f});
        when(embeddingModel.embed(query)).thenReturn(dev.langchain4j.model.output.Response.from(mockEmbedding));

        when(chromaDbClient.getStoreForRepository(repoId)).thenReturn(embeddingStore);

        Metadata metadata = new Metadata();
        metadata.put("chunk_id", UUID.randomUUID().toString());
        metadata.put("repository_file_id", UUID.randomUUID().toString());
        metadata.put("file_path", "src/main/Test.java");
        metadata.put("chunk_index", "0");
        metadata.put("language", "java");
        TextSegment segment = TextSegment.from("test code", metadata);
        
        EmbeddingMatch<TextSegment> match = new EmbeddingMatch<>(0.95, "mock-id", mockEmbedding, segment);
        EmbeddingSearchResult<TextSegment> searchResult = new EmbeddingSearchResult<>(List.of(match));
        
        when(embeddingStore.search(any(EmbeddingSearchRequest.class))).thenReturn(searchResult);

        // Act
        List<SemanticSearchResultDto> results = searchService.search(repoId, query, 5);

        // Assert
        assertEquals(1, results.size());
        SemanticSearchResultDto dto = results.get(0);
        assertEquals("src/main/Test.java", dto.filePath());
        assertEquals("java", dto.language());
        assertEquals("test code", dto.content());
        assertEquals(0.95, dto.similarityScore());
    }
}
