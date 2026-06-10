package com.codesage.domain.repos.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class GitHubTreeResponseDto {
    private String sha;
    private String url;
    private List<GitHubTreeItemDto> tree;
    private Boolean truncated;
}
