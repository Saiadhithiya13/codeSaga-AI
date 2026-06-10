package com.codesage.domain.repos.service.embedding;

import com.codesage.config.ChromaConfig;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.chroma.ChromaEmbeddingStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service to manage ChromaDB collections per repository.
 */
@Log4j2
@Service
@RequiredArgsConstructor
public class ChromaDbClient {

    private final ChromaConfig chromaConfig;
    
    // Cache of EmbeddingStores per repository collection
    private final Map<UUID, EmbeddingStore<TextSegment>> storeCache = new ConcurrentHashMap<>();

    /**
     * Gets or creates an EmbeddingStore mapped to a specific repository's collection.
     */
    public EmbeddingStore<TextSegment> getStoreForRepository(UUID repositoryId) {
        return storeCache.computeIfAbsent(repositoryId, this::createStore);
    }

    private EmbeddingStore<TextSegment> createStore(UUID repositoryId) {
        String collectionName = "repo_" + repositoryId.toString().replace("-", "");
        log.debug("Initializing ChromaEmbeddingStore for collection: {}", collectionName);
        
        return ChromaEmbeddingStore.builder()
                .baseUrl(chromaConfig.getChromaUrl())
                .collectionName(collectionName)
                .timeout(Duration.ofSeconds(60))
                .build();
    }
}
