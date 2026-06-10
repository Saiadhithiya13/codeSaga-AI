package com.codesage.security.jwt;

import com.codesage.config.properties.JwtProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link JwtTokenProvider}.
 */
class JwtTokenProviderTest {

    private JwtTokenProvider jwtTokenProvider;
    private JwtProperties    jwtProperties;

    // A valid 256-bit+ base64 key
    private static final String TEST_SECRET = "super_secret_key_that_must_be_at_least_256_bits_long_for_hs256_algorithm";

    @BeforeEach
    void setUp() {
        jwtProperties = new JwtProperties();
        jwtProperties.setSecret(TEST_SECRET);
        jwtProperties.setAccessTokenExpiryMinutes(15);
        jwtProperties.setRefreshTokenExpiryDays(7);

        jwtTokenProvider = new JwtTokenProvider(jwtProperties);
    }

    @Test
    @DisplayName("Should generate valid access token and extract all claims")
    void generateAccessToken_validClaims() {
        // Arrange
        UUID userId = UUID.randomUUID();
        String login = "octocat";
        String email = "octocat@github.com";
        String role = "USER";

        // Act
        String token = jwtTokenProvider.generateAccessToken(userId, login, email, role);

        // Assert
        assertNotNull(token);
        assertTrue(jwtTokenProvider.validateToken(token));

        assertEquals(userId, jwtTokenProvider.extractUserId(token));
        assertEquals(login, jwtTokenProvider.extractLogin(token));
        assertEquals(email, jwtTokenProvider.extractEmail(token));
        assertEquals(role, jwtTokenProvider.extractRole(token));
        assertEquals("ACCESS", jwtTokenProvider.parseClaims(token).get("type", String.class));
    }

    @Test
    @DisplayName("Should generate valid refresh token with jti and family")
    void generateRefreshToken_validClaims() {
        // Arrange
        UUID userId = UUID.randomUUID();
        UUID family = UUID.randomUUID();

        // Act
        JwtTokenProvider.RefreshTokenData data = jwtTokenProvider.generateRefreshToken(userId, family);

        // Assert
        assertNotNull(data.token());
        assertNotNull(data.jti());
        assertEquals(family, data.tokenFamily());
        assertTrue(jwtTokenProvider.validateToken(data.token()));

        assertEquals(userId, jwtTokenProvider.extractUserId(data.token()));
        assertEquals(data.jti(), jwtTokenProvider.extractJti(data.token()));
        assertEquals(family.toString(), jwtTokenProvider.extractTokenFamily(data.token()));
        assertEquals("REFRESH", jwtTokenProvider.parseClaims(data.token()).get("type", String.class));
    }

    @Test
    @DisplayName("Should reject token signed with different secret")
    void validateToken_invalidSignature_returnsFalse() {
        // Arrange
        String token = jwtTokenProvider.generateAccessToken(UUID.randomUUID(), "test", "test@test.com", "USER");

        JwtProperties badProps = new JwtProperties();
        badProps.setSecret("different_secret_key_that_must_be_at_least_256_bits_long_for_hs256_algorithm");
        JwtTokenProvider badProvider = new JwtTokenProvider(badProps);

        // Act
        boolean isValid = badProvider.validateToken(token);

        // Assert
        assertFalse(isValid, "Token should be invalid when verified with a different secret");
    }

    @Test
    @DisplayName("Should reject malformed token string")
    void validateToken_malformedToken_returnsFalse() {
        assertFalse(jwtTokenProvider.validateToken("not.a.real.jwt"));
        assertFalse(jwtTokenProvider.validateToken(null));
        assertFalse(jwtTokenProvider.validateToken(""));
    }
}
