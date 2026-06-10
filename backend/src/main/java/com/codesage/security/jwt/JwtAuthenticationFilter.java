package com.codesage.security.jwt;

import com.codesage.security.principal.UserPrincipal;
import com.codesage.security.util.CookieUtils;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * JWT authentication filter — runs once per request.
 *
 * <p>Extracts the access token from the HttpOnly cookie, validates it,
 * and populates the {@link SecurityContextHolder} with a
 * {@link UserPrincipal} constructed from JWT claims.
 *
 * <p><strong>No database call is made per request.</strong>
 * All required user information (id, login, email, role) is encoded
 * directly in the JWT payload.
 *
 * <p>If no valid token is found, the request continues unauthenticated —
 * the endpoint's authorization rules determine whether it's accessible.
 */
@Log4j2
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final CookieUtils      cookieUtils;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest  request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain         filterChain
    ) throws ServletException, IOException {

        try {
            cookieUtils.extractAccessToken(request)
                    .filter(jwtTokenProvider::validateToken)
                    .ifPresent(token -> authenticateRequest(token, request));
        } catch (Exception e) {
            // Never block the request — let security rules decide
            log.debug("JWT authentication failed for request [{}]: {}",
                    request.getRequestURI(), e.getMessage());
        }

        filterChain.doFilter(request, response);
    }

    private void authenticateRequest(String token, HttpServletRequest request) {
        UUID   userId = jwtTokenProvider.extractUserId(token);
        String login  = jwtTokenProvider.extractLogin(token);
        String email  = jwtTokenProvider.extractEmail(token);
        String role   = jwtTokenProvider.extractRole(token);

        UserPrincipal principal = new UserPrincipal(userId, login, email, role);

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        principal, null, principal.getAuthorities());

        authentication.setDetails(
                new WebAuthenticationDetailsSource().buildDetails(request));

        SecurityContextHolder.getContext().setAuthentication(authentication);
        log.debug("Authenticated user [{}] for request [{}]", login, request.getRequestURI());
    }

    /**
     * Skip JWT validation for public endpoints to avoid unnecessary processing.
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/actuator")
                || path.startsWith("/swagger-ui")
                || path.startsWith("/v3/api-docs");
    }
}
