package com.codesage.domain.repos.service.embedding;

import com.codesage.domain.repos.model.CodeChunk;
import com.codesage.domain.repos.model.EmbeddingStatus;
import com.codesage.domain.repos.model.RepositoryFile;
import com.codesage.domain.repos.repository.CodeChunkRepository;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

@Log4j2
@Service
@RequiredArgsConstructor
public class RepositoryEmbeddingService {

    private final CodeChunkRepository codeChunkRepository;
    private final EmbeddingGenerationService embeddingGenerationService;
    private final ChromaDbClient chromaDbClient;

    private static final int BATCH_SIZE = 100;
    private static final int MAX_RETRIES = 5;

    @Async
    public void startRepositoryEmbeddingAsync(UUID repositoryId) {
        log.info("Starting asynchronous embedding generation for repository {}", repositoryId);

        List<EmbeddingStatus> targetStatuses = List.of(EmbeddingStatus.PENDING, EmbeddingStatus.FAILED);
        List<CodeChunk> unembeddedChunks = codeChunkRepository.findByRepositoryIdAndEmbeddingStatusIn(repositoryId, targetStatuses);

        if (unembeddedChunks.isEmpty()) {
            log.info("No chunks require embedding for repository {}", repositoryId);
            return;
        }

        processChunks(repositoryId, unembeddedChunks);
    }

    @org.springframework.scheduling.annotation.Scheduled(fixedDelayString = "${ai.embedding.retry-interval-ms:60000}")
    public void retryFailedEmbeddings() {
        List<EmbeddingStatus> targetStatuses = List.of(EmbeddingStatus.PENDING, EmbeddingStatus.FAILED);
        List<CodeChunk> unembeddedChunks = codeChunkRepository.findByEmbeddingStatusIn(targetStatuses);

        // Silent return when nothing to do — prevents log spam on empty systems
        if (unembeddedChunks.isEmpty()) {
            return;
        }

        log.info("Running scheduled retry for {} pending/failed chunks...", unembeddedChunks.size());

        // Group by repository ID
        java.util.Map<UUID, List<CodeChunk>> grouped = unembeddedChunks.stream()
                .collect(java.util.stream.Collectors.groupingBy(c -> c.getRepositoryFile().getRepository().getId()));

        for (java.util.Map.Entry<UUID, List<CodeChunk>> entry : grouped.entrySet()) {
            log.info("Retrying {} chunks for repository {}", entry.getValue().size(), entry.getKey());
            processChunks(entry.getKey(), entry.getValue());
        }
    }

    private void processChunks(UUID repositoryId, List<CodeChunk> unembeddedChunks) {
        log.info("Processing {} chunks to embed for repository {}", unembeddedChunks.size(), repositoryId);

        // Mark them all as PROCESSING in DB
        unembeddedChunks.forEach(c -> c.setEmbeddingStatus(EmbeddingStatus.PROCESSING));
        codeChunkRepository.saveAll(unembeddedChunks);

        EmbeddingStore<TextSegment> store = chromaDbClient.getStoreForRepository(repositoryId);

        // Process in batches using virtual threads
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            List<List<CodeChunk>> batches = partition(unembeddedChunks, BATCH_SIZE);
            List<Future<Void>> futures = new ArrayList<>();

            for (List<CodeChunk> batch : batches) {
                futures.add(executor.submit(() -> {
                    processBatch(batch, store);
                    return null;
                }));
            }

            // Wait for all to complete
            for (Future<Void> future : futures) {
                future.get();
            }
        } catch (Exception e) {
            log.error("Error during parallel embedding generation for repository {}", repositoryId, e);
        }

        log.info("Finished embedding pipeline for repository {}", repositoryId);
    }

    private void processBatch(List<CodeChunk> batch, EmbeddingStore<TextSegment> store) {
        try {
            List<TextSegment> segments = batch.stream().map(this::toTextSegment).toList();
            List<Embedding> embeddings = embeddingGenerationService.generateEmbeddings(segments);

            // Insert into ChromaDB
            store.addAll(embeddings, segments);

            // Mark successful
            Instant now = Instant.now();
            batch.forEach(chunk -> {
                chunk.setEmbeddingStatus(EmbeddingStatus.COMPLETED);
                chunk.setEmbeddedAt(now);
                chunk.setLastError(null);
            });
            codeChunkRepository.saveAll(batch);

        } catch (Exception e) {
            log.error("Failed to process embedding batch of size {}", batch.size(), e);
            batch.forEach(chunk -> {
                chunk.setRetryCount(chunk.getRetryCount() + 1);
                chunk.setLastError(e.getMessage());
                if (chunk.getRetryCount() >= MAX_RETRIES) {
                    chunk.setEmbeddingStatus(EmbeddingStatus.DEAD_LETTER);
                } else {
                    chunk.setEmbeddingStatus(EmbeddingStatus.FAILED);
                }
            });
            codeChunkRepository.saveAll(batch);
        }
    }

    private TextSegment toTextSegment(CodeChunk chunk) {
        RepositoryFile file = chunk.getRepositoryFile();
        Metadata metadata = new Metadata();
        metadata.put("repository_id", file.getRepository().getId().toString());
        metadata.put("repository_file_id", file.getId().toString());
        metadata.put("chunk_id", chunk.getId().toString());
        metadata.put("file_path", file.getPath());
        metadata.put("chunk_index", String.valueOf(chunk.getChunkIndex()));
        metadata.put("language", file.getExtension() == null ? "unknown" : file.getExtension());
        metadata.put("content_hash", chunk.getContentHash());
        return TextSegment.from(chunk.getContent(), metadata);
    }

    private <T> List<List<T>> partition(List<T> list, int size) {
        List<List<T>> partitions = new ArrayList<>();
        for (int i = 0; i < list.size(); i += size) {
            partitions.add(new ArrayList<>(list.subList(i, Math.min(list.size(), i + size))));
        }
        return partitions;
    }
}
