package com.codesage.domain.chat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record ChatSessionCreateDto(
        @NotNull UUID repositoryId,
        @NotBlank String title
) {}
