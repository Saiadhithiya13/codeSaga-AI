package com.codesage.domain.auth.repository;

import com.codesage.domain.auth.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for {@link User} entities.
 *
 * <p>Provides GitHub-specific lookups needed during OAuth callback
 * (find-or-create by GitHub ID) and profile retrieval.
 */
@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    /**
     * Finds a user by their GitHub numeric ID.
     * Used during OAuth callback to determine create vs. update.
     */
    Optional<User> findByGithubId(Long githubId);

    /**
     * Finds a user by their GitHub login (username).
     */
    Optional<User> findByLogin(String login);

    /**
     * Checks whether a user with the given GitHub ID exists.
     */
    boolean existsByGithubId(Long githubId);

    /**
     * Updates last_login_at timestamp for a user without loading the full entity.
     * Called on every successful authentication.
     */
    @Modifying
    @Query("UPDATE User u SET u.lastLoginAt = :loginAt WHERE u.id = :userId")
    int updateLastLoginAt(@Param("userId") UUID userId, @Param("loginAt") Instant loginAt);
}
