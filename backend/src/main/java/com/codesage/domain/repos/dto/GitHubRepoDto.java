package com.codesage.domain.repos.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class GitHubRepoDto {

    private Long id;

    @JsonAlias("full_name")
    private String fullName;

    private String name;

    private String description;

    private String language;

    @JsonProperty("private")
    private Boolean isPrivate;

    @JsonAlias("default_branch")
    private String defaultBranch;

    @JsonAlias("stargazers_count")
    private Integer stargazersCount;

    @JsonAlias("forks_count")
    private Integer forksCount;

    @JsonAlias("open_issues_count")
    private Integer openIssuesCount;
}
