package com.codesage.domain.repos.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

@Schema(description = "Semantic Search Request")
public record SearchRequestDto(
        @NotBlank String query,
        @Min(1) @Max(50) Integer maxResults
) {
    public SearchRequestDto {
        if (maxResults == null) {
            maxResults = 5;
        }
    }
}
