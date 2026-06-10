package com.codesage.config.properties;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * JWT configuration properties.
 *
 * <p>Bound from {@code app.jwt.*} in application YAML.
 * Architecture Spec: Access Token = 15 min, Refresh Token = 7 days.
 * Signing secret MUST be provided via environment variable — never hardcoded.
 */
@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "app.jwt")
public class JwtProperties {

    /**
     * HS256 signing secret — must be base64-encoded, minimum 256 bits (32 bytes).
     * Set via {@code JWT_SECRET} environment variable.
     */
    @NotBlank(message = "JWT signing secret must not be blank")
    @Size(min = 32, message = "JWT secret must be at least 32 characters (256 bits)")
    private String secret;

    /** Access token TTL in minutes. Architecture Spec: 15 minutes. */
    @Min(1)
    private long accessTokenExpiryMinutes = 15;

    /** Refresh token TTL in days. Architecture Spec: 7 days. */
    @Min(1)
    private long refreshTokenExpiryDays = 7;

    /** Cookie name for access token. */
    private String accessTokenCookieName = "access_token";

    /** Cookie name for refresh token. */
    private String refreshTokenCookieName = "refresh_token";

    public long getAccessTokenExpiryMs() {
        return accessTokenExpiryMinutes * 60 * 1000L;
    }

    public long getRefreshTokenExpiryMs() {
        return refreshTokenExpiryDays * 24 * 60 * 60 * 1000L;
    }
}
