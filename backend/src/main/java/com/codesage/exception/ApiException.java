package com.codesage.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Base exception for all CodeSage AI application exceptions.
 *
 * <p>Every domain-specific exception should extend this class.
 * The {@link #status} field is used by {@link GlobalExceptionHandler}
 * to determine the HTTP response status code automatically.
 */
@Getter
public abstract class ApiException extends RuntimeException {

    private final HttpStatus status;
    private final String errorCode;

    protected ApiException(String message, HttpStatus status, String errorCode) {
        super(message);
        this.status = status;
        this.errorCode = errorCode;
    }

    protected ApiException(String message, HttpStatus status, String errorCode, Throwable cause) {
        super(message, cause);
        this.status = status;
        this.errorCode = errorCode;
    }
}
