package com.codesage.exception;

import com.codesage.common.dto.ApiResponse;
import com.codesage.common.dto.ErrorDetail;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.List;
import java.util.UUID;

/**
 * Centralized exception handler for all REST controllers.
 *
 * <p>Converts exceptions to standardized {@link ApiResponse} envelopes.
 * Every exception type is logged with a correlation ID for traceability.
 *
 * <p>Handler resolution order (most specific to least):
 * <ol>
 *   <li>{@link MethodArgumentNotValidException} — bean validation errors</li>
 *   <li>{@link ApiException} subclasses — domain exceptions</li>
 *   <li>{@link Exception} — unexpected fallback (500)</li>
 * </ol>
 */
@Log4j2
@RestControllerAdvice
public class GlobalExceptionHandler {

    // ─── Bean Validation (@Valid) ─────────────────────────────────────────

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<List<ErrorDetail>>> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex,
            HttpServletRequest request) {

        String correlationId = generateCorrelationId();
        log.warn("[{}] Validation failed on request to {}: {}",
                correlationId, request.getRequestURI(), ex.getMessage());

        List<ErrorDetail> errors = ex.getBindingResult()
                .getAllErrors()
                .stream()
                .filter(error -> error instanceof FieldError)
                .map(error -> {
                    FieldError fe = (FieldError) error;
                    return ErrorDetail.of(fe.getField(), fe.getDefaultMessage(), fe.getRejectedValue());
                })
                .toList();

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ApiResponse<>(
                        false,
                        "Validation failed — see errors for details",
                        errors,
                        java.time.Instant.now(),
                        request.getRequestURI()
                ));
    }

    // ─── Type Mismatch (e.g. UUID path variable) ──────────────────────────

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex,
            HttpServletRequest request) {

        String correlationId = generateCorrelationId();
        log.warn("[{}] Type mismatch on '{}': expected {}, got '{}'",
                correlationId, ex.getName(),
                ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "unknown",
                ex.getValue());

        return buildErrorResponse(
                HttpStatus.BAD_REQUEST,
                "Invalid value for parameter '%s': %s".formatted(ex.getName(), ex.getValue()),
                request.getRequestURI()
        );
    }

    // ─── Domain Exceptions (ResourceNotFoundException etc.) ──────────────

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleResourceNotFound(
            ResourceNotFoundException ex,
            HttpServletRequest request) {

        log.warn("[NOT_FOUND] {}: {}", request.getRequestURI(), ex.getMessage());
        return buildErrorResponse(ex.getStatus(), ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(
            ValidationException ex,
            HttpServletRequest request) {

        log.warn("[VALIDATION] {}: {}", request.getRequestURI(), ex.getMessage());
        return buildErrorResponse(ex.getStatus(), ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(ExternalServiceException.class)
    public ResponseEntity<ApiResponse<Void>> handleExternalService(
            ExternalServiceException ex,
            HttpServletRequest request) {

        log.error("[EXTERNAL_SERVICE] {}: {}", request.getRequestURI(), ex.getMessage(), ex.getCause());
        return buildErrorResponse(ex.getStatus(), ex.getMessage(), request.getRequestURI());
    }

    /**
     * Catch-all for any {@link ApiException} subclasses not explicitly mapped above.
     */
    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiResponse<Void>> handleApiException(
            ApiException ex,
            HttpServletRequest request) {

        String correlationId = generateCorrelationId();
        log.error("[{}] ApiException at {}: [{}] {}",
                correlationId, request.getRequestURI(), ex.getErrorCode(), ex.getMessage());

        return buildErrorResponse(ex.getStatus(), ex.getMessage(), request.getRequestURI());
    }

    // ─── Unexpected Fallback ──────────────────────────────────────────────

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpected(
            Exception ex,
            HttpServletRequest request) {

        String correlationId = generateCorrelationId();
        log.error("[{}] Unexpected error at {}: {}",
                correlationId, request.getRequestURI(), ex.getMessage(), ex);

        return buildErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred. Reference: " + correlationId,
                request.getRequestURI()
        );
    }

    // ─── Helpers ──────────────────────────────────────────────────────────

    private ResponseEntity<ApiResponse<Void>> buildErrorResponse(
            HttpStatus status, String message, String path) {

        return ResponseEntity
                .status(status)
                .body(ApiResponse.error(message, path));
    }

    /**
     * Generates a short correlation ID for log traceability.
     * This will be replaced with MDC / Trace IDs in Sprint 2.
     */
    private String generateCorrelationId() {
        return UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}
