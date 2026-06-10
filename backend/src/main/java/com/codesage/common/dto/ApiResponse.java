package com.codesage.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

/**
 * Standardized API response envelope used across all endpoints.
 *
 * <p>Architecture Spec §API Standards mandates this exact shape:
 * <pre>{@code
 *   { "success": true, "message": "Success", "data": {} }
 * }</pre>
 *
 * <p>{@code null} fields are omitted from serialization via
 * {@link JsonInclude.Include#NON_NULL} to keep responses clean.
 *
 * @param <T> the type of the response payload
 */
@Schema(description = "Standard API response envelope")
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(

        @Schema(description = "Indicates whether the request succeeded", example = "true")
        boolean success,

        @Schema(description = "Human-readable status message", example = "Success")
        String message,

        @Schema(description = "Response payload — null on errors")
        T data,

        @Schema(description = "ISO-8601 timestamp of when the response was generated")
        Instant timestamp,

        @Schema(description = "The request path that generated this response", example = "/api/v1/health")
        String path
) {

    // ─── Static Factory Methods ───────────────────────────────────────────

    /**
     * Creates a successful response with data payload.
     */
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, "Success", data, Instant.now(), null);
    }

    /**
     * Creates a successful response with a custom message and data payload.
     */
    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(true, message, data, Instant.now(), null);
    }

    /**
     * Creates a successful response with no data (e.g., 204-style with body).
     */
    public static <Void> ApiResponse<Void> success(String message) {
        return new ApiResponse<>(true, message, null, Instant.now(), null);
    }

    /**
     * Creates an error response (no data payload).
     */
    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<>(false, message, null, Instant.now(), null);
    }

    /**
     * Creates an error response with a request path for traceability.
     */
    public static <T> ApiResponse<T> error(String message, String path) {
        return new ApiResponse<>(false, message, null, Instant.now(), path);
    }
}
