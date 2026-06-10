package com.codesage.domain.auth.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Internal DTO for deserializing GitHub user profile responses.
 * Used only within the {@link com.codesage.domain.auth.service.GitHubApiClient} —
 * never exposed in API responses.
 */
@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class GitHubUserDto {

    @JsonProperty("id")
    private Long id;

    @JsonProperty("login")
    private String login;

    @JsonProperty("name")
    private String name;

    @JsonProperty("email")
    private String email;

    @JsonProperty("avatar_url")
    private String avatarUrl;

    @JsonProperty("bio")
    private String bio;

    @JsonProperty("public_repos")
    private Integer publicRepos;

    @JsonProperty("followers")
    private Integer followers;
}
