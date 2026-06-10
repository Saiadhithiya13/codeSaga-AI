package com.codesage.domain.auth.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Response returned after successful authentication (login or token refresh).
 *
 * <p>Architecture Spec: Tokens are set in HttpOnly cookies — not in this body.
 * This DTO conveys the user profile and authentication status only.
 * The caller reads tokens from the {@code Set-Cookie} response headers.
 */
@Schema(description = "Authentication success response — tokens are set via HttpOnly cookies")
public record AuthTokenResponseDto(

        @Schema(description = "Authenticated user profile")
        UserResponseDto user,

        @Schema(description = "Indicates new vs. returning user", example = "true")
        boolean isNewUser,

        @Schema(description = "Human-readable auth status message", example = "Login successful")
        String message
) {}
