package com.codesage.domain.auth.service;

import com.codesage.domain.auth.dto.GitHubUserDto;
import com.codesage.domain.auth.dto.request.GitHubCallbackRequestDto;
import com.codesage.domain.auth.dto.response.AuthTokenResponseDto;
import com.codesage.domain.auth.mapper.UserMapper;
import com.codesage.domain.auth.model.User;
import com.codesage.domain.auth.model.UserSession;
import com.codesage.domain.auth.repository.UserRepository;
import com.codesage.domain.auth.repository.UserSessionRepository;
import com.codesage.exception.UnauthorizedException;
import com.codesage.security.jwt.JwtTokenProvider;
import com.codesage.security.util.CookieUtils;
import com.codesage.security.util.TokenEncryptionService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Implementation of {@link AuthService}.
 */
@Log4j2
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final GitHubApiClient         gitHubApiClient;
    private final UserRepository          userRepository;
    private final UserSessionRepository   userSessionRepository;
    private final JwtTokenProvider        jwtTokenProvider;
    private final CookieUtils             cookieUtils;
    private final TokenEncryptionService  tokenEncryptionService;
    private final UserMapper              userMapper;

    @Override
    @Transactional
    public AuthTokenResponseDto handleGitHubCallback(GitHubCallbackRequestDto requestDto, HttpServletResponse response) {
        log.debug("Processing GitHub OAuth callback");

        // 1. Exchange code for access token
        String githubToken = gitHubApiClient.exchangeCodeForToken(requestDto.code());

        // 2. Fetch user profile from GitHub
        GitHubUserDto gitHubUser = gitHubApiClient.fetchUser(githubToken);

        // 3. Find existing user or create new
        Optional<User> existingUserOpt = userRepository.findByGithubId(gitHubUser.getId());
        boolean isNewUser = existingUserOpt.isEmpty();

        User user;
        if (isNewUser) {
            user = new User();
            user.setGithubId(gitHubUser.getId());
            user.setLogin(gitHubUser.getLogin());
            user.setRole("USER");
            log.info("Creating new user from GitHub OAuth: {}", gitHubUser.getLogin());
        } else {
            user = existingUserOpt.get();
            log.debug("Updating existing user from GitHub OAuth: {}", gitHubUser.getLogin());
        }

        // 4. Update profile details & encrypted token
        user.setName(gitHubUser.getName());
        user.setEmail(gitHubUser.getEmail());
        user.setAvatarUrl(gitHubUser.getAvatarUrl());
        user.setGithubAccessToken(tokenEncryptionService.encrypt(githubToken));
        user.setLastLoginAt(Instant.now());
        
        user = userRepository.save(user);

        // 5. Generate application JWTs
        issueTokens(user, null, response);

        return new AuthTokenResponseDto(
                userMapper.toDto(user),
                isNewUser,
                "Authentication successful"
        );
    }

    @Override
    @Transactional
    public AuthTokenResponseDto refreshToken(HttpServletRequest request, HttpServletResponse response) {
        log.debug("Processing token refresh request");

        // 1. Extract token from cookie
        String refreshToken = cookieUtils.extractRefreshToken(request)
                .orElseThrow(() -> new UnauthorizedException("Refresh token not found"));

        // 2. Validate JWT signature & expiry
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new UnauthorizedException("Invalid or expired refresh token");
        }

        String jti = jwtTokenProvider.extractJti(refreshToken);
        UUID userId = jwtTokenProvider.extractUserId(refreshToken);

        // 3. Find session in DB
        UserSession session = userSessionRepository.findByJti(jti)
                .orElseThrow(() -> new UnauthorizedException("Session not found or already revoked"));

        // 4. Theft detection: if session is already revoked, someone is reusing an old token!
        if (session.getIsRevoked()) {
            log.warn("TOKEN THEFT DETECTED for user {}. Revoking entire token family {}", userId, session.getTokenFamily());
            userSessionRepository.revokeByTokenFamily(session.getTokenFamily());
            cookieUtils.clearAuthCookies(response);
            throw new UnauthorizedException("Security alert: token reuse detected. Please log in again.");
        }

        // 5. Revoke current token (rotation)
        session.setIsRevoked(true);
        session.setRevokedReason("ROTATED");
        userSessionRepository.save(session);

        // 6. Issue new tokens in the same family
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UnauthorizedException("User not found"));
        
        userRepository.updateLastLoginAt(userId, Instant.now());

        issueTokens(user, session.getTokenFamily(), response);

        return new AuthTokenResponseDto(
                userMapper.toDto(user),
                false,
                "Token refreshed successfully"
        );
    }

    @Override
    @Transactional
    public void logout(UUID userId, HttpServletRequest request, HttpServletResponse response) {
        log.debug("Processing logout for user: {}", userId);

        // Clear cookies immediately
        cookieUtils.clearAuthCookies(response);

        // Try to revoke specific session if refresh token is present
        cookieUtils.extractRefreshToken(request).ifPresent(token -> {
            try {
                if (jwtTokenProvider.validateToken(token)) {
                    String jti = jwtTokenProvider.extractJti(token);
                    userSessionRepository.revokeByJti(jti, "LOGOUT");
                    log.debug("Revoked session JTI: {}", jti);
                }
            } catch (Exception e) {
                log.warn("Failed to parse refresh token during logout", e);
            }
        });
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private void issueTokens(User user, UUID tokenFamily, HttpServletResponse response) {
        // Generate tokens
        String accessToken = jwtTokenProvider.generateAccessToken(
                user.getId(), user.getLogin(), user.getEmail(), user.getRole());

        // If no family provided (new login), create a new family
        UUID family = (tokenFamily != null) ? tokenFamily : UUID.randomUUID();
        JwtTokenProvider.RefreshTokenData refreshData = jwtTokenProvider.generateRefreshToken(user.getId(), family);

        // Save session
        UserSession session = UserSession.builder()
                .user(user)
                .jti(refreshData.jti())
                .tokenFamily(refreshData.tokenFamily())
                .expiresAt(refreshData.expiresAt())
                .build();
        userSessionRepository.save(session);

        // Set cookies
        cookieUtils.addAccessTokenCookie(response, accessToken);
        cookieUtils.addRefreshTokenCookie(response, refreshData.token());
    }
}
