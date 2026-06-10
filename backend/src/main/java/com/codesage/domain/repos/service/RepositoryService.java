package com.codesage.domain.repos.service;

import com.codesage.domain.auth.model.User;
import com.codesage.domain.auth.service.UserService;
import com.codesage.domain.repos.dto.GitHubRepoDto;
import com.codesage.domain.repos.dto.RepositoryDto;
import com.codesage.domain.repos.model.Repository;
import com.codesage.domain.repos.repository.RepositoryRepository;
import com.codesage.exception.ConflictException;
import com.codesage.exception.ResourceNotFoundException;
import com.codesage.security.util.TokenEncryptionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Log4j2
@Service
@RequiredArgsConstructor
public class RepositoryService {

    private final RepositoryRepository repositoryRepository;
    private final GitHubRepositoryClient gitHubRepositoryClient;
    private final UserService userService;
    private final TokenEncryptionService tokenEncryptionService;
    private final GitHubRepositorySyncService syncService;

    @Transactional(readOnly = true)
    public List<RepositoryDto> getUserRepositories(UUID userId) {
        return repositoryRepository.findByUserId(userId).stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public RepositoryDto getRepository(UUID id, UUID userId) {
        return toDto(getRepositoryEntity(id, userId));
    }

    @Transactional(readOnly = true)
    public Repository getRepositoryEntity(UUID id, UUID userId) {
        return repositoryRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Repository", id));
    }

    @Transactional(readOnly = true)
    public List<GitHubRepoDto> getAvailableRemoteRepositories(UUID userId) {
        User user = userService.findEntityById(userId);
        String accessToken = tokenEncryptionService.decrypt(user.getGithubAccessToken());
        return gitHubRepositoryClient.fetchUserRepositories(accessToken, userId.toString());
    }

    @Transactional
    public RepositoryDto connectRepository(UUID userId, String fullName) {
        User user = userService.findEntityById(userId);
        String accessToken = tokenEncryptionService.decrypt(user.getGithubAccessToken());

        GitHubRepoDto repoData = gitHubRepositoryClient.fetchRepository(accessToken, fullName);

        if (repositoryRepository.existsByUserIdAndGithubRepoId(userId, repoData.getId())) {
            throw new ConflictException("Repository already connected");
        }

        Repository repository = Repository.builder()
                .user(user)
                .githubRepoId(repoData.getId())
                .fullName(repoData.getFullName())
                .name(repoData.getName())
                .description(repoData.getDescription())
                .language(repoData.getLanguage())
                .isPrivate(repoData.getIsPrivate() != null ? repoData.getIsPrivate() : false)
                .defaultBranch(repoData.getDefaultBranch() != null ? repoData.getDefaultBranch() : "main")
                .starsCount(repoData.getStargazersCount() != null ? repoData.getStargazersCount() : 0)
                .forksCount(repoData.getForksCount() != null ? repoData.getForksCount() : 0)
                .openIssuesCount(repoData.getOpenIssuesCount() != null ? repoData.getOpenIssuesCount() : 0)
                .build();

        repository = repositoryRepository.save(repository);
        log.info("User {} connected repository {}", userId, fullName);

        // Trigger initial sync async
        syncService.syncRepositoryAsync(repository.getId(), userId);

        return toDto(repository);
    }

    @Transactional
    public void disconnectRepository(UUID id, UUID userId) {
        Repository repository = repositoryRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Repository", id));
        
        repositoryRepository.delete(repository);
        log.info("User {} disconnected repository {}", userId, repository.getFullName());
    }

    private RepositoryDto toDto(Repository entity) {
        return new RepositoryDto(
                entity.getId(),
                entity.getGithubRepoId(),
                entity.getFullName(),
                entity.getName(),
                entity.getDescription(),
                entity.getLanguage(),
                entity.getIsPrivate(),
                entity.getDefaultBranch(),
                entity.getStarsCount(),
                entity.getForksCount(),
                entity.getOpenIssuesCount(),
                entity.getLastSyncedAt(),
                entity.getCreatedAt()
        );
    }
}
