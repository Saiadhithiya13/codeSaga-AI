package com.codesage.domain.auth.model;

import com.codesage.infrastructure.persistence.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

/**
 * JPA entity representing an authenticated GitHub user.
 *
 * <p>Maps to the {@code users} table created in V1 migration.
 * Architecture Spec §Security: GitHub access tokens are stored
 * AES-256-GCM encrypted via {@link com.codesage.security.util.TokenEncryptionService}.
 */
@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User extends BaseEntity {

    @Column(name = "github_id", nullable = false, unique = true)
    private Long githubId;

    /** GitHub login (username), e.g. "octocat" */
    @Column(name = "login", nullable = false, unique = true, length = 64)
    private String login;

    /** Display name from GitHub profile */
    @Column(name = "name", length = 128)
    private String name;

    @Column(name = "email", length = 256)
    private String email;

    @Column(name = "avatar_url")
    private String avatarUrl;

    /**
     * AES-256-GCM encrypted GitHub OAuth access token.
     * Architecture Spec §Security: AES-256 encryption for GitHub tokens.
     */
    @Column(name = "github_access_token")
    private String githubAccessToken;

    @Column(name = "role", nullable = false, length = 32)
    @Builder.Default
    private String role = "USER";

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "last_login_at", columnDefinition = "TIMESTAMPTZ")
    private java.time.Instant lastLoginAt;
}
