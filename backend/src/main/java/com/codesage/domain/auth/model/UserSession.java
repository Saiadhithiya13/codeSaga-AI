package com.codesage.domain.auth.model;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

/**
 * JPA entity representing an active user session (refresh token record).
 *
 * <p>Maps to the {@code user_sessions} table created in V2 migration.
 * The actual refresh token JWT is stored in Redis with key
 * {@code session:refresh:{jti}} for fast lookup and automatic TTL expiry.
 *
 * <p>This table provides:
 * <ul>
 *   <li>Device management (list active sessions)</li>
 *   <li>Forced logout (revoke by jti or by user_id)</li>
 *   <li>Token theft detection (token family tracking)</li>
 * </ul>
 */
@Entity
@Table(name = "user_sessions")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserSession {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false, columnDefinition = "UUID")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** JWT ID claim — unique identifier for this specific refresh token */
    @Column(name = "jti", nullable = false, unique = true, length = 64)
    private String jti;

    /** Token family UUID — shared by all tokens in a rotation chain */
    @Column(name = "token_family", nullable = false, columnDefinition = "UUID")
    @Builder.Default
    private UUID tokenFamily = UUID.randomUUID();

    /** First 256 chars of User-Agent header for device identification */
    @Column(name = "device_hint", length = 256)
    private String deviceHint;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "expires_at", nullable = false, columnDefinition = "TIMESTAMPTZ")
    private Instant expiresAt;

    @Column(name = "is_revoked", nullable = false)
    @Builder.Default
    private Boolean isRevoked = false;

    @Column(name = "revoked_reason", length = 64)
    private String revokedReason;

    @Column(name = "last_used_at", nullable = false, columnDefinition = "TIMESTAMPTZ")
    @Builder.Default
    private Instant lastUsedAt = Instant.now();

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "TIMESTAMPTZ")
    private Instant createdAt;
}
