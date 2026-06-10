package com.codesage.domain.auth.repository;

import com.codesage.domain.auth.model.UserSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for {@link UserSession} entities.
 *
 * <p>Used for session lifecycle management:
 * forced logout, theft detection, device listing.
 */
@Repository
public interface UserSessionRepository extends JpaRepository<UserSession, UUID> {

    Optional<UserSession> findByJti(String jti);

    List<UserSession> findByUserIdAndIsRevokedFalse(UUID userId);

    /**
     * Revokes all active sessions for a user (forced logout / theft response).
     */
    @Modifying
    @Query("""
        UPDATE UserSession s
        SET s.isRevoked = TRUE, s.revokedReason = :reason
        WHERE s.user.id = :userId AND s.isRevoked = FALSE
        """)
    int revokeAllByUserId(@Param("userId") UUID userId, @Param("reason") String reason);

    /**
     * Revokes all sessions sharing the same token family (theft detection).
     * When a reused token is detected, all sessions from that family are invalidated.
     */
    @Modifying
    @Query("""
        UPDATE UserSession s
        SET s.isRevoked = TRUE, s.revokedReason = 'THEFT_DETECTED'
        WHERE s.tokenFamily = :family AND s.isRevoked = FALSE
        """)
    int revokeByTokenFamily(@Param("family") UUID family);

    /**
     * Revokes a specific session by its JWT ID.
     */
    @Modifying
    @Query("""
        UPDATE UserSession s
        SET s.isRevoked = TRUE, s.revokedReason = :reason
        WHERE s.jti = :jti
        """)
    int revokeByJti(@Param("jti") String jti, @Param("reason") String reason);
}
