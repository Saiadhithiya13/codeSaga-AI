package com.codesage.config.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Cookie security configuration.
 *
 * <p>Bound from {@code app.cookie.*} in application YAML.
 * Architecture Spec:
 * <ul>
 *   <li>JWT in HttpOnly cookies</li>
 *   <li>SameSite=Lax</li>
 *   <li>Secure flag configurable by environment</li>
 * </ul>
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "app.cookie")
public class CookieProperties {

    /**
     * Whether to set the Secure flag on cookies.
     * Must be {@code true} in production (HTTPS only).
     * Set to {@code false} for local HTTP development.
     */
    private boolean secure = false;

    /**
     * Cookie domain.
     * Leave blank to use the request domain (recommended for local dev).
     */
    private String domain = "";

    /** Cookie path — restrict to API routes */
    private String path = "/";

    /** SameSite policy — Lax prevents CSRF while allowing top-level navigation */
    private String sameSite = "Lax";
}
