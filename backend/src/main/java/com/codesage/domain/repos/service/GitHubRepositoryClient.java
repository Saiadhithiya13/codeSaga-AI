package com.codesage.domain.repos.service;

import com.codesage.config.properties.GitHubProperties;
import com.codesage.domain.repos.dto.GitHubContributorDto;
import com.codesage.domain.repos.dto.GitHubRepoDto;
import com.codesage.exception.ExternalServiceException;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Client for GitHub REST API v3 focused on repository operations.
 *
 * <p>Implements pagination, caching (via Redis), and retries on transient failures.
 */
@Log4j2
@Service
@RequiredArgsConstructor
public class GitHubRepositoryClient {

    private final RestTemplate restTemplate;
    private final GitHubProperties gitHubProperties;

    /**
     * Fetches all repositories for the authenticated user.
     * Results are cached for 5 minutes.
     */
    @Cacheable(value = "github:user:repos", key = "#userId")
    @Retryable(
            retryFor = {RestClientException.class},
            noRetryFor = {HttpClientErrorException.Unauthorized.class, HttpClientErrorException.Forbidden.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public List<GitHubRepoDto> fetchUserRepositories(String accessToken, String userId) {
        log.debug("Fetching GitHub repositories for user {}", userId);
        return fetchAllPages("/user/repos", accessToken, GitHubRepoDto[].class);
    }

    /**
     * Fetches metadata for a specific repository by its full name (e.g. "octocat/Hello-World").
     * Cached for 10 minutes.
     */
    @Cacheable(value = "github:repo:metadata", key = "#fullName")
    @Retryable(
            retryFor = {RestClientException.class},
            noRetryFor = {HttpClientErrorException.NotFound.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000)
    )
    public GitHubRepoDto fetchRepository(String accessToken, String fullName) {
        log.debug("Fetching GitHub repository metadata for {}", fullName);
        try {
            ResponseEntity<GitHubRepoDto> response = restTemplate.exchange(
                    gitHubProperties.getApiBaseUrl() + "/repos/" + fullName,
                    HttpMethod.GET,
                    new HttpEntity<>(buildAuthHeaders(accessToken)),
                    GitHubRepoDto.class
            );
            return response.getBody();
        } catch (HttpClientErrorException.NotFound e) {
            throw new ExternalServiceException("GitHub API", "Repository not found or access denied: " + fullName);
        } catch (RestClientException e) {
            log.error("Error fetching repository {}", fullName, e);
            throw new ExternalServiceException("GitHub API", "Failed to fetch repository details", e);
        }
    }

    /**
     * Fetches contributors for a repository.
     * Cached for 10 minutes.
     */
    @Cacheable(value = "github:repo:contributors", key = "#fullName")
    @Retryable(
            retryFor = {RestClientException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000)
    )
    public List<GitHubContributorDto> fetchContributors(String accessToken, String fullName) {
        log.debug("Fetching contributors for {}", fullName);
        return fetchAllPages("/repos/" + fullName + "/contributors", accessToken, GitHubContributorDto[].class);
    }

    /**
     * Fetches metadata for a pull request.
     */
    @Retryable(
            retryFor = {RestClientException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000)
    )
    public com.codesage.domain.prreview.dto.GitHubPullRequestDto fetchPullRequest(String accessToken, String fullName, String pullNumber) {
        log.debug("Fetching PR {} for {}", pullNumber, fullName);
        try {
            ResponseEntity<com.codesage.domain.prreview.dto.GitHubPullRequestDto> response = restTemplate.exchange(
                    gitHubProperties.getApiBaseUrl() + "/repos/" + fullName + "/pulls/" + pullNumber,
                    HttpMethod.GET,
                    new HttpEntity<>(buildAuthHeaders(accessToken)),
                    com.codesage.domain.prreview.dto.GitHubPullRequestDto.class
            );
            return response.getBody();
        } catch (HttpClientErrorException.NotFound e) {
            throw new ExternalServiceException("GitHub API", "PR not found: " + pullNumber);
        } catch (RestClientException e) {
            log.error("Error fetching PR {} for {}", pullNumber, fullName, e);
            throw new ExternalServiceException("GitHub API", "Failed to fetch PR", e);
        }
    }

    /**
     * Fetches the raw diff of a pull request.
     */
    @Retryable(
            retryFor = {RestClientException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000)
    )
    public String fetchPullRequestDiff(String accessToken, String fullName, String pullNumber) {
        log.debug("Fetching PR diff {} for {}", pullNumber, fullName);
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);
            headers.set("Accept", "application/vnd.github.v3.diff");
            headers.set("X-GitHub-Api-Version", "2022-11-28");

            ResponseEntity<String> response = restTemplate.exchange(
                    gitHubProperties.getApiBaseUrl() + "/repos/" + fullName + "/pulls/" + pullNumber,
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    String.class
            );
            return response.getBody();
        } catch (HttpClientErrorException.NotFound e) {
            throw new ExternalServiceException("GitHub API", "PR Diff not found: " + pullNumber);
        } catch (RestClientException e) {
            log.error("Error fetching PR diff {} for {}", pullNumber, fullName, e);
            throw new ExternalServiceException("GitHub API", "Failed to fetch PR diff", e);
        }
    }

    /**
     * Fetches the full tree of a repository.
     */
    @Retryable(
            retryFor = {RestClientException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000)
    )
    public com.codesage.domain.repos.dto.GitHubTreeResponseDto fetchRepositoryTree(String accessToken, String fullName, String branch) {
        log.debug("Fetching tree for {} branch {}", fullName, branch);
        try {
            ResponseEntity<com.codesage.domain.repos.dto.GitHubTreeResponseDto> response = restTemplate.exchange(
                    gitHubProperties.getApiBaseUrl() + "/repos/" + fullName + "/git/trees/" + branch + "?recursive=1",
                    HttpMethod.GET,
                    new HttpEntity<>(buildAuthHeaders(accessToken)),
                    com.codesage.domain.repos.dto.GitHubTreeResponseDto.class
            );
            return response.getBody();
        } catch (HttpClientErrorException.NotFound e) {
            throw new ExternalServiceException("GitHub API", "Tree not found for: " + fullName);
        } catch (RestClientException e) {
            log.error("Error fetching tree for {}", fullName, e);
            throw new ExternalServiceException("GitHub API", "Failed to fetch repository tree", e);
        }
    }

    /**
     * Fetches file content as string.
     */
    @Retryable(
            retryFor = {RestClientException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000)
    )
    public String fetchFileContent(String accessToken, String fullName, String fileSha) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);
            headers.set("Accept", "application/vnd.github.v3.raw");
            headers.set("X-GitHub-Api-Version", "2022-11-28");

            ResponseEntity<String> response = restTemplate.exchange(
                    gitHubProperties.getApiBaseUrl() + "/repos/" + fullName + "/git/blobs/" + fileSha,
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    String.class
            );
            return response.getBody();
        } catch (HttpClientErrorException.NotFound e) {
            throw new ExternalServiceException("GitHub API", "Blob not found: " + fileSha);
        } catch (RestClientException e) {
            log.error("Error fetching blob {} for {}", fileSha, fullName, e);
            throw new ExternalServiceException("GitHub API", "Failed to fetch file content", e);
        }
    }

    /**
     * Helper to fetch all pages of a GitHub API collection endpoint.
     */
    private <T> List<T> fetchAllPages(String path, String accessToken, Class<T[]> responseType) {
        List<T> allItems = new ArrayList<>();
        int page = 1;
        int perPage = 100;
        boolean hasMore = true;

        HttpEntity<Void> entity = new HttpEntity<>(buildAuthHeaders(accessToken));

        while (hasMore) {
            String url = UriComponentsBuilder.fromHttpUrl(gitHubProperties.getApiBaseUrl() + path)
                    .queryParam("per_page", perPage)
                    .queryParam("page", page)
                    .toUriString();

            try {
                ResponseEntity<T[]> response = restTemplate.exchange(url, HttpMethod.GET, entity, responseType);
                T[] items = response.getBody();

                if (items != null && items.length > 0) {
                    allItems.addAll(Arrays.asList(items));
                    // If we got exactly perPage items, there might be another page
                    if (items.length < perPage) {
                        hasMore = false;
                    } else {
                        page++;
                    }
                } else {
                    hasMore = false;
                }
            } catch (HttpClientErrorException e) {
                log.warn("GitHub API error fetching {}: {} - {}", url, e.getStatusCode(), e.getMessage());
                throw new ExternalServiceException("GitHub API", "Failed to fetch data: " + e.getStatusText(), e);
            } catch (RestClientException e) {
                log.error("Network error fetching {}", url, e);
                throw new ExternalServiceException("GitHub API", "Network error while connecting to GitHub", e);
            }
        }

        return allItems;
    }

    private HttpHeaders buildAuthHeaders(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.set("Accept", "application/vnd.github+json");
        headers.set("X-GitHub-Api-Version", "2022-11-28");
        return headers;
    }
}
