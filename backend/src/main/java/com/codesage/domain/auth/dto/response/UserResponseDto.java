package com.codesage.domain.auth.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

/**
 * Public user profile response DTO.
 *
 * <p>Returned by {@code GET /api/v1/users/me} and included in
 * the auth callback response after successful login.
 * Never exposes sensitive fields (encrypted token, internal IDs).
 */
@Schema(description = "Authenticated user's public profile")
public record UserResponseDto(

        @Schema(description = "User's internal UUID", example = "550e8400-e29b-41d4-a716-446655440000")
        UUID id,

        @Schema(description = "GitHub login (username)", example = "octocat")
        String login,

        @Schema(description = "Display name from GitHub profile", example = "The Octocat")
        String name,

        @Schema(description = "Primary email address", example = "octocat@github.com")
        String email,

        @Schema(description = "GitHub avatar URL")
        String avatarUrl,

        @Schema(description = "User role", example = "USER", allowableValues = {"USER", "ADMIN"})
        String role,

        @Schema(description = "UTC timestamp of last login")
        Instant lastLoginAt,

        @Schema(description = "UTC timestamp of account creation")
        Instant createdAt
) {}
