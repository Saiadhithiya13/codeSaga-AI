package com.codesage.security.jwt;

import com.codesage.config.properties.JwtProperties;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

/**
 * JWT token generation and validation using JJWT 0.12.x.
 *
 * <p>Architecture Spec:
 * <ul>
 *   <li>Access Token TTL: 15 minutes</li>
 *   <li>Refresh Token TTL: 7 days</li>
 *   <li>Signed with HS256 using secret from {@link JwtProperties}</li>
 *   <li>JWT stored in HttpOnly cookies — never in localStorage</li>
 * </ul>
 *
 * <p>Access token claims:
 * <pre>{@code
 *   sub     — userId (UUID string)
 *   login   — GitHub login
 *   email   — user email
 *   role    — user role
 *   iat     — issued at
 *   exp     — expiry
 * }</pre>
 *
 * <p>Refresh token claims:
 * <pre>{@code
 *   sub     — userId (UUID string)
 *   jti     — unique token ID (stored in Redis for rotation)
 *   family  — token family UUID (for theft detection)
 *   iat     — issued at
 *   exp     — expiry
 * }</pre>
 */
@Log4j2
@Component
@RequiredArgsConstructor
public class JwtTokenProvider {

    private final JwtProperties jwtProperties;

    // ─── Key ─────────────────────────────────────────────────────────────────

    /**
     * Derives the signing key from the configured secret.
     * JJWT 0.12.x requires {@link SecretKey} — not raw strings.
     */
    private SecretKey signingKey() {
        byte[] keyBytes = jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    // ─── Token Generation ─────────────────────────────────────────────────────

    /**
     * Generates a signed JWT access token for the given user.
     *
     * @param userId    user's UUID
     * @param login     GitHub login
     * @param email     user's email
     * @param role      user's role
     * @return compact signed JWT string
     */
    public String generateAccessToken(UUID userId, String login, String email, String role) {
        Instant now = Instant.now();
        Instant expiry = now.plusMillis(jwtProperties.getAccessTokenExpiryMs());

        return Jwts.builder()
                .subject(userId.toString())
                .claims(Map.of(
                        "login", login != null ? login : "",
                        "email", email != null ? email : "",
                        "role",  role  != null ? role  : "USER",
                        "type",  "ACCESS"
                ))
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(signingKey())
                .compact();
    }

    /**
     * Generates a signed JWT refresh token.
     *
     * @param userId      user's UUID
     * @param tokenFamily shared family UUID for this rotation chain
     * @return {@link RefreshTokenData} containing compact JWT and its jti
     */
    public RefreshTokenData generateRefreshToken(UUID userId, UUID tokenFamily) {
        String jti = UUID.randomUUID().toString();
        Instant now = Instant.now();
        Instant expiry = now.plusMillis(jwtProperties.getRefreshTokenExpiryMs());

        String token = Jwts.builder()
                .subject(userId.toString())
                .id(jti)
                .claims(Map.of(
                        "family", tokenFamily.toString(),
                        "type",   "REFRESH"
                ))
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(signingKey())
                .compact();

        return new RefreshTokenData(token, jti, tokenFamily, expiry);
    }

    // ─── Token Validation ─────────────────────────────────────────────────────

    /**
     * Validates a JWT token signature and expiry.
     *
     * @param token the compact JWT string
     * @return {@code true} if valid; {@code false} otherwise
     */
    public boolean validateToken(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (ExpiredJwtException e) {
            log.debug("JWT token is expired: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            log.warn("JWT token is unsupported: {}", e.getMessage());
        } catch (MalformedJwtException e) {
            log.warn("JWT token is malformed: {}", e.getMessage());
        } catch (SignatureException e) {
            log.warn("JWT signature validation failed: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            log.warn("JWT token is empty or null: {}", e.getMessage());
        }
        return false;
    }

    // ─── Claims Extraction ────────────────────────────────────────────────────

    /**
     * Parses and returns the claims from a JWT (throws on invalid token).
     */
    public Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public UUID extractUserId(String token) {
        return UUID.fromString(parseClaims(token).getSubject());
    }

    public String extractLogin(String token) {
        return parseClaims(token).get("login", String.class);
    }

    public String extractEmail(String token) {
        return parseClaims(token).get("email", String.class);
    }

    public String extractRole(String token) {
        return parseClaims(token).get("role", String.class);
    }

    public String extractJti(String token) {
        return parseClaims(token).getId();
    }

    public String extractTokenFamily(String token) {
        return parseClaims(token).get("family", String.class);
    }

    // ─── Inner Record ─────────────────────────────────────────────────────────

    /**
     * Holds all data associated with a newly generated refresh token.
     */
    public record RefreshTokenData(
            String token,
            String jti,
            UUID tokenFamily,
            Instant expiresAt
    ) {}
}
