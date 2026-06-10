package com.codesage.security.util;

import com.codesage.config.properties.CookieProperties;
import com.codesage.config.properties.JwtProperties;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Optional;

/**
 * Utility for building and extracting HttpOnly JWT cookies.
 *
 * <p>Architecture Spec §Security:
 * <ul>
 *   <li>JWT in HttpOnly cookies</li>
 *   <li>SameSite=Lax</li>
 *   <li>Secure flag configurable by environment</li>
 *   <li>No localStorage tokens</li>
 * </ul>
 *
 * <p>Uses {@link ResponseCookie} (Spring 5.1+) to properly set the
 * {@code SameSite} attribute, which is not supported by the legacy
 * {@link Cookie} API.
 */
@Log4j2
@Component
@RequiredArgsConstructor
public class CookieUtils {

    private final JwtProperties jwtProperties;
    private final CookieProperties cookieProperties;

    // ─── Cookie Creation ──────────────────────────────────────────────────────

    /**
     * Adds the JWT access token as an HttpOnly cookie to the response.
     */
    public void addAccessTokenCookie(HttpServletResponse response, String token) {
        ResponseCookie cookie = buildCookie(
                jwtProperties.getAccessTokenCookieName(),
                token,
                (int) (jwtProperties.getAccessTokenExpiryMs() / 1000)
        );
        response.addHeader("Set-Cookie", cookie.toString());
        log.debug("Access token cookie set (max-age: {}s)", jwtProperties.getAccessTokenExpiryMs() / 1000);
    }

    /**
     * Adds the JWT refresh token as an HttpOnly cookie to the response.
     */
    public void addRefreshTokenCookie(HttpServletResponse response, String token) {
        ResponseCookie cookie = buildCookie(
                jwtProperties.getRefreshTokenCookieName(),
                token,
                (int) (jwtProperties.getRefreshTokenExpiryMs() / 1000)
        );
        response.addHeader("Set-Cookie", cookie.toString());
        log.debug("Refresh token cookie set (max-age: {}s)", jwtProperties.getRefreshTokenExpiryMs() / 1000);
    }

    /**
     * Clears both access and refresh token cookies (logout).
     * Sets Max-Age=0 and empty value to immediately expire.
     */
    public void clearAuthCookies(HttpServletResponse response) {
        ResponseCookie clearAccess = buildCookie(
                jwtProperties.getAccessTokenCookieName(), "", 0);
        ResponseCookie clearRefresh = buildCookie(
                jwtProperties.getRefreshTokenCookieName(), "", 0);

        response.addHeader("Set-Cookie", clearAccess.toString());
        response.addHeader("Set-Cookie", clearRefresh.toString());
        log.debug("Auth cookies cleared");
    }

    // ─── Cookie Extraction ────────────────────────────────────────────────────

    /**
     * Extracts the access token from the incoming request's cookies.
     *
     * @param request the HTTP request
     * @return Optional containing the token value, or empty if not present
     */
    public Optional<String> extractAccessToken(HttpServletRequest request) {
        return extractCookieValue(request, jwtProperties.getAccessTokenCookieName());
    }

    /**
     * Extracts the refresh token from the incoming request's cookies.
     */
    public Optional<String> extractRefreshToken(HttpServletRequest request) {
        return extractCookieValue(request, jwtProperties.getRefreshTokenCookieName());
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private ResponseCookie buildCookie(String name, String value, int maxAgeSeconds) {
        ResponseCookie.ResponseCookieBuilder builder = ResponseCookie.from(name, value)
                .httpOnly(true)                              // No JS access
                .secure(cookieProperties.isSecure())         // HTTPS only in prod
                .sameSite(cookieProperties.getSameSite())    // SameSite=Lax (arch spec)
                .path(cookieProperties.getPath())
                .maxAge(maxAgeSeconds);

        if (!cookieProperties.getDomain().isBlank()) {
            builder.domain(cookieProperties.getDomain());
        }

        return builder.build();
    }

    private Optional<String> extractCookieValue(HttpServletRequest request, String cookieName) {
        if (request.getCookies() == null) return Optional.empty();
        return Arrays.stream(request.getCookies())
                .filter(c -> cookieName.equals(c.getName()))
                .map(Cookie::getValue)
                .filter(v -> v != null && !v.isBlank())
                .findFirst();
    }
}
