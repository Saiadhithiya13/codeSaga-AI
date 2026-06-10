package com.codesage.domain.auth.service;

import com.codesage.domain.auth.dto.response.UserResponseDto;
import com.codesage.domain.auth.model.User;

import java.util.UUID;

/**
 * Contract for user management operations.
 */
public interface UserService {

    /**
     * Retrieves the profile of the currently authenticated user by their ID.
     *
     * @param userId the authenticated user's UUID
     * @return public profile DTO
     */
    UserResponseDto findById(UUID userId);

    /**
     * Finds a user entity by ID. Used internally by auth flows.
     *
     * @param userId the user's UUID
     * @return the User entity
     */
    User findEntityById(UUID userId);
}
