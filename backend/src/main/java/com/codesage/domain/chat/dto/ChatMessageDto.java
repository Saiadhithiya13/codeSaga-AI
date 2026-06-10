package com.codesage.domain.chat.dto;

import com.codesage.domain.chat.model.MessageRole;

import java.time.Instant;
import java.util.UUID;

public record ChatMessageDto(
        UUID id,
        MessageRole role,
        String content,
        Instant createdAt
) {}
