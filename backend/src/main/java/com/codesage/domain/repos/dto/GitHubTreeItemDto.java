package com.codesage.domain.repos.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class GitHubTreeItemDto {
    private String path;
    private String mode;
    private String type; // "blob" or "tree"
    private String sha;
    private Long size;
    private String url;
}
