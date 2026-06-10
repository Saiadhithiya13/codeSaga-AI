package com.codesage.domain.repos.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class GitHubContributorDto {

    @JsonProperty("login")
    private String login;

    @JsonProperty("avatar_url")
    private String avatarUrl;

    @JsonProperty("contributions")
    private Integer contributions;
}
