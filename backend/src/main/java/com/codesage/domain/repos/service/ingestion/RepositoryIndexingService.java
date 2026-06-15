package com.codesage.domain.repos.service.ingestion;

import com.codesage.domain.auth.model.User;
import com.codesage.domain.auth.service.UserService;
import com.codesage.domain.repos.model.IndexingStatus;
import com.codesage.domain.repos.model.Repository;
import com.codesage.domain.repos.model.RepositoryFile;
import com.codesage.domain.repos.service.embedding.RepositoryEmbeddingService;
import com.codesage.security.util.TokenEncryptionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

/**
 * Orchestrator for the repository indexing pipeline.
 *
 * <p>Pipeline order:
 * <ol>
 *   <li>Download repository blobs via GitHub Tree API (rate-limited with Semaphore)</li>
 *   <li>Scan temp dir and persist {@code RepositoryFile} records</li>
 *   <li>Guard: require at least 1 file — fail fast otherwise</li>
 *   <li>Chunk files and persist {@code CodeChunk} records (status = PENDING)</li>
 *   <li>Mark repository as CHUNKED (files + chunks persisted)</li>
 *   <li>Trigger embedding pipeline asynchronously; status → EMBEDDING</li>
 *   <li>Status → INDEXED only after embedding completes successfully</li>
 * </ol>
 *
 * <p>Status mutations delegate to {@link RepositoryStatusUpdater} to ensure
 * @Transactional boundaries are honoured through the Spring proxy.
 */
@Log4j2
@Service
@RequiredArgsConstructor
public class RepositoryIndexingService {

    private final GitHubTreeService treeService;
    private final RepositoryScannerService scannerService;
    private final CodeChunkingService chunkingService;
    private final UserService userService;
    private final TokenEncryptionService tokenEncryptionService;
    private final RepositoryEmbeddingService embeddingService;
    private final RepositoryStatusUpdater statusUpdater;

    @Async
    public void startIndexingAsync(UUID repositoryId, UUID userId) {
        log.info("Starting async indexing pipeline for repository {}", repositoryId);

        Repository repository = statusUpdater.loadForUpdate(repositoryId, userId);
        statusUpdater.updateStatus(repository, IndexingStatus.INDEXING);

        Path repoPath = null;
        try {
            User user = userService.findEntityById(userId);
            String accessToken = tokenEncryptionService.decrypt(user.getGithubAccessToken());

            // 1. Download via GitHub Tree API
            repoPath = treeService.downloadRepository(repository.getFullName(), accessToken);
            log.info("Downloaded repository {} to {}", repository.getFullName(), repoPath);

            // 2. Scan and Save RepositoryFiles
            List<RepositoryFile> files = scannerService.scanAndSaveFiles(repository, repoPath);
            log.info("Scanned {} files for repository {}", files.size(), repository.getFullName());

            // 3. Guard: require at least 1 file to declare success
            if (files.isEmpty()) {
                log.warn("No supported files found for repository {}. Marking FAILED.", repository.getFullName());
                statusUpdater.updateStatus(repository, IndexingStatus.FAILED);
                return;
            }

            // 4. Chunk Files and Save CodeChunks
            int chunkCount = chunkingService.chunkFiles(files, repoPath);
            log.info("Generated {} chunks for repository {}", chunkCount, repository.getFullName());

            // 5. Mark chunking complete — files and chunks are now in DB
            statusUpdater.updateStatus(repository, IndexingStatus.CHUNKED);
            log.info("Repository {} marked CHUNKED ({} files, {} chunks)", repository.getFullName(), files.size(), chunkCount);

            // 6. Trigger embedding pipeline (async — non-blocking); status → EMBEDDING immediately
            statusUpdater.updateStatus(repository, IndexingStatus.EMBEDDING);
            embeddingService.startRepositoryEmbeddingAsync(repositoryId);
            log.info("Embedding pipeline triggered for {}", repository.getFullName());

        } catch (Exception e) {
            log.error("Indexing pipeline failed for repository {}", repositoryId, e);
            statusUpdater.updateStatus(repository, IndexingStatus.FAILED);
        } finally {
            // 7. Cleanup temp files — must happen after chunking (reads from temp dir)
            if (repoPath != null) {
                try {
                    org.springframework.util.FileSystemUtils.deleteRecursively(repoPath);
                    log.debug("Cleaned up temp directory {} for {}", repoPath, repositoryId);
                } catch (Exception e) {
                    log.warn("Failed to cleanup temp directory {}", repoPath, e);
                }
            }
        }
    }
}
