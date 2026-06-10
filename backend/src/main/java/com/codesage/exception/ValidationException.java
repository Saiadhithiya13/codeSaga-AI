package com.codesage.exception;

import org.springframework.http.HttpStatus;

/**
 * Thrown when business rule validation fails (not bean validation).
 *
 * <p>Maps to HTTP {@code 400 Bad Request}.
 *
 * <p>Use this for domain-level invariant violations, e.g.:
 * <pre>{@code
 *   if (repo.isAlreadyIndexed()) {
 *       throw new ValidationException("Repository is already indexed");
 *   }
 * }</pre>
 */
public class ValidationException extends ApiException {

    private static final String ERROR_CODE = "VALIDATION_ERROR";

    public ValidationException(String message) {
        super(message, HttpStatus.BAD_REQUEST, ERROR_CODE);
    }

    public ValidationException(String message, Throwable cause) {
        super(message, HttpStatus.BAD_REQUEST, ERROR_CODE, cause);
    }
}
