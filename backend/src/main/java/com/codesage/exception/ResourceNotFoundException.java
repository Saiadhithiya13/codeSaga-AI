package com.codesage.exception;

import org.springframework.http.HttpStatus;

/**
 * Thrown when a requested resource does not exist in the system.
 *
 * <p>Maps to HTTP {@code 404 Not Found}.
 *
 * <p>Usage:
 * <pre>{@code
 *   throw new ResourceNotFoundException("Repository", repoId);
 * }</pre>
 */
public class ResourceNotFoundException extends ApiException {

    private static final String ERROR_CODE = "RESOURCE_NOT_FOUND";

    public ResourceNotFoundException(String resourceName, Object identifier) {
        super(
                "%s not found with identifier: %s".formatted(resourceName, identifier),
                HttpStatus.NOT_FOUND,
                ERROR_CODE
        );
    }

    public ResourceNotFoundException(String message) {
        super(message, HttpStatus.NOT_FOUND, ERROR_CODE);
    }
}
