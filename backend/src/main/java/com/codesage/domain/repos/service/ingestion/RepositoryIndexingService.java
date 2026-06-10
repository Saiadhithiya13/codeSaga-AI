package com.codesage.domain.repos.service.ingestion;

import com.codesage.domain.auth.model.User;
import com.codesage.domain.auth.service.UserService;
import com.codesage.domain.repos.model.IndexingStatus;
import com.codesage.domain.repos.model.Repository;
import com.codesage.domain.repos.model.RepositoryFile;
import com.codesage.domain.repos.repository.RepositoryRepository;
import com.codesage.exception.ResourceNotFoundException;
import com.codesage.security.util.TokenEncryptionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

/**
 * Orchestrator for the repository indexing pipeline.
 */
@Log4j2
@Service
@RequiredArgsConstructor
public class RepositoryIndexingService {

    private final RepositoryRepository repositoryRepository;
    private final GitHubTreeService treeService;
    private final RepositoryScannerService scannerService;
    private final CodeChunkingService chunkingService;
    private final UserService userService;
    private final TokenEncryptionService tokenEncryptionService;

    @Async
    public void startIndexingAsync(UUID repositoryId, UUID userId) {
        log.info("Starting async indexing pipeline for repository {}", repositoryId);
        
        Repository repository = getRepositoryForUpdate(repositoryId, userId);
        updateStatus(repository, IndexingStatus.INDEXING);

        Path repoPath = null;
        try {
            User user = userService.findEntityById(userId);
            String accessToken = tokenEncryptionService.decrypt(user.getGithubAccessToken());

            // 1. Download via GitHub Tree API
            repoPath = treeService.downloadRepository(repository.getFullName(), accessToken);

            // 2. Scan and Save RepositoryFiles
            List<RepositoryFile> files = scannerService.scanAndSaveFiles(repository, repoPath);

            // 3. Chunk Files and Save CodeChunks
            chunkingService.chunkFiles(files, repoPath);

            // 4. Mark success
            updateStatus(repository, IndexingStatus.INDEXED);
            log.info("Successfully completed indexing pipeline for {}", repository.getFullName());

        } catch (Exception e) {
            log.error("Indexing pipeline failed for repository {}", repositoryId, e);
            updateStatus(repository, IndexingStatus.FAILED);
        } finally {
            // 5. Cleanup temp files
            if (repoPath != null) {
                try {
                    org.springframework.util.FileSystemUtils.deleteRecursively(repoPath);
                } catch (Exception e) {
                    log.warn("Failed to cleanup temp directory {}", repoPath, e);
                }
            }
        }
    }

    @Transactional
    protected Repository getRepositoryForUpdate(UUID repositoryId, UUID userId) {
        return repositoryRepository.findByIdAndUserId(repositoryId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Repository", repositoryId));
    }

    @Transactional
    protected void updateStatus(Repository repository, IndexingStatus status) {
        repository.setIndexingStatus(status);
        repositoryRepository.save(repository);
    }
}
