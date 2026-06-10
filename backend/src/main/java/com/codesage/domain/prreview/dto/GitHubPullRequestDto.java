package com.codesage.domain.prreview.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class GitHubPullRequestDto {
    private String title;
    private String state;
    private String body;
    @JsonProperty("html_url")
    private String htmlUrl;
}
