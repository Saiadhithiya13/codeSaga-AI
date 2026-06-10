package com.codesage.domain.chat.dto;

import java.time.Instant;
import java.util.UUID;

public record ChatSessionDto(
        UUID id,
        UUID repositoryId,
        String title,
        Instant createdAt,
        Instant updatedAt
) {}
