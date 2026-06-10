package com.codesage.exception;

import org.springframework.http.HttpStatus;

/**
 * Thrown when an external service (GitHub API, Gemini, ChromaDB) fails
 * or returns an unexpected response.
 *
 * <p>Maps to HTTP {@code 502 Bad Gateway}.
 *
 * <p>Usage:
 * <pre>{@code
 *   throw new ExternalServiceException("GitHub API", "rate limit exceeded", cause);
 * }</pre>
 */
public class ExternalServiceException extends ApiException {

    private static final String ERROR_CODE = "EXTERNAL_SERVICE_ERROR";

    public ExternalServiceException(String serviceName, String reason) {
        super(
                "External service error [%s]: %s".formatted(serviceName, reason),
                HttpStatus.BAD_GATEWAY,
                ERROR_CODE
        );
    }

    public ExternalServiceException(String serviceName, String reason, Throwable cause) {
        super(
                "External service error [%s]: %s".formatted(serviceName, reason),
                HttpStatus.BAD_GATEWAY,
                ERROR_CODE,
                cause
        );
    }
}
