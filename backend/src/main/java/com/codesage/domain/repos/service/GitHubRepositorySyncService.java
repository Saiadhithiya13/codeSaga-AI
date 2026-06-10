package com.codesage.domain.repos.service;

import com.codesage.domain.auth.model.User;
import com.codesage.domain.auth.service.UserService;
import com.codesage.domain.repos.dto.GitHubContributorDto;
import com.codesage.domain.repos.dto.GitHubRepoDto;
import com.codesage.domain.repos.model.Repository;
import com.codesage.domain.repos.model.RepositoryContributor;
import com.codesage.domain.repos.model.RepositoryMetric;
import com.codesage.domain.repos.repository.RepositoryContributorRepository;
import com.codesage.domain.repos.repository.RepositoryMetricRepository;
import com.codesage.domain.repos.repository.RepositoryRepository;
import com.codesage.exception.ResourceNotFoundException;
import com.codesage.security.util.TokenEncryptionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Log4j2
@Service
@RequiredArgsConstructor
public class GitHubRepositorySyncService {

    private final RepositoryRepository repositoryRepository;
    private final RepositoryContributorRepository contributorRepository;
    private final RepositoryMetricRepository metricRepository;
    private final GitHubRepositoryClient gitHubClient;
    private final UserService userService;
    private final TokenEncryptionService tokenEncryptionService;

    @Async
    public void syncRepositoryAsync(UUID repositoryId, UUID userId) {
        log.info("Starting async sync for repository {}", repositoryId);
        try {
            syncRepository(repositoryId, userId);
        } catch (Exception e) {
            log.error("Failed to sync repository asynchronously {}", repositoryId, e);
        }
    }

    @Transactional
    public void syncRepository(UUID repositoryId, UUID userId) {
        Repository repository = repositoryRepository.findByIdAndUserId(repositoryId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Repository", repositoryId));

        User user = userService.findEntityById(userId);
        String accessToken = tokenEncryptionService.decrypt(user.getGithubAccessToken());

        log.info("Syncing repository: {}", repository.getFullName());

        // 1. Fetch updated metadata
        GitHubRepoDto repoData = gitHubClient.fetchRepository(accessToken, repository.getFullName());
        
        repository.setDescription(repoData.getDescription());
        repository.setStarsCount(repoData.getStargazersCount() != null ? repoData.getStargazersCount() : 0);
        repository.setForksCount(repoData.getForksCount() != null ? repoData.getForksCount() : 0);
        repository.setOpenIssuesCount(repoData.getOpenIssuesCount() != null ? repoData.getOpenIssuesCount() : 0);
        
        // 2. Fetch contributors
        List<GitHubContributorDto> contributorsData = gitHubClient.fetchContributors(accessToken, repository.getFullName());
        
        // Replace contributors for this snapshot
        contributorRepository.deleteByRepositoryId(repository.getId());
        contributorRepository.flush();
        
        List<RepositoryContributor> contributors = contributorsData.stream()
                .map(c -> RepositoryContributor.builder()
                        .repository(repository)
                        .username(c.getLogin())
                        .avatarUrl(c.getAvatarUrl())
                        .contributions(c.getContributions() != null ? c.getContributions() : 0)
                        .build())
                .toList();
        
        contributorRepository.saveAll(contributors);

        // 3. Record Metrics Snapshot
        RepositoryMetric metric = RepositoryMetric.builder()
                .repository(repository)
                .contributorCount(contributors.size())
                // In a real scenario, we might query PRs specifically. For now, open_issues_count includes PRs in GitHub API
                .openPrCount(0) // Requires a separate API call to `/pulls`, skipping for now to respect rate limits or implement later
                .openIssueCount(repository.getOpenIssuesCount())
                .starsCount(repository.getStarsCount())
                .forksCount(repository.getForksCount())
                .build();
                
        metricRepository.save(metric);

        // 4. Update last synced
        repository.setLastSyncedAt(Instant.now());
        repositoryRepository.save(repository);
        
        log.info("Successfully synced repository {}", repository.getFullName());
    }
}
