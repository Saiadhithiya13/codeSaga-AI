package com.codesage.domain.repos.service.ingestion;

import com.codesage.domain.repos.model.IndexingStatus;
import com.codesage.domain.repos.model.Repository;
import com.codesage.domain.repos.repository.RepositoryRepository;
import com.codesage.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Spring-managed helper that owns the @Transactional boundary for repository
 * status reads and writes.
 *
 * <p>These methods were extracted from {@link RepositoryIndexingService} to fix
 * the self-invocation proxy bypass: Spring's @Transactional proxy is only
 * active when the call passes through the proxy, which does not happen for
 * {@code this.someMethod()} calls inside the same bean.
 */
@Service
@RequiredArgsConstructor
public class RepositoryStatusUpdater {

    private final RepositoryRepository repositoryRepository;

    /**
     * Loads the repository for the given owner, asserting ownership.
     * Runs in its own transaction so the entity is managed and changes are tracked.
     */
    @Transactional
    public Repository loadForUpdate(UUID repositoryId, UUID userId) {
        return repositoryRepository.findByIdAndUserId(repositoryId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Repository", repositoryId));
    }

    /**
     * Persists the new indexing status for the repository.
     * Runs in its own transaction to guarantee a flush to the DB.
     */
    @Transactional
    public void updateStatus(Repository repository, IndexingStatus status) {
        repository.setIndexingStatus(status);
        repositoryRepository.save(repository);
    }
}
