package com.codesage.config.properties;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * GitHub OAuth application configuration.
 *
 * <p>Bound from {@code app.github.*} in application YAML.
 * Architecture Spec: Environment variables for all secrets.
 * {@code clientSecret} MUST come from {@code GITHUB_CLIENT_SECRET} env var.
 */
@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "app.github")
public class GitHubProperties {

    @NotBlank(message = "GitHub client ID must not be blank")
    private String clientId;

    @NotBlank(message = "GitHub client secret must not be blank")
    private String clientSecret;

    @NotBlank(message = "GitHub webhook secret must not be blank")
    private String webhookSecret;

    /** GitHub OAuth token exchange endpoint */
    private String tokenUrl = "https://github.com/login/oauth/access_token";

    /** GitHub REST API base URL */
    private String apiBaseUrl = "https://api.github.com";
}
