package com.codesage.domain.repos.service.embedding;

import com.codesage.domain.repos.model.CodeChunk;
import com.codesage.domain.repos.model.EmbeddingStatus;
import com.codesage.domain.repos.model.IndexingStatus;
import com.codesage.domain.repos.model.RepositoryFile;
import com.codesage.domain.repos.repository.CodeChunkRepository;
import com.codesage.domain.repos.repository.RepositoryRepository;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

@Log4j2
@Service
@RequiredArgsConstructor
public class RepositoryEmbeddingService {

    private final CodeChunkRepository codeChunkRepository;
    private final EmbeddingGenerationService embeddingGenerationService;
    private final ChromaDbClient chromaDbClient;
    private final RepositoryRepository repositoryRepository;

    private static final int BATCH_SIZE = 100;
    private static final int MAX_RETRIES = 5;

    /**
     * Eagerly loads all PENDING/FAILED chunks for the repository (with repositoryFile and
     * repository associations already fetched via JOIN FETCH in the query), converts them
     * to TextSegments — all within this @Transactional method before any virtual threads
     * are spawned — then hands the pre-materialised data to the async embedding pipeline.
     *
     * <p><strong>Root cause of FAILED embeddings:</strong> {@code toTextSegment()} accesses
     * {@code chunk.getRepositoryFile()} which is {@code FetchType.LAZY}. When called from a
     * virtual thread, Hibernate's {@code ThreadLocal}-bound session is not available, causing
     * {@code LazyInitializationException}. Fix: materialise all lazy data inside this
     * transaction, then pass value-only {@link TextSegment} objects (no JPA proxies) to threads.
     */
    @Async
    @Transactional
    public void startRepositoryEmbeddingAsync(UUID repositoryId) {
        log.info("Starting asynchronous embedding generation for repository {}", repositoryId);

        List<EmbeddingStatus> targetStatuses = List.of(EmbeddingStatus.PENDING, EmbeddingStatus.FAILED);
        // JOIN FETCH in the query ensures repositoryFile and repository are loaded in this session
        List<CodeChunk> unembeddedChunks = codeChunkRepository.findByRepositoryIdAndEmbeddingStatusIn(repositoryId, targetStatuses);

        if (unembeddedChunks.isEmpty()) {
            log.info("No chunks require embedding for repository {}", repositoryId);
            // Mark INDEXED since there are no chunks to embed — still a valid (tiny) repo
            markRepositoryIndexed(repositoryId, true);
            return;
        }

        // Materialise TextSegments inside the Hibernate session while associations are loaded.
        // This MUST happen before the executor is invoked to avoid LazyInitializationException
        // in virtual threads that have no Hibernate session bound.
        List<TextSegment> segments = unembeddedChunks.stream()
                .map(this::toTextSegment)
                .toList();

        // Capture IDs for status updates (no JPA entity reference escapes the transaction boundary)
        List<UUID> chunkIds = unembeddedChunks.stream().map(CodeChunk::getId).toList();

        processChunks(repositoryId, chunkIds, segments);
    }

    @org.springframework.scheduling.annotation.Scheduled(fixedDelayString = "${ai.embedding.retry-interval-ms:60000}")
    @Transactional
    public void retryFailedEmbeddings() {
        List<EmbeddingStatus> targetStatuses = List.of(EmbeddingStatus.PENDING, EmbeddingStatus.FAILED);
        // JOIN FETCH ensures all lazy associations are loaded within this transaction
        List<CodeChunk> unembeddedChunks = codeChunkRepository.findByEmbeddingStatusIn(targetStatuses);

        if (unembeddedChunks.isEmpty()) {
            return; // Silent — prevents log spam on quiet systems
        }

        log.info("Running scheduled retry for {} pending/failed chunks...", unembeddedChunks.size());

        // Group by repository ID
        java.util.Map<UUID, List<CodeChunk>> grouped = unembeddedChunks.stream()
                .collect(Collectors.groupingBy(c -> c.getRepositoryFile().getRepository().getId()));

        for (java.util.Map.Entry<UUID, List<CodeChunk>> entry : grouped.entrySet()) {
            UUID repoId = entry.getKey();
            List<CodeChunk> chunks = entry.getValue();
            log.info("Retrying {} chunks for repository {}", chunks.size(), repoId);

            // Materialise segments inside the transaction before handing to virtual threads
            List<TextSegment> segments = chunks.stream().map(this::toTextSegment).toList();
            List<UUID> chunkIds = chunks.stream().map(CodeChunk::getId).toList();
            processChunks(repoId, chunkIds, segments);
        }
    }

    /**
     * Marks chunk IDs as PROCESSING, then fans out embedding generation across batches
     * using virtual threads. Uses only primitive IDs and value-type TextSegments —
     * no JPA proxies cross thread boundaries.
     */
    private void processChunks(UUID repositoryId, List<UUID> chunkIds, List<TextSegment> segments) {
        log.info("Processing {} chunks to embed for repository {}", chunkIds.size(), repositoryId);

        // Mark as PROCESSING to prevent the scheduler from double-processing
        codeChunkRepository.markStatusForIds(chunkIds, EmbeddingStatus.PROCESSING);

        EmbeddingStore<TextSegment> store = chromaDbClient.getStoreForRepository(repositoryId);

        // Partition into batches of BATCH_SIZE
        List<List<UUID>> idBatches = partition(chunkIds, BATCH_SIZE);
        List<List<TextSegment>> segBatches = partition(segments, BATCH_SIZE);

        boolean allSucceeded = true;

        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            List<Future<Boolean>> futures = new ArrayList<>();

            for (int i = 0; i < idBatches.size(); i++) {
                final List<UUID> batchIds = idBatches.get(i);
                final List<TextSegment> batchSegs = segBatches.get(i);
                futures.add(executor.submit(() -> processBatch(batchIds, batchSegs, store)));
            }

            for (Future<Boolean> future : futures) {
                try {
                    if (!future.get()) {
                        allSucceeded = false;
                    }
                } catch (Exception e) {
                    log.error("Batch embedding future threw unexpectedly for repository {}", repositoryId, e);
                    allSucceeded = false;
                }
            }
        } catch (Exception e) {
            log.error("Error during parallel embedding generation for repository {}", repositoryId, e);
            allSucceeded = false;
            // Reset PROCESSING → FAILED for any chunks that were not individually handled
            codeChunkRepository.markStatusForIds(chunkIds, EmbeddingStatus.FAILED);
        }

        // Update repository IndexingStatus based on outcome
        markRepositoryIndexed(repositoryId, allSucceeded);

        log.info("Finished embedding pipeline for repository {}. Success: {}", repositoryId, allSucceeded);
    }

    /**
     * Processes a single batch. Returns {@code true} on full success, {@code false} on any failure.
     * <p>No JPA entities are used here — only IDs (UUIDs) and TextSegments are passed in.
     */
    private boolean processBatch(List<UUID> batchIds, List<TextSegment> segments, EmbeddingStore<TextSegment> store) {
        try {
            List<Embedding> embeddings = embeddingGenerationService.generateEmbeddings(segments);

            // Insert into ChromaDB
            store.addAll(embeddings, segments);

            // Mark successful — use a @Transactional helper to write back through a fresh session
            markBatchCompleted(batchIds);
            return true;

        } catch (Exception e) {
            log.error("Failed to process embedding batch of size {}", batchIds.size(), e);
            markBatchFailed(batchIds, e.getMessage());
            return false;
        }
    }

    @Transactional
    protected void markBatchCompleted(List<UUID> chunkIds) {
        Instant now = Instant.now();
        codeChunkRepository.markCompletedForIds(chunkIds, now);
    }

    @Transactional
    protected void markBatchFailed(List<UUID> chunkIds, String error) {
        codeChunkRepository.incrementRetryAndMarkFailed(chunkIds, error, MAX_RETRIES);
    }

    @Transactional
    protected void markRepositoryIndexed(UUID repositoryId, boolean success) {
        repositoryRepository.findById(repositoryId).ifPresent(repo -> {
            if (success) {
                repo.setIndexingStatus(IndexingStatus.INDEXED);
                log.info("Repository {} marked INDEXED — RAG pipeline fully operational.", repositoryId);
            } else {
                repo.setIndexingStatus(IndexingStatus.EMBEDDING_FAILED);
                log.warn("Repository {} marked EMBEDDING_FAILED — some chunks could not be embedded.", repositoryId);
            }
            repositoryRepository.save(repo);
        });
    }

    /**
     * Converts a {@link CodeChunk} to a {@link TextSegment}.
     * <strong>Must be called within a Hibernate session</strong> (i.e., inside a @Transactional method)
     * because it accesses the lazy {@code repositoryFile} and {@code repository} associations.
     */
    private TextSegment toTextSegment(CodeChunk chunk) {
        RepositoryFile file = chunk.getRepositoryFile(); // safe — JOIN FETCH loaded this
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
