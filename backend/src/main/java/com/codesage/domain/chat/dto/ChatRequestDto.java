package com.codesage.domain.chat.dto;

import jakarta.validation.constraints.NotBlank;

public record ChatRequestDto(
        @NotBlank String message
) {}
