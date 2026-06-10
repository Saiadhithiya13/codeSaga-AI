package com.codesage.domain.auth.service;

import com.codesage.config.properties.GitHubProperties;
import com.codesage.domain.auth.dto.GitHubUserDto;
import com.codesage.exception.ExternalServiceException;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * GitHub REST API client for OAuth and user profile operations.
 *
 * <p>Handles:
 * <ol>
 *   <li>OAuth code-for-token exchange</li>
 *   <li>User profile fetch ({@code GET /user})</li>
 *   <li>Private email fetch ({@code GET /user/emails}) — when profile email is null</li>
 * </ol>
 *
 * <p>Architecture Spec §GitHub APIs: REST API v3.
 */
@Log4j2
@Service
@RequiredArgsConstructor
public class GitHubApiClient {

    private final RestTemplate    restTemplate;
    private final GitHubProperties githubProperties;

    // ─── OAuth Code Exchange ──────────────────────────────────────────────────

    /**
     * Exchanges a GitHub OAuth authorization code for an access token.
     *
     * @param code the authorization code received from GitHub's redirect
     * @return the GitHub access token string
     * @throws ExternalServiceException if GitHub rejects the code
     */
    public String exchangeCodeForToken(String code) {
        log.debug("Exchanging GitHub OAuth code for access token");

        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, String> body = Map.of(
                "client_id",     githubProperties.getClientId(),
                "client_secret", githubProperties.getClientSecret(),
                "code",          code
        );

        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                    githubProperties.getTokenUrl(),
                    HttpMethod.POST,
                    new HttpEntity<>(body, headers),
                    Map.class
            );

            Map<?, ?> responseBody = response.getBody();
            if (responseBody == null) {
                throw new ExternalServiceException("GitHub OAuth", "Empty response from token endpoint");
            }

            if (responseBody.containsKey("error")) {
                String error = String.valueOf(responseBody.get("error_description"));
                log.warn("GitHub token exchange failed: {}", error);
                throw new ExternalServiceException("GitHub OAuth", "Code exchange failed: " + error);
            }

            String accessToken = String.valueOf(responseBody.get("access_token"));
            if (accessToken.isBlank() || "null".equals(accessToken)) {
                throw new ExternalServiceException("GitHub OAuth", "No access_token in response");
            }

            log.debug("GitHub access token obtained successfully");
            return accessToken;

        } catch (RestClientException e) {
            log.error("GitHub token exchange request failed", e);
            throw new ExternalServiceException("GitHub OAuth", "Network error during token exchange", e);
        }
    }

    // ─── User Profile ─────────────────────────────────────────────────────────

    /**
     * Fetches the authenticated user's GitHub profile.
     *
     * @param accessToken valid GitHub OAuth access token
     * @return populated {@link GitHubUserDto}
     */
    public GitHubUserDto fetchUser(String accessToken) {
        log.debug("Fetching GitHub user profile");

        try {
            ResponseEntity<GitHubUserDto> response = restTemplate.exchange(
                    githubProperties.getApiBaseUrl() + "/user",
                    HttpMethod.GET,
                    new HttpEntity<>(buildAuthHeaders(accessToken)),
                    GitHubUserDto.class
            );

            GitHubUserDto user = response.getBody();
            if (user == null || user.getId() == null) {
                throw new ExternalServiceException("GitHub API", "Empty user profile response");
            }

            // If public email is null, fetch private email
            if (user.getEmail() == null || user.getEmail().isBlank()) {
                String primaryEmail = fetchPrimaryEmail(accessToken);
                user.setEmail(primaryEmail);
            }

            log.debug("Fetched GitHub profile for user: {}", user.getLogin());
            return user;

        } catch (HttpClientErrorException.Unauthorized e) {
            throw new ExternalServiceException("GitHub API", "Invalid or expired access token");
        } catch (RestClientException e) {
            log.error("Failed to fetch GitHub user profile", e);
            throw new ExternalServiceException("GitHub API", "Failed to fetch user profile", e);
        }
    }

    // ─── Private Email ────────────────────────────────────────────────────────

    /**
     * Fetches the primary verified email from {@code GET /user/emails}.
     * Called when the public profile email is null (user has private email).
     */
    private String fetchPrimaryEmail(String accessToken) {
        try {
            ResponseEntity<Map[]> response = restTemplate.exchange(
                    githubProperties.getApiBaseUrl() + "/user/emails",
                    HttpMethod.GET,
                    new HttpEntity<>(buildAuthHeaders(accessToken)),
                    Map[].class
            );

            Map[] emails = response.getBody();
            if (emails == null) return null;

            // Prefer: primary + verified email
            return Arrays.stream(emails)
                    .filter(e -> Boolean.TRUE.equals(e.get("primary"))
                            && Boolean.TRUE.equals(e.get("verified")))
                    .map(e -> String.valueOf(e.get("email")))
                    .findFirst()
                    .orElse(null);

        } catch (Exception e) {
            log.warn("Failed to fetch private emails (non-critical): {}", e.getMessage());
            return null;
        }
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private HttpHeaders buildAuthHeaders(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.set("X-GitHub-Api-Version", "2022-11-28");
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        return headers;
    }
}
