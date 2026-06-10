package com.codesage.domain.auth.service;

import com.codesage.domain.auth.dto.request.GitHubCallbackRequestDto;
import com.codesage.domain.auth.dto.response.AuthTokenResponseDto;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.util.UUID;

/**
 * Contract for authentication operations (login, refresh, logout).
 */
public interface AuthService {

    /**
     * Handles the GitHub OAuth callback.
     * Exchanges the code for a token, fetches the user profile, creates/updates the user,
     * and sets HttpOnly JWT cookies.
     *
     * @param requestDto the authorization code from GitHub
     * @param response   the HTTP response (to set cookies)
     * @return the authenticated user profile and status
     */
    AuthTokenResponseDto handleGitHubCallback(GitHubCallbackRequestDto requestDto, HttpServletResponse response);

    /**
     * Refreshes an expired access token using a valid refresh token.
     * Implements refresh token rotation and theft detection.
     *
     * @param request  the HTTP request (to read cookies)
     * @param response the HTTP response (to set new cookies)
     * @return the authenticated user profile
     */
    AuthTokenResponseDto refreshToken(HttpServletRequest request, HttpServletResponse response);

    /**
     * Logs out the user by clearing cookies and revoking the active session in the database.
     *
     * @param userId   the user's UUID
     * @param request  the HTTP request (to read the refresh token being revoked)
     * @param response the HTTP response (to clear cookies)
     */
    void logout(UUID userId, HttpServletRequest request, HttpServletResponse response);
}
